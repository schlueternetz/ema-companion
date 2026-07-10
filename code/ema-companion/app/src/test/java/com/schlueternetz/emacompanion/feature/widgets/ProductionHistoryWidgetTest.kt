package com.schlueternetz.emacompanion.feature.widgets

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasContentDescription
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.api.DailyEnergySource
import com.schlueternetz.emacompanion.core.api.DailyProductionState
import com.schlueternetz.emacompanion.core.api.DailySnapshot
import com.schlueternetz.emacompanion.core.api.FetchError
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProductionHistoryWidgetTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var settings: SettingsRepository
    private val today = LocalDate.of(2026, 7, 15)

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
        ProductionHistoryWidget.todayOverride = { today }
    }

    @After
    fun tearDown() {
        ProductionHistoryWidget.dailySourceOverride = null
        ProductionHistoryWidget.todayOverride = null
    }

    @Test
    fun multiDayData_rendersChartAndDefaultTitle() =
        runTest {
            seed(DailyProductionState(snapshot = DailySnapshot(mapOf("2026-07-14" to 3.0, "2026-07-15" to 4.0))))

            runWidget {
                onNode(hasContentDescription(context.getString(R.string.widget_history_chart_description))).assertExists()
                onNode(hasText(context.getString(R.string.widget_history_title, settings.getHistoricDataDays()))).assertExists()
            }
        }

    @Test
    fun singleDayData_rendersSingleBarChart() =
        runTest {
            seed(DailyProductionState(snapshot = DailySnapshot(mapOf("2026-07-15" to 4.0))))

            runWidget {
                onNode(hasContentDescription(context.getString(R.string.widget_history_chart_description))).assertExists()
            }
        }

    @Test
    fun noData_showsPlaceholder() =
        runTest {
            seed(DailyProductionState())

            runWidget {
                onNode(hasText(context.getString(R.string.widget_no_data))).assertExists()
            }
        }

    @Test
    fun notConfigured_showsPlaceholder() =
        runTest {
            settings.clearAll()
            seed(DailyProductionState(snapshot = DailySnapshot(mapOf("2026-07-15" to 4.0))))

            runWidget {
                onNode(hasText(context.getString(R.string.widget_not_configured))).assertExists()
            }
        }

    @Test
    fun titleFollowsConfiguredHistoryWindow() =
        runTest {
            settings.setHistoricDataDays(10)
            seed(DailyProductionState(snapshot = DailySnapshot(mapOf("2026-07-15" to 4.0))))

            runWidget {
                onNode(hasText(context.getString(R.string.widget_history_title, 10))).assertExists()
            }
        }

    @Test
    fun networkError_replacesChartWithErrorMessage() =
        runTest {
            seed(DailyProductionState(error = FetchError.NETWORK))

            runWidget {
                onNode(hasText(context.getString(R.string.home_history_status_network_error))).assertExists()
            }
        }

    @Test
    fun authError_replacesChartWithErrorMessage() =
        runTest {
            seed(DailyProductionState(error = FetchError.AUTH))

            runWidget {
                onNode(hasText(context.getString(R.string.home_history_status_auth_error))).assertExists()
            }
        }

    @Test
    fun apiError_replacesChartWithErrorMessage() =
        runTest {
            seed(DailyProductionState(error = FetchError.API))

            runWidget {
                onNode(hasText(context.getString(R.string.home_history_status_api_error))).assertExists()
            }
        }

    @Test
    fun errorClears_chartReappearsOnNextSuccess() =
        runTest {
            seed(DailyProductionState(snapshot = DailySnapshot(mapOf("2026-07-15" to 4.0)), error = null))

            runWidget {
                onNode(hasContentDescription(context.getString(R.string.widget_history_chart_description))).assertExists()
            }
        }

    @Test
    fun capacityConfigured_chartRendersWithoutError() =
        runTest {
            settings.setSystemCapacity(9f)
            seed(DailyProductionState(snapshot = DailySnapshot(mapOf("2026-07-15" to 4.0))))

            runWidget {
                onNode(hasContentDescription(context.getString(R.string.widget_history_chart_description))).assertExists()
            }
        }

    @Test
    fun themeRenders_forEachDisplayMode() =
        runTest {
            seed(DailyProductionState(snapshot = DailySnapshot(mapOf("2026-07-15" to 4.0))))

            for (mode in listOf("system", "light", "dark")) {
                settings.setDisplayMode(mode)
                runWidget {
                    onNode(hasContentDescription(context.getString(R.string.widget_history_chart_description))).assertExists()
                }
            }
        }

    private fun seed(state: DailyProductionState) {
        ProductionHistoryWidget.dailySourceOverride = FakeSource(state)
    }

    private suspend fun runWidget(assertions: GlanceAppWidgetUnitTest.() -> Unit) {
        runGlanceAppWidgetUnitTest {
            setAppWidgetSize(DpSize(300.dp, 150.dp))
            setContext(context)
            provideComposable { ProductionHistoryWidget().TestContent() }
            assertions()
        }
    }

    private class FakeSource(
        private val state: DailyProductionState,
    ) : DailyEnergySource {
        override fun currentState(): DailyProductionState = state

        override suspend fun refresh(force: Boolean): DailyProductionState = state
    }
}
