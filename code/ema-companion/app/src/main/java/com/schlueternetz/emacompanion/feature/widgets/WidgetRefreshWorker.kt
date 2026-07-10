package com.schlueternetz.emacompanion.feature.widgets

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.schlueternetz.emacompanion.core.api.DailyEnergyRepository
import com.schlueternetz.emacompanion.core.api.DailyEnergySource
import com.schlueternetz.emacompanion.core.api.HourlyEnergyRepository
import com.schlueternetz.emacompanion.core.api.HourlyEnergySource
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Keeps widget content current without the app being opened. Scheduled every 2 hours (the hourly
 * cadence); the daily branch self-gates on [DAILY_TRIGGER_HOUR] so it only actually fires once a
 * day even though this worker itself runs every 2 hours. Both branches reuse each repository's own
 * existing throttle — `force = false` — so the ADR-009 call budget is unaffected by this worker's
 * own trigger cadence, only by how often a throttle window has actually elapsed.
 */
class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val hourlySource = hourlySourceOverride ?: HourlyEnergyRepository.create(applicationContext)
        val dailySource = dailySourceOverride ?: DailyEnergyRepository.create(applicationContext)
        val widgets = widgetsOverride ?: listOf(TodayProductionWidget(), ProductionSummaryWidget(), ProductionHistoryWidget())

        val hourlyState = hourlySource.refresh(force = false)
        if (hourlyState.error == null) {
            updateAllAction(applicationContext, widgets)
        }

        val hour = (hourOverride ?: { LocalTime.now().hour })()
        if (hour == DAILY_TRIGGER_HOUR) {
            val dailyState = dailySource.refresh(force = false)
            if (dailyState.error == null) {
                updateAllAction(applicationContext, widgets)
            }
        }

        return Result.success()
    }

    companion object {
        /** Test seam: substitutes the hourly energy source. */
        var hourlySourceOverride: HourlyEnergySource? = null

        /** Test seam: substitutes the daily energy source. */
        var dailySourceOverride: DailyEnergySource? = null

        /** Test seam: substitutes the widgets whose content is updated. */
        var widgetsOverride: List<GlanceAppWidget>? = null

        /** Test seam: overrides the current local hour (0-23) used for the daily trigger gate. */
        var hourOverride: (() -> Int)? = null

        internal val defaultUpdateAllAction: suspend (Context, List<GlanceAppWidget>) -> Unit =
            { context, widgets -> WidgetUpdater.updateAll(context, widgets) }

        /** Test seam: substitutes the widget-update side effect. */
        var updateAllAction: suspend (Context, List<GlanceAppWidget>) -> Unit = defaultUpdateAllAction

        internal const val DAILY_TRIGGER_HOUR = 21

        private const val WORK_NAME = "widget_refresh"
        private val HOURLY_INTERVAL_MS = TimeUnit.HOURS.toMillis(2)

        /** Enqueues the 2-hour periodic refresh. Uses KEEP policy: does not disturb an already-enqueued request. */
        fun schedule(
            context: Context,
            policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP,
        ) {
            val request =
                PeriodicWorkRequestBuilder<WidgetRefreshWorker>(HOURLY_INTERVAL_MS, TimeUnit.MILLISECONDS)
                    .build()
            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, policy, request)
        }
    }
}
