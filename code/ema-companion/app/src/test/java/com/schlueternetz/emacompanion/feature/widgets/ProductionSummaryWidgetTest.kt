package com.schlueternetz.emacompanion.feature.widgets

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.HomeWidget
import com.schlueternetz.emacompanion.core.api.DailyEnergySource
import com.schlueternetz.emacompanion.core.api.DailyProductionState
import com.schlueternetz.emacompanion.core.api.DailySnapshot
import com.schlueternetz.emacompanion.core.api.FetchError
import com.schlueternetz.emacompanion.core.api.HourlyEnergySource
import com.schlueternetz.emacompanion.core.api.HourlyProductionState
import com.schlueternetz.emacompanion.core.api.HourlySnapshot
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProductionSummaryWidgetTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        context
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        settings = SettingsRepository.create(context)
        settings.setEmaAppId("a".repeat(32))
        settings.setEmaAppSecret("b".repeat(12))
        settings.setEmaSystemId("c".repeat(16))
        settings.setEmaEcuId("1".repeat(12))
        settings.setSystemCapacity(5f)
    }

    @After
    fun tearDown() {
        ProductionSummaryWidget.hourlySourceOverride = null
        ProductionSummaryWidget.dailySourceOverride = null
        ProductionSummaryWidget.todayOverride = null
    }

    @Test
    fun allThreeTotals_displayedWhenDataCached() =
        runTest {
            seed(
                hourly = HourlyProductionState(snapshot = HourlySnapshot(mapOf(6 to 1.0, 7 to 2.5))),
                daily =
                    DailyProductionState(
                        snapshot = DailySnapshot(mapOf("2026-07-15" to 4.0, "2026-06-20" to 3.0)),
                    ),
            )

            runWidget {
                onNode(hasText(context.getString(R.string.home_today_value_kwh, 3.5))).assertExists()
                onNode(hasText(context.getString(R.string.home_today_value_kwh, 4.0))).assertExists()
                onNode(hasText(context.getString(R.string.home_today_value_kwh, 7.0))).assertExists()
            }
        }

    @Test
    fun missingData_showsZeroPerFigure() =
        runTest {
            seed(hourly = HourlyProductionState(), daily = DailyProductionState())

            runWidget {
                onAllNodes(hasText(context.getString(R.string.home_today_value_kwh, 0.0))).assertCountEquals(3)
            }
        }

    @Test
    fun notConfigured_showsPlaceholderOnly() =
        runTest {
            settings.clearAll()
            seed(
                hourly = HourlyProductionState(snapshot = HourlySnapshot(mapOf(6 to 1.0))),
                daily = DailyProductionState(snapshot = DailySnapshot(mapOf("2026-07-15" to 4.0))),
            )

            runWidget {
                onNode(hasText(context.getString(R.string.widget_not_configured))).assertExists()
            }
        }

    @Test
    fun hourlyError_replacesOnlyTodayFigure() =
        runTest {
            seed(
                hourly = HourlyProductionState(error = FetchError.NETWORK),
                daily = DailyProductionState(snapshot = DailySnapshot(mapOf("2026-07-15" to 4.0))),
            )

            runWidget {
                onNode(hasText(context.getString(R.string.home_today_status_network_error))).assertExists()
                onAllNodes(hasText(context.getString(R.string.home_today_value_kwh, 4.0))).assertCountEquals(2)
            }
        }

    @Test
    fun dailyError_replacesMonthAndLast30Figures() =
        runTest {
            seed(
                hourly = HourlyProductionState(snapshot = HourlySnapshot(mapOf(6 to 2.0))),
                daily = DailyProductionState(error = FetchError.AUTH),
            )

            runWidget {
                onNode(hasText(context.getString(R.string.home_today_value_kwh, 2.0))).assertExists()
                onAllNodes(hasText(context.getString(R.string.home_history_status_auth_error))).assertCountEquals(2)
            }
        }

    @Test
    fun themeRenders_forEachDisplayMode() =
        runTest {
            seed(
                hourly = HourlyProductionState(snapshot = HourlySnapshot(mapOf(6 to 1.0))),
                daily = DailyProductionState(snapshot = DailySnapshot(mapOf("2026-07-15" to 4.0))),
            )

            for (mode in listOf("system", "light", "dark")) {
                settings.setDisplayMode(mode)
                runWidget {
                    onNode(hasText(context.getString(R.string.home_today_value_kwh, 1.0))).assertExists()
                }
            }
        }

    private fun seed(
        hourly: HourlyProductionState,
        daily: DailyProductionState,
    ) {
        ProductionSummaryWidget.hourlySourceOverride = FakeHourlySource(hourly)
        ProductionSummaryWidget.dailySourceOverride = FakeDailySource(daily)
        ProductionSummaryWidget.todayOverride = { java.time.LocalDate.of(2026, 7, 15) }
    }

    private suspend fun runWidget(assertions: androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest.() -> Unit) {
        runGlanceAppWidgetUnitTest {
            setAppWidgetSize(DpSize(200.dp, 100.dp))
            setContext(context)
            provideComposable { ProductionSummaryWidget().TestContent() }
            assertions()
        }
    }

    private class FakeHourlySource(
        private val state: HourlyProductionState,
    ) : HourlyEnergySource {
        var currentStateCalls = 0

        override fun currentState(): HourlyProductionState {
            currentStateCalls++
            return state
        }

        override suspend fun refresh(force: Boolean): HourlyProductionState = state
    }

    private class FakeDailySource(
        private val state: DailyProductionState,
    ) : DailyEnergySource {
        var currentStateCalls = 0

        override fun currentState(): DailyProductionState {
            currentStateCalls++
            return state
        }

        override suspend fun refresh(force: Boolean): DailyProductionState = state
    }

    @Test
    fun showsDisabledMessage_whenWidgetDisabledInSettings() =
        runTest {
            val hourly = FakeHourlySource(HourlyProductionState(snapshot = HourlySnapshot(mapOf(6 to 1.0))))
            val daily = FakeDailySource(DailyProductionState())
            ProductionSummaryWidget.hourlySourceOverride = hourly
            ProductionSummaryWidget.dailySourceOverride = daily
            ProductionSummaryWidget.todayOverride = { java.time.LocalDate.of(2026, 7, 15) }
            settings.setWidgetEnabled(HomeWidget.PRODUCTION_SUMMARY, false)

            runWidget {
                onNode(hasText(context.getString(R.string.widget_disabled_in_settings))).assertExists()
            }
            assertEquals("disabled widget must not read its hourly data source", 0, hourly.currentStateCalls)
            assertEquals("disabled widget must not read its daily data source", 0, daily.currentStateCalls)
        }

    @Test
    fun rendersNormalContent_whenWidgetReEnabled() =
        runTest {
            seed(
                hourly = HourlyProductionState(snapshot = HourlySnapshot(mapOf(6 to 1.0))),
                daily = DailyProductionState(snapshot = DailySnapshot(mapOf("2026-07-15" to 1.0))),
            )
            settings.setWidgetEnabled(HomeWidget.PRODUCTION_SUMMARY, true)

            runWidget {
                onNode(hasText(context.getString(R.string.home_today_total_label))).assertExists()
            }
        }
}
