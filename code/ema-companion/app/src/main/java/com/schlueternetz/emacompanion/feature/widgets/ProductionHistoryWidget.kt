package com.schlueternetz.emacompanion.feature.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.HomeWidget
import com.schlueternetz.emacompanion.core.api.ApiSyncScheduler
import com.schlueternetz.emacompanion.core.api.ApiSyncWorker
import com.schlueternetz.emacompanion.core.api.DailyEnergyRepository
import com.schlueternetz.emacompanion.core.api.DailyEnergySource
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import java.time.LocalDate

class ProductionHistoryWidget : GlanceAppWidget() {
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

        if (!settings.isWidgetEnabled(HomeWidget.PRODUCTION_HISTORY)) {
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

        val dailyState = (dailySourceOverride ?: DailyEnergyRepository.create(context)).currentState()
        val historyDays = settings.getHistoricDataDays()
        val today = todayOverride?.invoke() ?: LocalDate.now()

        WidgetTheme(settings.getDisplayMode()) {
            val configured = settings.isConfigured()
            val hasError = dailyState.error != null
            val target = WidgetTapTarget.target(configured, hasError)

            Column(
                modifier =
                    GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.background)
                        .clickable(WidgetTapTarget.action(context, target))
                        .padding(12.dp),
            ) {
                val daysInWindow =
                    dailyState.snapshot
                        ?.days
                        ?.entries
                        ?.mapNotNull { (dateStr, kwh) ->
                            val d = LocalDate.parse(dateStr)
                            val windowStart = today.minusDays(historyDays.toLong())
                            if (!d.isBefore(windowStart) && !d.isAfter(today)) d to kwh else null
                        }.orEmpty()

                when {
                    !configured -> Text(context.getString(R.string.widget_not_configured))
                    dailyState.error != null ->
                        Text(context.getString(widgetErrorStringRes(dailyState.error, WidgetDataSource.DAILY)))
                    daysInWindow.isEmpty() -> Text(context.getString(R.string.widget_no_data))
                    else -> {
                        Text(context.getString(R.string.widget_history_title, historyDays), style = WidgetTextStyles.title)
                        val size = LocalSize.current
                        val density = context.resources.displayMetrics.density
                        val widthPx = (size.width.value * density).toInt().coerceAtLeast(1)
                        val heightPx = (size.height.value * density).toInt().coerceAtLeast(1)
                        val bitmap =
                            WidgetChartRenderer.renderHistoryChart(
                                context = context,
                                daysInWindow = daysInWindow,
                                capacity = settings.getSystemCapacity(),
                                widthPx = widthPx,
                                heightPx = heightPx,
                            )
                        Image(
                            provider = ImageProvider(bitmap),
                            contentDescription = context.getString(R.string.widget_history_chart_description),
                            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
        }
    }

    companion object {
        /** Test seam: substitutes the daily energy source. */
        var dailySourceOverride: DailyEnergySource? = null

        /** Test seam: overrides "today" used for the history window. */
        var todayOverride: (() -> LocalDate)? = null
    }
}

class ProductionHistoryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ProductionHistoryWidget()

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
