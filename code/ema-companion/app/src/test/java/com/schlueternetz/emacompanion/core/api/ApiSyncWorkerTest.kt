package com.schlueternetz.emacompanion.core.api

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.testing.TestListenableWorkerBuilder
import com.schlueternetz.emacompanion.core.HomeTile
import com.schlueternetz.emacompanion.core.HomeWidget
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

private object StubbedLifecycleOwner : LifecycleOwner {
    override val lifecycle: Lifecycle
        get() = throw UnsupportedOperationException("not used by AppForegroundTracker")
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ApiSyncWorkerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private var updateAllCallCount = 0

    @Before
    fun setUp() {
        context.getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("ema_api_sync_worker", Context.MODE_PRIVATE).edit().clear().commit()
        updateAllCallCount = 0
        ApiSyncWorker.updateAllAction = { _, _ -> updateAllCallCount++ }
        ApiSyncWorker.hasConsumingWidgetPlacedAction = { false }
        ApiSyncWorker.foregroundTrackerOverride = null
        ApiSyncWorker.currentArrayLocalTimeOverride = { LocalTime.of(10, 0) }
        ApiSyncWorker.currentLocalDateOverride = { LocalDate.of(2026, 7, 15) }
    }

    @After
    fun tearDown() {
        ApiSyncWorker.hourlySourceOverride = null
        ApiSyncWorker.dailySourceOverride = null
        ApiSyncWorker.widgetsOverride = null
        ApiSyncWorker.currentArrayLocalTimeOverride = null
        ApiSyncWorker.currentLocalDateOverride = null
        ApiSyncWorker.dailyCheckPrefsOverride = null
        ApiSyncWorker.foregroundTrackerOverride = null
        ApiSyncWorker.hasConsumingWidgetPlacedAction = ApiSyncWorker.defaultHasConsumingWidgetPlacedAction
        ApiSyncWorker.updateAllAction = ApiSyncWorker.defaultUpdateAllAction
    }

    // ── OPPORTUNISTIC: matches old WidgetRefreshWorker "hourly always, daily if needed" coverage ──

    @Test
    fun opportunistic_hourlyRefreshedWithoutForce_whenNeeded() =
        runBlocking {
            val hourly = FakeHourlySource(HourlyProductionState())
            ApiSyncWorker.hourlySourceOverride = hourly
            ApiSyncWorker.dailySourceOverride = FakeDailySource(DailyProductionState())

            runWorker(ApiSyncRequestKind.OPPORTUNISTIC)

            assertEquals(1, hourly.refreshCallCount)
            assertEquals(false, hourly.lastForce)
        }

    @Test
    fun opportunistic_hourlySkipped_whenNoEnabledConsumer() =
        runBlocking {
            disableAllHourlyAndDailyConsumers()
            val hourly = FakeHourlySource(HourlyProductionState())
            ApiSyncWorker.hourlySourceOverride = hourly
            ApiSyncWorker.dailySourceOverride = FakeDailySource(DailyProductionState())

            runWorker(ApiSyncRequestKind.OPPORTUNISTIC)

            assertEquals(0, hourly.refreshCallCount)
        }

    @Test
    fun opportunistic_dailySkipped_whenNoEnabledConsumer() =
        runBlocking {
            disableAllHourlyAndDailyConsumers()
            val daily = FakeDailySource(DailyProductionState())
            ApiSyncWorker.hourlySourceOverride = FakeHourlySource(HourlyProductionState())
            ApiSyncWorker.dailySourceOverride = daily

            runWorker(ApiSyncRequestKind.OPPORTUNISTIC)

            assertEquals(0, daily.refreshCallCount)
        }

    @Test
    fun opportunistic_successfulHourlyRefresh_updatesWidgets() =
        runBlocking {
            ApiSyncWorker.hourlySourceOverride = FakeHourlySource(HourlyProductionState())
            ApiSyncWorker.dailySourceOverride = FakeDailySource(DailyProductionState())

            runWorker(ApiSyncRequestKind.OPPORTUNISTIC)

            assertEquals(2, updateAllCallCount) // hourly branch + daily branch, both succeed
        }

    @Test
    fun opportunistic_failedHourlyRefresh_doesNotUpdateWidgetsForThatBranch() =
        runBlocking {
            ApiSyncWorker.hourlySourceOverride = FakeHourlySource(HourlyProductionState(error = FetchError.NETWORK))
            ApiSyncWorker.dailySourceOverride = FakeDailySource(DailyProductionState())

            runWorker(ApiSyncRequestKind.OPPORTUNISTIC)

            assertEquals(1, updateAllCallCount) // only the daily branch succeeded
        }

    // ── FORCED: bypasses each source's own throttle ─────────────────────────

    @Test
    fun forced_passesForceTrueToHourlySource() =
        runBlocking {
            val hourly = FakeHourlySource(HourlyProductionState())
            ApiSyncWorker.hourlySourceOverride = hourly
            ApiSyncWorker.dailySourceOverride = FakeDailySource(DailyProductionState())

            runWorker(ApiSyncRequestKind.FORCED)

            assertEquals(true, hourly.lastForce)
        }

    // ── SETTINGS_CHANGED: resets throttle regardless of isXDataNeeded, but only fetches if needed ──

    @Test
    fun settingsChanged_resetsThrottle_evenWhenNotNeeded() =
        runBlocking {
            disableAllHourlyAndDailyConsumers()
            val hourly = FakeHourlySource(HourlyProductionState())
            val daily = FakeDailySource(DailyProductionState())
            ApiSyncWorker.hourlySourceOverride = hourly
            ApiSyncWorker.dailySourceOverride = daily

            runWorker(ApiSyncRequestKind.SETTINGS_CHANGED)

            assertEquals(1, hourly.resetThrottleCallCount)
            assertEquals(1, daily.resetThrottleCallCount)
            assertEquals(0, hourly.refreshCallCount)
            assertEquals(0, daily.refreshCallCount)
        }

    @Test
    fun settingsChanged_fetchesWithoutForce_whenNeeded() =
        runBlocking {
            val hourly = FakeHourlySource(HourlyProductionState())
            ApiSyncWorker.hourlySourceOverride = hourly
            ApiSyncWorker.dailySourceOverride = FakeDailySource(DailyProductionState())

            runWorker(ApiSyncRequestKind.SETTINGS_CHANGED)

            assertEquals(1, hourly.refreshCallCount)
            assertEquals(false, hourly.lastForce)
        }

    // ── PERIODIC: daylight window ────────────────────────────────────────────

    @Test
    fun periodic_hourlySkipped_beforeDaylightWindow() =
        runBlocking {
            ApiSyncWorker.currentArrayLocalTimeOverride = { LocalTime.of(5, 59) }
            ApiSyncWorker.hasConsumingWidgetPlacedAction = { true }
            val hourly = FakeHourlySource(HourlyProductionState())
            ApiSyncWorker.hourlySourceOverride = hourly
            ApiSyncWorker.dailySourceOverride = FakeDailySource(DailyProductionState())

            runWorker(ApiSyncRequestKind.PERIODIC)

            assertEquals(0, hourly.refreshCallCount)
        }

    @Test
    fun periodic_hourlySkipped_afterDaylightWindow() =
        runBlocking {
            ApiSyncWorker.currentArrayLocalTimeOverride = { LocalTime.of(22, 0) }
            ApiSyncWorker.hasConsumingWidgetPlacedAction = { true }
            val hourly = FakeHourlySource(HourlyProductionState())
            ApiSyncWorker.hourlySourceOverride = hourly
            ApiSyncWorker.dailySourceOverride = FakeDailySource(DailyProductionState())

            runWorker(ApiSyncRequestKind.PERIODIC)

            assertEquals(0, hourly.refreshCallCount)
        }

    // ── PERIODIC: widget placement / app-foreground ──────────────────────────

    @Test
    fun periodic_hourlySkipped_noWidgetPlaced_appNotForegrounded() =
        runBlocking {
            ApiSyncWorker.hasConsumingWidgetPlacedAction = { false }
            ApiSyncWorker.foregroundTrackerOverride = stubForegroundTracker(recentlyForegrounded = false)
            val hourly = FakeHourlySource(HourlyProductionState())
            ApiSyncWorker.hourlySourceOverride = hourly
            ApiSyncWorker.dailySourceOverride = FakeDailySource(DailyProductionState())

            runWorker(ApiSyncRequestKind.PERIODIC)

            assertEquals(0, hourly.refreshCallCount)
        }

    @Test
    fun periodic_hourlyRuns_whenWidgetPlaced_withinWindow() =
        runBlocking {
            ApiSyncWorker.hasConsumingWidgetPlacedAction = { true }
            ApiSyncWorker.foregroundTrackerOverride = stubForegroundTracker(recentlyForegrounded = false)
            val hourly = FakeHourlySource(HourlyProductionState())
            ApiSyncWorker.hourlySourceOverride = hourly
            ApiSyncWorker.dailySourceOverride = FakeDailySource(DailyProductionState())

            runWorker(ApiSyncRequestKind.PERIODIC)

            assertEquals(1, hourly.refreshCallCount)
        }

    @Test
    fun periodic_hourlyRuns_whenAppRecentlyForegrounded_noWidgetPlaced() =
        runBlocking {
            ApiSyncWorker.hasConsumingWidgetPlacedAction = { false }
            ApiSyncWorker.foregroundTrackerOverride = stubForegroundTracker(recentlyForegrounded = true)
            val hourly = FakeHourlySource(HourlyProductionState())
            ApiSyncWorker.hourlySourceOverride = hourly
            ApiSyncWorker.dailySourceOverride = FakeDailySource(DailyProductionState())

            runWorker(ApiSyncRequestKind.PERIODIC)

            assertEquals(1, hourly.refreshCallCount)
        }

    // ── Daily-only consumer widens hourly's own trigger condition ───────────

    @Test
    fun periodic_hourlyRuns_whenOnlyDailyConsumerEnabled() =
        runBlocking {
            disableAllHourlyAndDailyConsumers()
            SettingsRepository.create(context).setTileEnabled(HomeTile.HISTORY_PRODUCTION, true)
            ApiSyncWorker.hasConsumingWidgetPlacedAction = { true }
            val hourly = FakeHourlySource(HourlyProductionState())
            ApiSyncWorker.hourlySourceOverride = hourly
            ApiSyncWorker.dailySourceOverride = FakeDailySource(DailyProductionState())

            runWorker(ApiSyncRequestKind.PERIODIC)

            assertEquals(1, hourly.refreshCallCount)
        }

    // ── PERIODIC: daily backfill check runs once per day, not window/placement-gated ──

    @Test
    fun periodic_dailyChecksOnce_regardlessOfWindowOrPlacement() =
        runBlocking {
            ApiSyncWorker.currentArrayLocalTimeOverride = { LocalTime.of(2, 0) } // outside daylight window
            ApiSyncWorker.hasConsumingWidgetPlacedAction = { false }
            ApiSyncWorker.foregroundTrackerOverride = stubForegroundTracker(recentlyForegrounded = false)
            val daily = FakeDailySource(DailyProductionState())
            ApiSyncWorker.hourlySourceOverride = FakeHourlySource(HourlyProductionState())
            ApiSyncWorker.dailySourceOverride = daily

            runWorker(ApiSyncRequestKind.PERIODIC)

            assertEquals(1, daily.refreshCallCount)
        }

    @Test
    fun periodic_dailySkipped_secondTriggerSameDay() =
        runBlocking {
            val daily = FakeDailySource(DailyProductionState())
            ApiSyncWorker.hourlySourceOverride = FakeHourlySource(HourlyProductionState())
            ApiSyncWorker.dailySourceOverride = daily

            runWorker(ApiSyncRequestKind.PERIODIC)
            runWorker(ApiSyncRequestKind.PERIODIC)

            assertEquals(1, daily.refreshCallCount)
        }

    @Test
    fun periodic_dailyChecksAgain_nextCalendarDay() =
        runBlocking {
            val daily = FakeDailySource(DailyProductionState())
            ApiSyncWorker.hourlySourceOverride = FakeHourlySource(HourlyProductionState())
            ApiSyncWorker.dailySourceOverride = daily

            runWorker(ApiSyncRequestKind.PERIODIC)
            ApiSyncWorker.currentLocalDateOverride = { LocalDate.of(2026, 7, 16) }
            runWorker(ApiSyncRequestKind.PERIODIC)

            assertEquals(2, daily.refreshCallCount)
        }

    @Test
    fun periodic_dailySkipped_whenNoEnabledConsumer() =
        runBlocking {
            disableAllHourlyAndDailyConsumers()
            val daily = FakeDailySource(DailyProductionState())
            ApiSyncWorker.hourlySourceOverride = FakeHourlySource(HourlyProductionState())
            ApiSyncWorker.dailySourceOverride = daily

            runWorker(ApiSyncRequestKind.PERIODIC)

            assertEquals(0, daily.refreshCallCount)
        }

    private fun disableAllHourlyAndDailyConsumers() {
        val settings = SettingsRepository.create(context)
        settings.setTileEnabled(HomeTile.TODAY_PRODUCTION, false)
        settings.setTileEnabled(HomeTile.HISTORY_PRODUCTION, false)
        settings.setWidgetEnabled(HomeWidget.TODAY_PRODUCTION, false)
        settings.setWidgetEnabled(HomeWidget.PRODUCTION_SUMMARY, false)
        settings.setWidgetEnabled(HomeWidget.PRODUCTION_HISTORY, false)
    }

    private var foregroundStubCounter = 0

    private fun stubForegroundTracker(recentlyForegrounded: Boolean): AppForegroundTracker {
        val prefs =
            context.getSharedPreferences("foreground_stub_${foregroundStubCounter++}", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        return AppForegroundTracker(prefs, clock = { 10_000_000L }).also {
            if (recentlyForegrounded) it.onStart(StubbedLifecycleOwner)
        }
    }

    private fun runWorker(kind: ApiSyncRequestKind) {
        val data = Data.Builder().putString(ApiSyncScheduler.KEY_REQUEST_KIND, kind.name).build()
        val worker = TestListenableWorkerBuilder<ApiSyncWorker>(context).setInputData(data).build()
        runBlocking { worker.doWork() }
    }

    private class FakeHourlySource(
        private val state: HourlyProductionState,
    ) : HourlyEnergySyncSource {
        var refreshCallCount = 0
        var lastForce: Boolean? = null
        var resetThrottleCallCount = 0

        override fun currentState(): HourlyProductionState = state

        override suspend fun refresh(force: Boolean): HourlyProductionState {
            refreshCallCount++
            lastForce = force
            return state
        }

        override fun resetThrottle() {
            resetThrottleCallCount++
        }
    }

    private class FakeDailySource(
        private val state: DailyProductionState,
    ) : DailyEnergySyncSource {
        var refreshCallCount = 0
        var lastForce: Boolean? = null
        var resetThrottleCallCount = 0

        override fun currentState(): DailyProductionState = state

        override suspend fun refresh(force: Boolean): DailyProductionState {
            refreshCallCount++
            lastForce = force
            return state
        }

        override fun resetThrottle() {
            resetThrottleCallCount++
        }
    }
}
