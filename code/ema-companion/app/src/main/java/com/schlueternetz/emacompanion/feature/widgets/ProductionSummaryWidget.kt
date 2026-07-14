package com.schlueternetz.emacompanion.feature.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.HomeWidget
import com.schlueternetz.emacompanion.core.api.ApiSyncScheduler
import com.schlueternetz.emacompanion.core.api.ApiSyncWorker
import com.schlueternetz.emacompanion.core.api.DailyEnergyRepository
import com.schlueternetz.emacompanion.core.api.DailyEnergySource
import com.schlueternetz.emacompanion.core.api.HourlyEnergyRepository
import com.schlueternetz.emacompanion.core.api.HourlyEnergySource
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import java.time.LocalDate

class ProductionSummaryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: android.content.Context,
        id: androidx.glance.GlanceId,
    ) {
        provideContent { TestContent() }
    }

    @Composable
    internal fun TestContent() {
        val context = LocalContext.current
        val settings = SettingsRepository.create(context)

        if (!settings.isWidgetEnabled(HomeWidget.PRODUCTION_SUMMARY)) {
            WidgetTheme(settings.getDisplayMode()) {
                Column(
                    modifier =
                        GlanceModifier
                            .fillMaxSize()
                            .background(GlanceTheme.colors.background)
                            .padding(12.dp),
                ) {
                    Text(context.getString(R.string.widget_disabled_in_settings), style = WidgetTextStyles.value)
                }
            }
            return
        }

        val hourlyState = (hourlySourceOverride ?: HourlyEnergyRepository.create(context)).currentState()
        val dailyState = (dailySourceOverride ?: DailyEnergyRepository.create(context)).currentState()
        val today = todayOverride?.invoke() ?: LocalDate.now()

        WidgetTheme(settings.getDisplayMode()) {
            val configured = settings.isConfigured()
            val hasError = hourlyState.error != null || dailyState.error != null
            val target = WidgetTapTarget.target(configured, hasError)

            Column(
                modifier =
                    GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.background)
                        .clickable(WidgetTapTarget.action(context, target))
                        .padding(12.dp),
            ) {
                if (!configured) {
                    Text(context.getString(R.string.widget_not_configured), style = WidgetTextStyles.value)
                } else {
                    Text(context.getString(R.string.home_today_total_label), style = WidgetTextStyles.header)
                    if (hourlyState.error != null) {
                        Text(context.getString(widgetErrorStringRes(hourlyState.error, WidgetDataSource.HOURLY)))
                    } else {
                        Text(
                            context.getString(R.string.home_today_value_kwh, todaysTotalKwh(hourlyState.snapshot)),
                            style = WidgetTextStyles.value,
                        )
                    }
                    Spacer(GlanceModifier.height(8.dp))

                    val dailyErrorText =
                        dailyState.error?.let { context.getString(widgetErrorStringRes(it, WidgetDataSource.DAILY)) }

                    Text(context.getString(R.string.home_history_this_month_label), style = WidgetTextStyles.header)
                    if (dailyErrorText != null) {
                        Text(dailyErrorText)
                    } else {
                        Text(
                            context.getString(R.string.home_today_value_kwh, thisMonthTotalKwh(dailyState.snapshot, today)),
                            style = WidgetTextStyles.value,
                        )
                    }
                    Spacer(GlanceModifier.height(8.dp))

                    Text(context.getString(R.string.home_history_last_30_label), style = WidgetTextStyles.header)
                    if (dailyErrorText != null) {
                        Text(dailyErrorText)
                    } else {
                        Text(
                            context.getString(R.string.home_today_value_kwh, last30DaysTotalKwh(dailyState.snapshot, today)),
                            style = WidgetTextStyles.value,
                        )
                    }
                }
            }
        }
    }

    companion object {
        /** Test seam: substitutes the hourly energy source. */
        var hourlySourceOverride: HourlyEnergySource? = null

        /** Test seam: substitutes the daily energy source. */
        var dailySourceOverride: DailyEnergySource? = null

        /** Test seam: overrides "today" used for month/last-30-days windowing. */
        var todayOverride: (() -> LocalDate)? = null
    }
}

class ProductionSummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ProductionSummaryWidget()

    override fun onEnabled(context: android.content.Context) {
        super.onEnabled(context)
        ApiSyncScheduler.schedulePeriodic(context)
    }

    override fun onDisabled(context: android.content.Context) {
        super.onDisabled(context)
        val stillPlaced = kotlinx.coroutines.runBlocking { ApiSyncWorker.hasConsumingWidgetPlacedAction(context) }
        if (!stillPlaced) {
            ApiSyncScheduler.cancelPeriodic(context)
        }
    }
}
