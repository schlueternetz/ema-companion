package com.schlueternetz.emacompanion.feature.widgets

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.TestListenableWorkerBuilder
import com.schlueternetz.emacompanion.core.HomeTile
import com.schlueternetz.emacompanion.core.HomeWidget
import com.schlueternetz.emacompanion.core.api.DailyEnergySource
import com.schlueternetz.emacompanion.core.api.DailyProductionState
import com.schlueternetz.emacompanion.core.api.FetchError
import com.schlueternetz.emacompanion.core.api.HourlyEnergySource
import com.schlueternetz.emacompanion.core.api.HourlyProductionState
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WidgetRefreshWorkerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private var updateAllCallCount = 0

    @Before
    fun setUp() {
        context
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        WidgetRefreshWorker.hourlySourceOverride = null
        WidgetRefreshWorker.dailySourceOverride = null
        WidgetRefreshWorker.hourOverride = null
        WidgetRefreshWorker.updateAllAction = WidgetRefreshWorker.defaultUpdateAllAction
    }

    @Test
    fun hourlyBranch_callsRefreshWithoutForce_atAnyHour() =
        runBlocking {
            val hourly = FakeHourlySource(HourlyProductionState())
            seed(hourly = hourly, daily = FakeDailySource(DailyProductionState()), hour = 10)

            runWorker()

            assertEquals(1, hourly.refreshCallCount)
            assertEquals(false, hourly.lastForce)
        }

    @Test
    fun dailyBranch_runsOnlyAtDailyTriggerHour() =
        runBlocking {
            val daily = FakeDailySource(DailyProductionState())
            seed(hourly = FakeHourlySource(HourlyProductionState()), daily = daily, hour = 10)

            runWorker()

            assertEquals(0, daily.refreshCallCount)
        }

    @Test
    fun dailyBranch_callsRefreshWithoutForce_atTriggerHour() =
        runBlocking {
            val daily = FakeDailySource(DailyProductionState())
            seed(
                hourly = FakeHourlySource(HourlyProductionState()),
                daily = daily,
                hour = WidgetRefreshWorker.DAILY_TRIGGER_HOUR,
            )

            runWorker()

            assertEquals(1, daily.refreshCallCount)
            assertEquals(false, daily.lastForce)
        }

    @Test
    fun hourlyBranch_skipsRefresh_whenNoEnabledConsumer() =
        runBlocking {
            val settings = SettingsRepository.create(context)
            settings.setTileEnabled(HomeTile.TODAY_PRODUCTION, false)
            settings.setWidgetEnabled(HomeWidget.TODAY_PRODUCTION, false)
            settings.setWidgetEnabled(HomeWidget.PRODUCTION_SUMMARY, false)
            val hourly = FakeHourlySource(HourlyProductionState())
            seed(hourly = hourly, daily = FakeDailySource(DailyProductionState()), hour = 10)

            runWorker()

            assertEquals(0, hourly.refreshCallCount)
        }

    @Test
    fun dailyBranch_skipsRefresh_whenNoEnabledConsumer() =
        runBlocking {
            val settings = SettingsRepository.create(context)
            settings.setTileEnabled(HomeTile.TODAY_PRODUCTION, false)
            settings.setTileEnabled(HomeTile.HISTORY_PRODUCTION, false)
            settings.setWidgetEnabled(HomeWidget.PRODUCTION_SUMMARY, false)
            settings.setWidgetEnabled(HomeWidget.PRODUCTION_HISTORY, false)
            val daily = FakeDailySource(DailyProductionState())
            seed(
                hourly = FakeHourlySource(HourlyProductionState()),
                daily = daily,
                hour = WidgetRefreshWorker.DAILY_TRIGGER_HOUR,
            )

            runWorker()

            assertEquals(0, daily.refreshCallCount)
        }

    @Test
    fun successfulHourlyRefresh_updatesWidgets() =
        runBlocking {
            seed(
                hourly = FakeHourlySource(HourlyProductionState()),
                daily = FakeDailySource(DailyProductionState()),
                hour = 10,
            )

            runWorker()

            assertEquals(1, updateAllCallCount)
        }

    @Test
    fun failedHourlyRefresh_doesNotUpdateWidgets() =
        runBlocking {
            seed(
                hourly = FakeHourlySource(HourlyProductionState(error = FetchError.NETWORK)),
                daily = FakeDailySource(DailyProductionState()),
                hour = 10,
            )

            runWorker()

            assertEquals(0, updateAllCallCount)
        }

    @Test
    fun successfulDailyRefresh_updatesWidgetsAgain() =
        runBlocking {
            seed(
                hourly = FakeHourlySource(HourlyProductionState()),
                daily = FakeDailySource(DailyProductionState()),
                hour = WidgetRefreshWorker.DAILY_TRIGGER_HOUR,
            )

            runWorker()

            // once for the hourly branch, once for the daily branch
            assertEquals(2, updateAllCallCount)
        }

    @Test
    fun failedDailyRefresh_doesNotUpdateWidgetsForThatBranch() =
        runBlocking {
            seed(
                hourly = FakeHourlySource(HourlyProductionState()),
                daily = FakeDailySource(DailyProductionState(error = FetchError.API)),
                hour = WidgetRefreshWorker.DAILY_TRIGGER_HOUR,
            )

            runWorker()

            // hourly branch succeeded (1), daily branch failed (0 more)
            assertEquals(1, updateAllCallCount)
        }

    private fun seed(
        hourly: HourlyEnergySource,
        daily: DailyEnergySource,
        hour: Int,
    ) {
        WidgetRefreshWorker.hourlySourceOverride = hourly
        WidgetRefreshWorker.dailySourceOverride = daily
        WidgetRefreshWorker.hourOverride = { hour }
        WidgetRefreshWorker.updateAllAction = { _, _ -> updateAllCallCount++ }
    }

    private fun runWorker() {
        val worker = TestListenableWorkerBuilder<WidgetRefreshWorker>(context).build()
        runBlocking { worker.doWork() }
    }

    private class FakeHourlySource(
        private val state: HourlyProductionState,
    ) : HourlyEnergySource {
        var refreshCallCount = 0
        var lastForce: Boolean? = null

        override fun currentState(): HourlyProductionState = state

        override suspend fun refresh(force: Boolean): HourlyProductionState {
            refreshCallCount++
            lastForce = force
            return state
        }
    }

    private class FakeDailySource(
        private val state: DailyProductionState,
    ) : DailyEnergySource {
        var refreshCallCount = 0
        var lastForce: Boolean? = null

        override fun currentState(): DailyProductionState = state

        override suspend fun refresh(force: Boolean): DailyProductionState {
            refreshCallCount++
            lastForce = force
            return state
        }
    }
}
