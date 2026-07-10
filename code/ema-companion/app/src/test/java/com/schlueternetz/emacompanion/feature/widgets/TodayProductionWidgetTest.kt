package com.schlueternetz.emacompanion.feature.widgets

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasContentDescription
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.api.FetchError
import com.schlueternetz.emacompanion.core.api.HourlyEnergySource
import com.schlueternetz.emacompanion.core.api.HourlyProductionState
import com.schlueternetz.emacompanion.core.api.HourlySnapshot
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TodayProductionWidgetTest {
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
        TodayProductionWidget.currentHourOverride = 8
    }

    @After
    fun tearDown() {
        TodayProductionWidget.hourlySourceOverride = null
        TodayProductionWidget.currentHourOverride = null
    }

    @Test
    fun rendersChartAndTotal_whenDataCached() =
        runTest {
            TodayProductionWidget.hourlySourceOverride =
                FakeSource(
                    HourlyProductionState(snapshot = HourlySnapshot(mapOf(6 to 1.0, 7 to 2.0, 8 to 1.5))),
                )

            runGlanceAppWidgetUnitTest {
                setAppWidgetSize(DpSize(200.dp, 100.dp))
                setContext(context)
                provideComposable { TodayProductionWidget().TestContent() }

                onNode(hasContentDescription(context.getString(R.string.widget_today_chart_description)))
                    .assertExists()
                onNode(hasText(context.getString(R.string.home_today_value_kwh, 4.5))).assertExists()
            }
        }

    @Test
    fun showsNotConfiguredPlaceholder_whenAppNotConfigured() =
        runTest {
            settings.clearAll()
            TodayProductionWidget.hourlySourceOverride = FakeSource(HourlyProductionState())

            runGlanceAppWidgetUnitTest {
                setAppWidgetSize(DpSize(200.dp, 100.dp))
                setContext(context)
                provideComposable { TodayProductionWidget().TestContent() }

                onNode(hasText(context.getString(R.string.widget_not_configured))).assertExists()
            }
        }

    @Test
    fun showsNoDataPlaceholder_whenNoHoursCached() =
        runTest {
            TodayProductionWidget.hourlySourceOverride = FakeSource(HourlyProductionState())

            runGlanceAppWidgetUnitTest {
                setAppWidgetSize(DpSize(200.dp, 100.dp))
                setContext(context)
                provideComposable { TodayProductionWidget().TestContent() }

                onNode(hasText(context.getString(R.string.widget_no_data))).assertExists()
            }
        }

    @Test
    fun networkError_replacesChartWithErrorMessage() =
        runTest {
            TodayProductionWidget.hourlySourceOverride =
                FakeSource(
                    HourlyProductionState(
                        snapshot = HourlySnapshot(mapOf(6 to 1.0)),
                        error = FetchError.NETWORK,
                    ),
                )

            runGlanceAppWidgetUnitTest {
                setAppWidgetSize(DpSize(200.dp, 100.dp))
                setContext(context)
                provideComposable { TodayProductionWidget().TestContent() }

                onNode(hasText(context.getString(R.string.home_today_status_network_error))).assertExists()
            }
        }

    @Test
    fun authError_replacesChartWithErrorMessage() =
        runTest {
            TodayProductionWidget.hourlySourceOverride =
                FakeSource(
                    HourlyProductionState(
                        snapshot = HourlySnapshot(mapOf(6 to 1.0)),
                        error = FetchError.AUTH,
                    ),
                )

            runGlanceAppWidgetUnitTest {
                setAppWidgetSize(DpSize(200.dp, 100.dp))
                setContext(context)
                provideComposable { TodayProductionWidget().TestContent() }

                onNode(hasText(context.getString(R.string.home_today_status_auth_error))).assertExists()
            }
        }

    @Test
    fun apiError_replacesChartWithErrorMessage() =
        runTest {
            TodayProductionWidget.hourlySourceOverride =
                FakeSource(
                    HourlyProductionState(
                        snapshot = HourlySnapshot(mapOf(6 to 1.0)),
                        error = FetchError.API,
                    ),
                )

            runGlanceAppWidgetUnitTest {
                setAppWidgetSize(DpSize(200.dp, 100.dp))
                setContext(context)
                provideComposable { TodayProductionWidget().TestContent() }

                onNode(hasText(context.getString(R.string.home_today_status_api_error))).assertExists()
            }
        }

    @Test
    fun errorClears_chartReappearsOnNextSuccess() =
        runTest {
            TodayProductionWidget.hourlySourceOverride =
                FakeSource(
                    HourlyProductionState(snapshot = HourlySnapshot(mapOf(6 to 1.0, 7 to 2.0, 8 to 1.5)), error = null),
                )

            runGlanceAppWidgetUnitTest {
                setAppWidgetSize(DpSize(200.dp, 100.dp))
                setContext(context)
                provideComposable { TodayProductionWidget().TestContent() }

                onNode(hasContentDescription(context.getString(R.string.widget_today_chart_description)))
                    .assertExists()
            }
        }

    @Test
    fun capacityConfigured_chartRendersWithoutError() =
        runTest {
            settings.setSystemCapacity(7f)
            TodayProductionWidget.hourlySourceOverride =
                FakeSource(HourlyProductionState(snapshot = HourlySnapshot(mapOf(6 to 1.0, 7 to 2.0, 8 to 1.5))))

            runGlanceAppWidgetUnitTest {
                setAppWidgetSize(DpSize(200.dp, 100.dp))
                setContext(context)
                provideComposable { TodayProductionWidget().TestContent() }

                onNode(hasContentDescription(context.getString(R.string.widget_today_chart_description)))
                    .assertExists()
            }
        }

    @Test
    fun themeRenders_forEachDisplayMode() =
        runTest {
            TodayProductionWidget.hourlySourceOverride =
                FakeSource(HourlyProductionState(snapshot = HourlySnapshot(mapOf(6 to 1.0))))

            for (mode in listOf("system", "light", "dark")) {
                settings.setDisplayMode(mode)
                runGlanceAppWidgetUnitTest {
                    setAppWidgetSize(DpSize(200.dp, 100.dp))
                    setContext(context)
                    provideComposable { TodayProductionWidget().TestContent() }

                    onNode(hasContentDescription(context.getString(R.string.widget_today_chart_description)))
                        .assertExists()
                }
            }
        }

    private class FakeSource(
        private val state: HourlyProductionState,
    ) : HourlyEnergySource {
        override fun currentState(): HourlyProductionState = state

        override suspend fun refresh(force: Boolean): HourlyProductionState = state
    }
}
