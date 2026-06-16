package com.schlueternetz.emacompanion.core.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.schlueternetz.emacompanion.core.api.log.ApiCallLogRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProductionRepositoryTest {

    private lateinit var usage: ApiUsageRepository
    private lateinit var log: ApiCallLogRepository
    private var now = 1_000_000L

    private class FakeClient(var fetch: ProductionFetch) : EmaApiClient {
        var calls = 0
        override suspend fun getCurrentProduction(): ProductionFetch {
            calls++
            return fetch
        }
    }

    private fun successFetch(power: Int = 8000) = ProductionFetch(
        result = ApiResult.Success(ProductionSnapshot(power)),
        endpoint = "/ecu/energy",
        durationMs = 5L,
        requestText = "GET",
        responseText = "{}",
    )

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.getSharedPreferences("usage_t", Context.MODE_PRIVATE).edit().clear().apply()
        ctx.getSharedPreferences("log_t", Context.MODE_PRIVATE).edit().clear().apply()
        usage = ApiUsageRepository(
            ctx.getSharedPreferences("usage_t", Context.MODE_PRIVATE),
            monthProvider = { "2026-06" },
        )
        log = ApiCallLogRepository(ctx.getSharedPreferences("log_t", Context.MODE_PRIVATE))
    }

    private fun repo(client: EmaApiClient) = ProductionRepository(
        client = client,
        usage = usage,
        log = log,
        appSecretProvider = { "secret123456" },
        clock = { now },
    )

    @Test
    fun fetchesWhenWindowElapsed() = runBlocking {
        val client = FakeClient(successFetch())
        val state = repo(client).refresh()
        assertEquals(ProductionSnapshot(8000), state.snapshot)
        assertEquals(1, client.calls)
        assertEquals(1, usage.getRequestCount())
        assertEquals(1, log.getAll().size)
    }

    @Test
    fun newInstanceWithinThrottle_showsLastPersistedSnapshot() = runBlocking {
        // First open: success, value persisted along with the throttle timestamp.
        repo(FakeClient(successFetch(8000))).refresh()
        // Navigate away and back within 10 min: Home + its repository are recreated.
        now += 2 * 60 * 1000L
        val freshClient = FakeClient(successFetch(9999))
        val state = repo(freshClient).refresh()
        assertEquals(0, freshClient.calls) // throttled — no new request
        assertEquals(ProductionSnapshot(8000), state.snapshot) // last value survives recreation
    }

    @Test
    fun noOpWithinThrottleWindow() = runBlocking {
        val client = FakeClient(successFetch())
        val repository = repo(client)
        repository.refresh()
        now += 5 * 60 * 1000L // 5 minutes later, within the 10-minute window
        val state = repository.refresh()
        assertEquals(1, client.calls) // no second call
        assertEquals(1, usage.getRequestCount()) // not incremented
        assertEquals(1, log.getAll().size) // not logged
        assertEquals(ProductionSnapshot(8000), state.snapshot) // cached value still shown
    }

    @Test
    fun fetchesAgainAfterWindowElapses() = runBlocking {
        val client = FakeClient(successFetch())
        val repository = repo(client)
        repository.refresh()
        now += 10 * 60 * 1000L // exactly 10 minutes later
        repository.refresh()
        assertEquals(2, client.calls)
        assertEquals(2, usage.getRequestCount())
        assertEquals(2, log.getAll().size)
    }

    @Test
    fun networkError_setsBannerAndKeepsCachedSnapshot() = runBlocking {
        val client = FakeClient(successFetch())
        val repository = repo(client)
        repository.refresh() // success, cached
        now += 11 * 60 * 1000L
        client.fetch = ProductionFetch(ApiResult.NetworkError, "/ecu/energy", 1L, "GET", "")
        val state = repository.refresh()
        assertTrue(state.networkError)
        assertEquals(ProductionSnapshot(8000), state.snapshot)
    }

    @Test
    fun success_clearsBannerAfterNetworkError() = runBlocking {
        val client = FakeClient(ProductionFetch(ApiResult.NetworkError, "/ecu/energy", 1L, "GET", ""))
        val repository = repo(client)
        repository.refresh() // network error
        now += 11 * 60 * 1000L
        client.fetch = successFetch()
        val state = repository.refresh()
        assertFalse(state.networkError)
        assertEquals(ProductionSnapshot(8000), state.snapshot)
    }

    @Test
    fun configurationError_doesNotCountOrLogOrThrottle() = runBlocking {
        val client = FakeClient(ProductionFetch(ApiResult.ConfigurationError))
        val repository = repo(client)
        val state = repository.refresh()
        assertEquals(null, state.snapshot)
        assertEquals(0, usage.getRequestCount())
        assertEquals(0, log.getAll().size)
        assertEquals(0L, usage.getLastFetchEpochMs()) // not throttled by a non-issued call
    }

    @Test
    fun networkError_isCountedAndLoggedAsIssuedAttempt() = runBlocking {
        val client = FakeClient(ProductionFetch(ApiResult.NetworkError, "/ecu/energy", 1L, "GET", ""))
        repo(client).refresh()
        assertEquals(1, usage.getRequestCount())
        assertEquals(1, log.getAll().size)
        assertFalse(log.getAll()[0].success)
    }
}
