package com.schlueternetz.emacompanion.core.api

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.schlueternetz.emacompanion.core.api.log.ApiCallLogRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HourlyEnergyRepositoryTest {
    private lateinit var usageCounter: ApiUsageRepository
    private lateinit var log: ApiCallLogRepository
    private lateinit var hourlyPrefs: SharedPreferences
    private var now = 3_700_000L

    private class FakeClient(
        var result: ApiResult<HourlySnapshot> = ApiResult.Success(HourlySnapshot(mapOf(6 to 1.0, 7 to 2.0))),
    ) : EmaApiClient {
        var calls = 0

        override suspend fun getCurrentProduction() = ProductionFetch(ApiResult.ConfigurationError)

        override suspend fun getBatchInverterEnergy(date: String) = BatchEnergyFetch(ApiResult.ConfigurationError)

        override suspend fun getHourlyEnergy(date: String): HourlyEnergyFetch {
            calls++
            return HourlyEnergyFetch(result, "/ecu/energy/test", 5L, "GET ...", "{}")
        }
    }

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        hourlyPrefs = ctx.getSharedPreferences("hourly_t", Context.MODE_PRIVATE)
        hourlyPrefs.edit().clear().apply()
        ctx
            .getSharedPreferences("usage_t", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        ctx
            .getSharedPreferences("log_t", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        usageCounter = ApiUsageRepository(ctx.getSharedPreferences("usage_t", Context.MODE_PRIVATE), monthProvider = { "2026-07" })
        log = ApiCallLogRepository(ctx.getSharedPreferences("log_t", Context.MODE_PRIVATE))
    }

    private fun repo(client: EmaApiClient) =
        HourlyEnergyRepository(
            client = client,
            usageCounter = usageCounter,
            log = log,
            appSecretProvider = { "secret" },
            today = { "2026-07-01" },
            clock = { now },
            prefs = hourlyPrefs,
        )

    @Test
    fun throttle_preventsCallBeforeWindowElapsed() =
        runBlocking {
            val client = FakeClient()
            val r = repo(client)
            r.refresh()
            assertEquals(1, client.calls)
            r.refresh()
            assertEquals(1, client.calls)
        }

    @Test
    fun force_bypassesThrottle() =
        runBlocking {
            val client = FakeClient()
            val r = repo(client)
            r.refresh()
            r.refresh(force = true)
            assertEquals(2, client.calls)
        }

    @Test
    fun successPath_countsAndThrottlesAndPersists() =
        runBlocking {
            val client = FakeClient()
            val r = repo(client)
            val state = r.refresh()
            assertEquals(1, client.calls)
            assertEquals(1, usageCounter.getRequestCount())
            assertEquals(now, hourlyPrefs.getLong("lastFetchMs", 0L))
            assertEquals(1.0, state.snapshot?.hours?.get(6)!!, 0.001)
            assertNull(state.error)
        }

    @Test
    fun failurePath_doesNotCountOrThrottle() =
        runBlocking {
            val client = FakeClient(result = ApiResult.NetworkError)
            val r = repo(client)
            r.refresh()
            assertEquals(0, usageCounter.getRequestCount())
            assertEquals(0L, hourlyPrefs.getLong("lastFetchMs", 0L))
        }

    @Test
    fun currentState_reconstructsFromPrefs() =
        runBlocking {
            val client = FakeClient()
            val r = repo(client)
            r.refresh()
            val r2 = repo(FakeClient())
            val state = r2.currentState()
            assertEquals(1.0, state.snapshot?.hours?.get(6)!!, 0.001)
        }

    @Test
    fun resetThrottle_clearsLastFetchTimestamp() =
        runBlocking {
            val client = FakeClient()
            val r = repo(client)
            r.refresh()
            assertEquals(now, hourlyPrefs.getLong("lastFetchMs", 0L))
            r.resetThrottle()
            assertEquals(0L, hourlyPrefs.getLong("lastFetchMs", 0L))
            r.refresh()
            assertEquals(2, client.calls)
        }

    @Test
    fun currentState_errorPreservedAfterFailure() =
        runBlocking {
            val client = FakeClient(result = ApiResult.NetworkError)
            val r = repo(client)
            r.refresh()
            val state = r.currentState()
            assertEquals(FetchError.NETWORK, state.error)
        }
}
