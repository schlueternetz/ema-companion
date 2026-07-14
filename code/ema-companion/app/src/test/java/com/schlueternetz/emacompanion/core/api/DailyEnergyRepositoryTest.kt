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
        todayTotalProvider: () -> Double? = { null },
    ) = DailyEnergyRepository(
        client = client,
        usageCounter = usageCounter,
        log = log,
        appSecretProvider = { "secret" },
        today = { today },
        historyDays = { historyDays },
        clock = { now },
        prefs = dailyPrefs,
        todayTotalProvider = todayTotalProvider,
    )

    // Seed past-day entries into prefs so loadDays() picks them up when the next repo is built
    private fun seedPastDays(vararg dates: String) {
        val edit = dailyPrefs.edit()
        dates.forEach { edit.putString("day_$it", "5.0") }
        edit.apply()
    }

    // ── First-fetch: full window backfill ───────────────────────────────────

    @Test
    fun firstFetch_issuesFullWindowCall() =
        runBlocking {
            val client = FakeClient()
            repo(client, today = "2026-07-15", historyDays = 30).refresh()
            assertEquals(1, client.calls)
            assertEquals("2026-06-15", client.lastStart)
            assertEquals("2026-07-15", client.lastEnd)
        }

    // ── Steady state: today derived from hourly, zero API cost ─────────────

    @Test
    fun steadyState_derivesTodayFromHourly_noApiCall() =
        runBlocking {
            seedPastDays("2026-07-14", "2026-07-13", "2026-07-12")
            val client = FakeClient()
            val r = repo(client, today = "2026-07-15", historyDays = 3, todayTotalProvider = { 7.5 })
            val state = r.refresh()
            assertEquals(0, client.calls)
            assertEquals(7.5, state.snapshot?.days?.get("2026-07-15")!!, 0.001)
        }

    @Test
    fun steadyState_withNoDerivedTotalYet_leavesCacheUntouched() =
        runBlocking {
            seedPastDays("2026-07-14", "2026-07-13", "2026-07-12")
            val client = FakeClient()
            val r = repo(client, today = "2026-07-15", historyDays = 3, todayTotalProvider = { null })
            val state = r.refresh()
            assertEquals(0, client.calls)
            assertNull(state.snapshot?.days?.get("2026-07-15"))
        }

    @Test
    fun steadyState_repeatedCalls_keepDerivingWithoutApiCalls() =
        runBlocking {
            seedPastDays("2026-07-14", "2026-07-13", "2026-07-12")
            val client = FakeClient()
            var derived = 1.0
            val r = repo(client, today = "2026-07-15", historyDays = 3, todayTotalProvider = { derived })
            r.refresh()
            derived = 2.0
            now += 60_000L
            val state = r.refresh(force = true)
            assertEquals(0, client.calls)
            assertEquals(2.0, state.snapshot?.days?.get("2026-07-15")!!, 0.001)
        }

    // ── Day rollover: exactly one backfill call for the newly-completed day ─

    @Test
    fun dayRollover_backfillsExactlyTheMissingDay() =
        runBlocking {
            // Yesterday (07-14) was never locked in — simulates the day just having rolled over
            seedPastDays("2026-07-12", "2026-07-13")
            val client = FakeClient(result = ApiResult.Success(DailySnapshot(mapOf("2026-07-14" to 6.0))))
            val r = repo(client, today = "2026-07-15", historyDays = 3)
            r.refresh()
            assertEquals(1, client.calls)
            assertEquals("2026-07-12", client.lastStart)
            assertEquals("2026-07-15", client.lastEnd)
        }

    @Test
    fun dayRollover_lockedInDayNeverRefetchedAgain() =
        runBlocking {
            seedPastDays("2026-07-12", "2026-07-13")
            val client =
                FakeClient(
                    result = ApiResult.Success(DailySnapshot(mapOf("2026-07-14" to 6.0))),
                )
            val r = repo(client, today = "2026-07-15", historyDays = 3, todayTotalProvider = { 1.0 })
            r.refresh() // backfills 07-14
            assertEquals(1, client.calls)
            now += 60_000L
            r.refresh() // no longer missing → derives today instead
            assertEquals(1, client.calls)
        }

    @Test
    fun backfillFailure_doesNotMarkDayCached_retriesOnNextTrigger() =
        runBlocking {
            seedPastDays("2026-07-12", "2026-07-13")
            val client = FakeClient(result = ApiResult.NetworkError)
            val r = repo(client, today = "2026-07-15", historyDays = 3)
            r.refresh()
            assertEquals(1, client.calls)
            now += 60_000L
            r.refresh()
            assertEquals(2, client.calls)
            assertEquals("2026-07-12", client.lastStart)
        }

    // ── Past days never re-fetched ────────────────────────────────────────

    @Test
    fun pastDays_notRefetchedWhenAlreadyCached() =
        runBlocking {
            seedPastDays("2026-07-12", "2026-07-13", "2026-07-14")
            val client = FakeClient()
            val r = repo(client, today = "2026-07-15", historyDays = 3, todayTotalProvider = { 1.0 })
            r.refresh()
            assertEquals(0, client.calls)
        }

    // ── Success path ───────────────────────────────────────────────────────

    @Test
    fun successPath_countsRequestAndUpdatesTimestamp() =
        runBlocking {
            val client = FakeClient(result = ApiResult.Success(DailySnapshot(mapOf("2026-07-15" to 5.0))))
            val r = repo(client)
            val state = r.refresh()
            assertEquals(1, usageCounter.getRequestCount())
            assertEquals(now, state.updatedAtEpochMs)
        }

    // ── Failure path ───────────────────────────────────────────────────────

    @Test
    fun failurePath_doesNotCountRequest() =
        runBlocking {
            val client = FakeClient(result = ApiResult.NetworkError)
            repo(client).refresh()
            assertEquals(0, usageCounter.getRequestCount())
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
    fun resetThrottle_clearsErrorButKeepsCache() =
        runBlocking {
            val failing = FakeClient(result = ApiResult.NetworkError)
            val r = repo(failing)
            r.refresh()
            assertEquals(FetchError.NETWORK, r.currentState().error)

            r.resetThrottle()
            assertNull(r.currentState().error)
        }
}
