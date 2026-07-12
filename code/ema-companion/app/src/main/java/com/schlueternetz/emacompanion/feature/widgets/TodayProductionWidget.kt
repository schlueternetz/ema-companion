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
import com.schlueternetz.emacompanion.core.api.HourlyEnergyRepository
import com.schlueternetz.emacompanion.core.api.HourlyEnergySource
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import java.time.LocalTime

class TodayProductionWidget : GlanceAppWidget() {
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
        val hourlySource = hourlySourceOverride ?: HourlyEnergyRepository.create(context)
        val state = hourlySource.currentState()
        val currentHour = currentHourOverride ?: LocalTime.now().hour

        WidgetTheme(settings.getDisplayMode()) {
            val configured = settings.isConfigured()
            val hasError = state.error != null
            val target = WidgetTapTarget.target(configured, hasError)

            Column(
                modifier =
                    GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.background)
                        .clickable(WidgetTapTarget.action(context, target))
                        .padding(12.dp),
            ) {
                when {
                    !configured -> Text(context.getString(R.string.widget_not_configured), style = WidgetTextStyles.value)
                    state.error != null ->
                        Text(context.getString(widgetErrorStringRes(state.error, WidgetDataSource.HOURLY)))
                    state.snapshot == null || state.snapshot.hours.isEmpty() ->
                        Text(context.getString(R.string.widget_no_data))
                    else -> {
                        val size = LocalSize.current
                        val density = context.resources.displayMetrics.density
                        val widthPx = (size.width.value * density).toInt().coerceAtLeast(1)
                        val heightPx = (size.height.value * density).toInt().coerceAtLeast(1)
                        val bitmap =
                            WidgetChartRenderer.renderHourlyChart(
                                context = context,
                                snapshot = state.snapshot,
                                capacity = settings.getSystemCapacity(),
                                currentHour = currentHour,
                                widthPx = widthPx,
                                heightPx = heightPx,
                            )
                        Text(context.getString(R.string.home_today_title), style = WidgetTextStyles.title)
                        Image(
                            provider = ImageProvider(bitmap),
                            contentDescription = context.getString(R.string.widget_today_chart_description),
                            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                            contentScale = ContentScale.Fit,
                        )
                        Text(
                            context.getString(R.string.home_today_value_kwh, todaysTotalKwh(state.snapshot)),
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

        /** Test seam: overrides the current hour used for chart rendering (0-23). */
        var currentHourOverride: Int? = null
    }
}

class TodayProductionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayProductionWidget()
}
