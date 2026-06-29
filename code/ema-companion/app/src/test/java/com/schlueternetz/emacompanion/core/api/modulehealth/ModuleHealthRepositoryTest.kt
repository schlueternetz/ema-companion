package com.schlueternetz.emacompanion.core.api.modulehealth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.schlueternetz.emacompanion.core.AppConfig
import com.schlueternetz.emacompanion.core.api.ApiResult
import com.schlueternetz.emacompanion.core.api.BatchEnergyFetch
import com.schlueternetz.emacompanion.core.api.EmaApiClient
import com.schlueternetz.emacompanion.core.api.FetchError
import com.schlueternetz.emacompanion.core.api.ProductionFetch
import com.schlueternetz.emacompanion.core.api.ProductionSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ModuleHealthRepositoryTest {
    private lateinit var context: Context
    private lateinit var fakeClient: FakeClient
    private lateinit var repo: ModuleHealthRepository

    private val today = LocalDate.of(2025, 7, 24)
    private val yesterday = today.minusDays(1)
    private val dayBefore = today.minusDays(2)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context
            .getSharedPreferences(ModuleHealthRepository.PREFS_HEALTH, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context
            .getSharedPreferences(ModuleHealthRepository.PREFS_DAILY, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        fakeClient = FakeClient()
        repo = ModuleHealthRepository.forTest(context, fakeClient, today = { today })
    }

    // ── computeStatus tests (pure logic, 4.2) ────────────────────────────────

    @Test
    fun computeStatus_allProducing_returnsGreen() {
        val window =
            mapOf(
                today to mapOf("INV1" to 1.5, "INV2" to 0.8),
                yesterday to mapOf("INV1" to 1.2, "INV2" to 0.9),
                dayBefore to mapOf("INV1" to 1.1, "INV2" to 1.0),
            )
        val state = repo.computeStatus(window)
        assertEquals(ModuleHealthStatus.GREEN, state.status)
        assertEquals(emptyList<Module>(), state.offlineModules)
    }

    @Test
    fun computeStatus_oneInverterOfflineToday_returnsYellow() {
        val window =
            mapOf(
                today to mapOf("INV1" to 0.0, "INV2" to 0.8),
                yesterday to mapOf("INV1" to 1.2, "INV2" to 0.9),
                dayBefore to mapOf("INV1" to 1.1, "INV2" to 1.0),
            )
        val state = repo.computeStatus(window)
        assertEquals(ModuleHealthStatus.YELLOW, state.status)
        assertEquals(1, state.offlineModules.size)
        assertEquals("INV1", state.offlineModules[0].uid)
        assertEquals(1, state.offlineModules[0].offlineDays)
    }

    @Test
    fun computeStatus_oneInverterOfflineTwoDays_returnsYellow() {
        val window =
            mapOf(
                today to mapOf("INV1" to 0.0, "INV2" to 0.8),
                yesterday to mapOf("INV1" to 0.0, "INV2" to 0.9),
                dayBefore to mapOf("INV1" to 1.1, "INV2" to 1.0),
            )
        val state = repo.computeStatus(window)
        assertEquals(ModuleHealthStatus.YELLOW, state.status)
        assertEquals(1, state.offlineModules.size)
        assertEquals(2, state.offlineModules[0].offlineDays)
    }

    @Test
    fun computeStatus_oneInverterOfflineThreeDays_returnsRed() {
        val window =
            mapOf(
                today to mapOf("INV1" to 0.0, "INV2" to 0.8),
                yesterday to mapOf("INV1" to 0.0, "INV2" to 0.9),
                dayBefore to mapOf("INV1" to 0.0, "INV2" to 1.0),
            )
        val state = repo.computeStatus(window)
        assertEquals(ModuleHealthStatus.RED, state.status)
        assertEquals(1, state.offlineModules.size)
        assertEquals("INV1", state.offlineModules[0].uid)
        assertEquals(3, state.offlineModules[0].offlineDays)
    }

    @Test
    fun computeStatus_absentInverterTreatedAsZero() {
        // INV1 is present in yesterday + dayBefore but absent from today → treated as 0
        val window =
            mapOf(
                today to mapOf("INV2" to 0.8),
                yesterday to mapOf("INV1" to 1.2, "INV2" to 0.9),
                dayBefore to mapOf("INV1" to 1.1, "INV2" to 1.0),
            )
        val state = repo.computeStatus(window)
        assertEquals(ModuleHealthStatus.YELLOW, state.status)
        assertEquals("INV1", state.offlineModules[0].uid)
        assertEquals(1, state.offlineModules[0].offlineDays)
    }

    @Test
    fun computeStatus_emptyWindow_returnsUnknown() {
        val state = repo.computeStatus(emptyMap())
        assertEquals(ModuleHealthStatus.UNKNOWN, state.status)
    }

    @Test
    fun computeStatus_noInvertersInWindow_returnsGreen() {
        val window =
            mapOf(
                today to emptyMap<String, Double>(),
            )
        val state = repo.computeStatus(window)
        assertEquals(ModuleHealthStatus.GREEN, state.status)
    }

    // ── currentState before any check ────────────────────────────────────────

    @Test
    fun currentState_beforeAnyCheck_returnsUnknown() {
        val state = repo.currentState()
        assertEquals(ModuleHealthStatus.UNKNOWN, state.status)
        assertNull(state.checkedAtEpochMs)
    }

    // ── throttle tests (4.3) ──────────────────────────────────────────────────

    @Test
    fun refresh_withinThrottle_makesNoApiCalls() {
        fakeClient.responses[today.toString()] = mapOf("INV1" to 1.5)
        fakeClient.responses[yesterday.toString()] = mapOf("INV1" to 1.2)
        fakeClient.responses[dayBefore.toString()] = mapOf("INV1" to 1.1)

        // First refresh succeeds and starts throttle
        val repo1 =
            ModuleHealthRepository.forTest(
                context,
                fakeClient,
                today = { today },
                clock = { 1000L },
            )
        kotlinx.coroutines.runBlocking { repo1.refresh() }
        val callsAfterFirst = fakeClient.callCount

        // Second call within throttle window
        val repo2 =
            ModuleHealthRepository.forTest(
                context,
                fakeClient,
                today = { today },
                clock = { 1000L + AppConfig.MODULE_HEALTH_CHECK_INTERVAL_MS / 2 },
            )
        kotlinx.coroutines.runBlocking { repo2.refresh() }
        assertEquals("within throttle — no additional calls expected", callsAfterFirst, fakeClient.callCount)
    }

    @Test
    fun refresh_throttleExpired_fetchesAgain() {
        fakeClient.responses[today.toString()] = mapOf("INV1" to 1.5)
        fakeClient.responses[yesterday.toString()] = mapOf("INV1" to 1.2)
        fakeClient.responses[dayBefore.toString()] = mapOf("INV1" to 1.1)

        val repo1 =
            ModuleHealthRepository.forTest(
                context,
                fakeClient,
                today = { today },
                clock = { 0L },
            )
        kotlinx.coroutines.runBlocking { repo1.refresh() }

        // Past the throttle window
        val nextDay = today.plusDays(1)
        fakeClient.responses[nextDay.toString()] = mapOf("INV1" to 1.3)
        val repo2 =
            ModuleHealthRepository.forTest(
                context,
                fakeClient,
                today = { nextDay },
                clock = { AppConfig.MODULE_HEALTH_CHECK_INTERVAL_MS + 1L },
            )
        val callsBefore = fakeClient.callCount
        kotlinx.coroutines.runBlocking { repo2.refresh() }
        assertEquals("after throttle expiry — 1 new call for today", callsBefore + 1, fakeClient.callCount)
    }

    // ── incremental fetch tests (4.4, 4.7, 4.8) ─────────────────────────────

    @Test
    fun refresh_normalCase_makesExactlyOneApiCall() {
        // Pre-cache yesterday and day-before; only today should be fetched
        fakeClient.responses[today.toString()] = mapOf("INV1" to 1.5)
        val healthPrefs = context.getSharedPreferences(ModuleHealthRepository.PREFS_HEALTH, Context.MODE_PRIVATE)
        val dailyPrefs = context.getSharedPreferences(ModuleHealthRepository.PREFS_DAILY, Context.MODE_PRIVATE)
        dailyPrefs
            .edit()
            .putString("daily_$yesterday", """{"INV1":1.2}""")
            .putString("daily_$dayBefore", """{"INV1":1.1}""")
            .commit()

        kotlinx.coroutines.runBlocking { repo.refresh() }

        assertEquals("only today should be fetched", 1, fakeClient.callCount)
    }

    @Test
    fun refresh_catchUpTwoDaysMissing_makesTwoApiCalls() {
        fakeClient.responses[today.toString()] = mapOf("INV1" to 1.5)
        fakeClient.responses[yesterday.toString()] = mapOf("INV1" to 1.2)
        // dayBefore is cached
        val dailyPrefs = context.getSharedPreferences(ModuleHealthRepository.PREFS_DAILY, Context.MODE_PRIVATE)
        dailyPrefs.edit().putString("daily_$dayBefore", """{"INV1":1.1}""").commit()

        kotlinx.coroutines.runBlocking { repo.refresh() }

        assertEquals("yesterday + today need fetching", 2, fakeClient.callCount)
    }

    @Test
    fun refresh_successfulCheck_persistsState() {
        fakeClient.responses[today.toString()] = mapOf("INV1" to 0.0, "INV2" to 1.5)
        fakeClient.responses[yesterday.toString()] = mapOf("INV1" to 0.0, "INV2" to 1.2)
        fakeClient.responses[dayBefore.toString()] = mapOf("INV1" to 1.0, "INV2" to 1.1)

        kotlinx.coroutines.runBlocking { repo.refresh() }

        val loaded = repo.currentState()
        assertEquals(ModuleHealthStatus.YELLOW, loaded.status)
        assertEquals(1, loaded.offlineModules.size)
        assertEquals("INV1", loaded.offlineModules[0].uid)
    }

    // ── cache pruning (4.5) ───────────────────────────────────────────────────

    @Test
    fun refresh_prunesOldCacheEntries() {
        val oldDate = today.minusDays(10)
        fakeClient.responses[today.toString()] = mapOf("INV1" to 1.5)
        fakeClient.responses[yesterday.toString()] = mapOf("INV1" to 1.2)
        fakeClient.responses[dayBefore.toString()] = mapOf("INV1" to 1.1)

        val dailyPrefs = context.getSharedPreferences(ModuleHealthRepository.PREFS_DAILY, Context.MODE_PRIVATE)
        dailyPrefs.edit().putString("daily_$oldDate", """{"INV1":2.0}""").commit()

        kotlinx.coroutines.runBlocking { repo.refresh() }

        assertNull(
            "old entry should be pruned",
            dailyPrefs.getString("daily_$oldDate", null),
        )
    }

    // ── api failure doesn't update state ─────────────────────────────────────

    @Test
    fun refresh_apiFails_preservesPreviousState() {
        // Seed a GREEN state
        val healthPrefs = context.getSharedPreferences(ModuleHealthRepository.PREFS_HEALTH, Context.MODE_PRIVATE)
        healthPrefs
            .edit()
            .putString("status", ModuleHealthStatus.GREEN.name)
            .putLong("lastCheckEpochMs", 0L) // zero so throttle is expired
            .commit()
        fakeClient.nextError = ApiResult.NetworkError

        kotlinx.coroutines.runBlocking { repo.refresh() }

        assertEquals(ModuleHealthStatus.GREEN, repo.currentState().status)
    }

    // ── error handling (ADR-006) ──────────────────────────────────────────────

    @Test
    fun refresh_networkError_setsErrorInState() {
        fakeClient.nextError = ApiResult.NetworkError
        val state = kotlinx.coroutines.runBlocking { repo.refresh() }
        assertEquals(FetchError.NETWORK, state.error)
    }

    @Test
    fun refresh_apiError_setsApiErrorInState() {
        fakeClient.nextError = ApiResult.ApiError(code = 4000)
        val state = kotlinx.coroutines.runBlocking { repo.refresh() }
        assertEquals(FetchError.API, state.error)
    }

    @Test
    fun refresh_authApiError_setsAuthErrorInState() {
        fakeClient.nextError = ApiResult.ApiError(code = 2001)
        val state = kotlinx.coroutines.runBlocking { repo.refresh() }
        assertEquals(FetchError.AUTH, state.error)
    }

    @Test
    fun refresh_networkError_errorPersistedAcrossRecreation() {
        fakeClient.nextError = ApiResult.NetworkError
        kotlinx.coroutines.runBlocking { repo.refresh() }
        val repo2 = ModuleHealthRepository.forTest(context, fakeClient, today = { today })
        assertEquals(FetchError.NETWORK, repo2.currentState().error)
    }

    @Test
    fun refresh_successAfterError_clearsError() {
        fakeClient.nextError = ApiResult.NetworkError
        kotlinx.coroutines.runBlocking { repo.refresh() }
        fakeClient.responses[today.toString()] = mapOf("INV1" to 1.5)
        fakeClient.responses[yesterday.toString()] = mapOf("INV1" to 1.2)
        fakeClient.responses[dayBefore.toString()] = mapOf("INV1" to 1.1)
        val repo2 =
            ModuleHealthRepository.forTest(
                context,
                fakeClient,
                today = { today },
                clock = { AppConfig.MODULE_HEALTH_CHECK_INTERVAL_MS + 1L },
            )
        val state = kotlinx.coroutines.runBlocking { repo2.refresh() }
        assertNull(state.error)
    }

    // ── RED latch (status stays RED until GREEN) ─────────────────────────────

    @Test
    fun refresh_previousRed_computedYellow_latchesToRed() {
        val healthPrefs = context.getSharedPreferences(ModuleHealthRepository.PREFS_HEALTH, Context.MODE_PRIVATE)
        healthPrefs
            .edit()
            .putString("status", ModuleHealthStatus.RED.name)
            .putLong("lastCheckEpochMs", 0L)
            .commit()
        // Data that pure computeStatus() would classify as YELLOW (INV1 offline 1 day)
        fakeClient.responses[today.toString()] = mapOf("INV1" to 0.0, "INV2" to 1.5)
        fakeClient.responses[yesterday.toString()] = mapOf("INV1" to 1.2, "INV2" to 1.2)
        fakeClient.responses[dayBefore.toString()] = mapOf("INV1" to 1.1, "INV2" to 1.1)

        val state = kotlinx.coroutines.runBlocking { repo.refresh() }

        assertEquals("RED latches until GREEN", ModuleHealthStatus.RED, state.status)
    }

    @Test
    fun refresh_previousRed_computedGreen_clearsToGreen() {
        val healthPrefs = context.getSharedPreferences(ModuleHealthRepository.PREFS_HEALTH, Context.MODE_PRIVATE)
        healthPrefs
            .edit()
            .putString("status", ModuleHealthStatus.RED.name)
            .putLong("lastCheckEpochMs", 0L)
            .commit()
        // All modules producing — computes GREEN
        fakeClient.responses[today.toString()] = mapOf("INV1" to 1.5, "INV2" to 1.5)
        fakeClient.responses[yesterday.toString()] = mapOf("INV1" to 1.2, "INV2" to 1.2)
        fakeClient.responses[dayBefore.toString()] = mapOf("INV1" to 1.1, "INV2" to 1.1)

        val state = kotlinx.coroutines.runBlocking { repo.refresh() }

        assertEquals("RED clears on GREEN recovery", ModuleHealthStatus.GREEN, state.status)
    }

    // ── lastNotifiedStatus ───────────────────────────────────────────────────

    @Test
    fun getLastNotifiedStatus_initially_returnsNull() {
        assertNull(repo.getLastNotifiedStatus())
    }

    @Test
    fun setLastNotifiedStatus_roundtrip() {
        repo.setLastNotifiedStatus(ModuleHealthStatus.YELLOW)
        assertEquals(ModuleHealthStatus.YELLOW, repo.getLastNotifiedStatus())
    }

    @Test
    fun setLastNotifiedStatus_persistsAcrossRecreation() {
        repo.setLastNotifiedStatus(ModuleHealthStatus.RED)
        val repo2 = ModuleHealthRepository.forTest(context, fakeClient, today = { today })
        assertEquals(ModuleHealthStatus.RED, repo2.getLastNotifiedStatus())
    }

    // ── resetThrottle (settings-change abstraction) ──────────────────────────

    @Test
    fun resetThrottle_clearsCheckTimestamp_allowingImmediateRefresh() {
        // Simulate an active 24-hour throttle: seed a recent lastCheckEpochMs
        val healthPrefs = context.getSharedPreferences(ModuleHealthRepository.PREFS_HEALTH, Context.MODE_PRIVATE)
        healthPrefs.edit().putLong("lastCheckEpochMs", System.currentTimeMillis()).commit()

        repo.resetThrottle()

        assertFalse(
            "lastCheckEpochMs should be removed so refresh skips the throttle check",
            healthPrefs.contains("lastCheckEpochMs"),
        )
    }

    @Test
    fun resetThrottle_clearsFetchError() {
        val healthPrefs = context.getSharedPreferences(ModuleHealthRepository.PREFS_HEALTH, Context.MODE_PRIVATE)
        healthPrefs.edit().putString("fetchError", "NETWORK").commit()

        repo.resetThrottle()

        assertNull(
            "stale fetch error should be cleared on settings change",
            healthPrefs.getString("fetchError", null),
        )
    }

    @Test
    fun resetThrottle_clearsLastNotifiedStatus() {
        repo.setLastNotifiedStatus(ModuleHealthStatus.RED)

        repo.resetThrottle()

        assertNull(
            "lastNotifiedStatus must be cleared so the next check re-fires notifications",
            repo.getLastNotifiedStatus(),
        )
    }

    // ── lastEmailedStatus ────────────────────────────────────────────────────

    @Test
    fun getLastEmailedStatus_initially_returnsNull() {
        assertNull(repo.getLastEmailedStatus())
    }

    @Test
    fun setLastEmailedStatus_roundtrip() {
        repo.setLastEmailedStatus(ModuleHealthStatus.RED)
        assertEquals(ModuleHealthStatus.RED, repo.getLastEmailedStatus())
    }

    @Test
    fun setLastEmailedStatus_persistsAcrossRecreation() {
        repo.setLastEmailedStatus(ModuleHealthStatus.GREEN)
        val repo2 = ModuleHealthRepository.forTest(context, fakeClient, today = { today })
        assertEquals(ModuleHealthStatus.GREEN, repo2.getLastEmailedStatus())
    }

    @Test
    fun resetThrottle_clearsLastEmailedStatus() {
        repo.setLastEmailedStatus(ModuleHealthStatus.YELLOW)

        repo.resetThrottle()

        assertNull(
            "lastEmailedStatus must be cleared on credential/settings change",
            repo.getLastEmailedStatus(),
        )
    }

    // ── helper ───────────────────────────────────────────────────────────────

    class FakeClient : EmaApiClient {
        val responses = mutableMapOf<String, Map<String, Double>>()
        var nextError: ApiResult<Map<String, Double>>? = null
        var callCount = 0

        override suspend fun getCurrentProduction(): ProductionFetch = ProductionFetch(ApiResult.Success(ProductionSnapshot(0)))

        override suspend fun getBatchInverterEnergy(date: String): BatchEnergyFetch {
            callCount++
            nextError?.also { err ->
                nextError = null
                return BatchEnergyFetch(err)
            }
            val data = responses[date] ?: return BatchEnergyFetch(ApiResult.ApiError(code = 4000))
            return BatchEnergyFetch(ApiResult.Success(data))
        }
    }
}
