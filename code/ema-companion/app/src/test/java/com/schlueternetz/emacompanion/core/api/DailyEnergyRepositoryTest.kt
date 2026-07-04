package com.schlueternetz.emacompanion.core.api

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.schlueternetz.emacompanion.core.api.log.ApiCallLogRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DailyEnergyRepositoryTest {
    private lateinit var usageCounter: ApiUsageRepository
    private lateinit var log: ApiCallLogRepository
    private lateinit var dailyPrefs: SharedPreferences
    private var now = 3_700_000L

    private class FakeClient(
        var result: ApiResult<DailySnapshot> = ApiResult.Success(DailySnapshot(emptyMap())),
    ) : EmaApiClient {
        var calls = 0
        var lastStart: String? = null
        var lastEnd: String? = null

        override suspend fun getCurrentProduction() = ProductionFetch(ApiResult.ConfigurationError)

        override suspend fun getBatchInverterEnergy(date: String) = BatchEnergyFetch(ApiResult.ConfigurationError)

        override suspend fun getDailyEnergy(
            startDate: String,
            endDate: String,
        ): DailyEnergyFetch {
            calls++
            lastStart = startDate
            lastEnd = endDate
            return DailyEnergyFetch(result, "/ecu/energy/test", 5L, "GET ...", "{}")
        }
    }

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        dailyPrefs = ctx.getSharedPreferences("daily_t", Context.MODE_PRIVATE)
        dailyPrefs.edit().clear().apply()
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
        usageCounter =
            ApiUsageRepository(
                ctx.getSharedPreferences("usage_t", Context.MODE_PRIVATE),
                monthProvider = { "2026-07" },
            )
        log = ApiCallLogRepository(ctx.getSharedPreferences("log_t", Context.MODE_PRIVATE))
    }

    private fun repo(
        client: EmaApiClient,
        today: String = "2026-07-15",
        historyDays: Int = 30,
    ) = DailyEnergyRepository(
        client = client,
        usageCounter = usageCounter,
        log = log,
        appSecretProvider = { "secret" },
        today = { today },
        historyDays = { historyDays },
        clock = { now },
        prefs = dailyPrefs,
    )

    // Seed past-day entries into prefs so loadDays() picks them up when the next repo is built
    private fun seedPastDays(vararg dates: String) {
        val edit = dailyPrefs.edit()
        dates.forEach { edit.putString("day_$it", "5.0") }
        edit.apply()
    }

    // ── First-fetch: full window ────────────────────────────────────────────

    @Test
    fun firstFetch_issuesFullWindowCall() =
        runBlocking {
            val client = FakeClient()
            repo(client, today = "2026-07-15", historyDays = 30).refresh()
            assertEquals(1, client.calls)
            assertEquals("2026-06-15", client.lastStart)
            assertEquals("2026-07-15", client.lastEnd)
        }

    // ── Steady-state: today-only call ─────────────────────────────────────

    @Test
    fun steadyState_issuesOnlyTodayCall() =
        runBlocking {
            // Seed all past days in window so no missing past days remain
            seedPastDays("2026-07-14", "2026-07-13", "2026-07-12")
            val client = FakeClient(result = ApiResult.Success(DailySnapshot(mapOf("2026-07-15" to 6.0))))
            val r = repo(client, today = "2026-07-15", historyDays = 3)
            r.refresh() // all past days cached → today-only
            assertEquals(1, client.calls)
            assertEquals("2026-07-15", client.lastStart)
            assertEquals("2026-07-15", client.lastEnd)
            // Advance time past throttle
            now += 3_700_000L
            r.refresh() // still steady-state
            assertEquals(2, client.calls)
            assertEquals("2026-07-15", client.lastStart)
        }

    // ── Today throttle ─────────────────────────────────────────────────────

    @Test
    fun throttle_preventsRepeatFetchWithinWindow() =
        runBlocking {
            val client = FakeClient()
            val r = repo(client)
            r.refresh()
            r.refresh()
            assertEquals(1, client.calls)
        }

    // ── force=true bypasses today throttle ────────────────────────────────

    @Test
    fun force_bypassesThrottleAndFetchesToday() =
        runBlocking {
            // Seed all past days so the force-refresh only fetches today
            seedPastDays("2026-07-14", "2026-07-13", "2026-07-12")
            val client = FakeClient(result = ApiResult.Success(DailySnapshot(mapOf("2026-07-15" to 6.0))))
            val r = repo(client, today = "2026-07-15", historyDays = 3)
            r.refresh()
            r.refresh(force = true) // within throttle, but force=true
            assertEquals(2, client.calls)
            // force still only re-fetches today (past days are cached)
            assertEquals("2026-07-15", client.lastStart)
            assertEquals("2026-07-15", client.lastEnd)
        }

    // ── Past days not re-fetched ────────────────────────────────────────

    @Test
    fun pastDays_notRefetchedWhenAlreadyCached() =
        runBlocking {
            // Seed past days BEFORE creating the repo so loadDays() picks them up
            seedPastDays("2026-07-12", "2026-07-13", "2026-07-14")
            val client = FakeClient(result = ApiResult.Success(DailySnapshot(mapOf("2026-07-15" to 6.0))))
            val r = repo(client, today = "2026-07-15", historyDays = 3)
            r.refresh()
            assertEquals(1, client.calls)
            assertEquals("2026-07-15", client.lastStart)
            assertEquals("2026-07-15", client.lastEnd)
        }

    // ── Success path ───────────────────────────────────────────────────────

    @Test
    fun successPath_countsAndThrottles() =
        runBlocking {
            val client = FakeClient(result = ApiResult.Success(DailySnapshot(mapOf("2026-07-15" to 5.0))))
            repo(client).refresh()
            assertEquals(1, usageCounter.getRequestCount())
            assertEquals(now, dailyPrefs.getLong("lastFetchMs", 0L))
        }

    // ── Failure path ───────────────────────────────────────────────────────

    @Test
    fun failurePath_doesNotCountOrThrottle() =
        runBlocking {
            val client = FakeClient(result = ApiResult.NetworkError)
            repo(client).refresh()
            assertEquals(0, usageCounter.getRequestCount())
            assertEquals(0L, dailyPrefs.getLong("lastFetchMs", 0L))
        }

    // ── currentState reconstruction ────────────────────────────────────────

    @Test
    fun currentState_reconstructsFromPrefs() =
        runBlocking {
            val client = FakeClient(result = ApiResult.Success(DailySnapshot(mapOf("2026-07-15" to 5.0))))
            val r = repo(client)
            r.refresh()
            val r2 = repo(FakeClient())
            val state = r2.currentState()
            assertEquals(5.0, state.snapshot?.days?.get("2026-07-15")!!, 0.001)
        }

    @Test
    fun currentState_errorPreservedAfterFailure() =
        runBlocking {
            val client = FakeClient(result = ApiResult.NetworkError)
            val r = repo(client)
            r.refresh()
            assertEquals(FetchError.NETWORK, r.currentState().error)
        }

    // ── resetThrottle ──────────────────────────────────────────────────────

    @Test
    fun resetThrottle_clearsLastFetchButKeepsCache() =
        runBlocking {
            val client = FakeClient(result = ApiResult.Success(DailySnapshot(mapOf("2026-07-15" to 5.0))))
            val r = repo(client)
            r.refresh()
            r.resetThrottle()
            assertEquals(0L, dailyPrefs.getLong("lastFetchMs", 0L))
            val state = r.currentState()
            assertEquals(5.0, state.snapshot?.days?.get("2026-07-15")!!, 0.001)
        }
}
