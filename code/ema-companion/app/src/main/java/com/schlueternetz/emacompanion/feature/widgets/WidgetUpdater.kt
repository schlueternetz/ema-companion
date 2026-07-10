package com.schlueternetz.emacompanion.feature.widgets

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll

/** Thin wrapper so callers (Home, the background worker, Settings) update every placed widget in one call. */
object WidgetUpdater {
    suspend fun updateAll(
        context: Context,
        widgets: List<GlanceAppWidget>,
    ) {
        widgets.forEach { it.updateAll(context) }
    }

    /** Convenience overload updating all three shipped widgets. */
    suspend fun updateAll(context: Context) {
        updateAll(context, listOf(TodayProductionWidget(), ProductionSummaryWidget(), ProductionHistoryWidget()))
    }
}
