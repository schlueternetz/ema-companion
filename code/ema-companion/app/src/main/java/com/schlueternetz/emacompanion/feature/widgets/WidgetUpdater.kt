package com.schlueternetz.emacompanion.feature.widgets

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll
import com.schlueternetz.emacompanion.core.HomeWidget
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository

/** Thin wrapper so callers (Home, the background worker, Settings) update every placed widget in one call. */
object WidgetUpdater {
    suspend fun updateAll(
        context: Context,
        widgets: List<GlanceAppWidget>,
    ) {
        widgets.forEach { it.updateAll(context) }
    }

    /** Convenience overload updating only the shipped widgets currently enabled in Settings. */
    suspend fun updateAll(context: Context) {
        updateAll(context, enabledWidgets(SettingsRepository.create(context)))
    }

    /** The three shipped widgets, filtered to those enabled in [settings]. */
    internal fun enabledWidgets(settings: SettingsRepository): List<GlanceAppWidget> =
        listOfNotNull(
            TodayProductionWidget().takeIf { settings.isWidgetEnabled(HomeWidget.TODAY_PRODUCTION) },
            ProductionSummaryWidget().takeIf { settings.isWidgetEnabled(HomeWidget.PRODUCTION_SUMMARY) },
            ProductionHistoryWidget().takeIf { settings.isWidgetEnabled(HomeWidget.PRODUCTION_HISTORY) },
        )
}
