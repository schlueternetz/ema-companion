package com.schlueternetz.emacompanion.core.api

import android.content.Context
import android.content.SharedPreferences
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import com.schlueternetz.emacompanion.feature.widgets.ProductionHistoryWidget
import com.schlueternetz.emacompanion.feature.widgets.ProductionSummaryWidget
import com.schlueternetz.emacompanion.feature.widgets.TodayProductionWidget
import com.schlueternetz.emacompanion.feature.widgets.WidgetUpdater
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Executes every EMA API sync request enqueued by [ApiSyncScheduler] — see ADR-010. Replaces the
 * old `WidgetRefreshWorker`; also handles Home's on-demand requests (opportunistic/forced) and
 * Settings' credential-change resync, so a fetch is no longer scoped to a Fragment's lifecycle.
 *
 * Module Health is deliberately absent here — it keeps its own always-on `ModuleHealthWorker`
 * schedule, never gated by any of the checks below (see ADR-010's "alerting is never gated"
 * invariant).
 */
class ApiSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val kind =
            inputData.getString(ApiSyncScheduler.KEY_REQUEST_KIND)?.let {
                runCatching { ApiSyncRequestKind.valueOf(it) }.getOrNull()
            } ?: ApiSyncRequestKind.PERIODIC

        val settings = SettingsRepository.create(applicationContext)
        val hourlySource = hourlySourceOverride ?: HourlyEnergyRepository.create(applicationContext)
        val dailySource = dailySourceOverride ?: DailyEnergyRepository.create(applicationContext)
        val widgets = widgetsOverride ?: WidgetUpdater.enabledWidgets(settings)

        if (kind == ApiSyncRequestKind.SETTINGS_CHANGED) {
            hourlySource.resetThrottle()
            dailySource.resetThrottle()
        }

        // Daily's live "today" value is derived from hourly data (see DailyEnergyRepository),
        // so hourly must keep polling for a daily-only consumer even with zero hourly consumers.
        val hourlyNeeded = settings.isHourlyDataNeeded() || settings.isDailyDataNeeded()
        val runHourly =
            when (kind) {
                ApiSyncRequestKind.PERIODIC -> hourlyNeeded && isPeriodicHourlyEligible(settings)
                else -> hourlyNeeded
            }
        if (runHourly) {
            val hourlyState = hourlySource.refresh(force = kind == ApiSyncRequestKind.FORCED)
            if (hourlyState.error == null) updateAllAction(applicationContext, widgets)
        }

        val dailyNeeded = settings.isDailyDataNeeded()
        val runDaily =
            when (kind) {
                ApiSyncRequestKind.PERIODIC -> dailyNeeded && isFirstDailyCheckToday()
                else -> dailyNeeded
            }
        if (runDaily) {
            val dailyState = dailySource.refresh(force = kind == ApiSyncRequestKind.FORCED)
            if (dailyState.error == null) updateAllAction(applicationContext, widgets)
            if (kind == ApiSyncRequestKind.PERIODIC) markDailyCheckedToday()
        }

        return Result.success()
    }

    /** The periodic hourly poll additionally requires the daylight window AND (placement OR app-foreground). */
    private suspend fun isPeriodicHourlyEligible(settings: SettingsRepository): Boolean {
        if (!isWithinDaylightWindow(settings)) return false
        val hasPlacedWidget = hasConsumingWidgetPlacedAction(applicationContext)
        val foregroundTracker = foregroundTrackerOverride ?: AppForegroundTracker.create(applicationContext)
        return hasPlacedWidget || foregroundTracker.isRecentlyForegrounded()
    }

    private fun isWithinDaylightWindow(settings: SettingsRepository): Boolean {
        val time = currentArrayLocalTimeOverride?.invoke() ?: LocalTime.now(arrayZone(settings))
        return !time.isBefore(DAYLIGHT_START) && time.isBefore(DAYLIGHT_END)
    }

    private fun arrayZone(settings: SettingsRepository): ZoneId =
        runCatching { ZoneId.of(settings.getArrayTimezone()) }.getOrDefault(ZoneId.systemDefault())

    private fun isFirstDailyCheckToday(): Boolean {
        val today = (currentLocalDateOverride?.invoke() ?: LocalDate.now()).toString()
        val prefs = dailyCheckPrefsOverride ?: applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_DAILY_CHECK_DATE, null) != today
    }

    private fun markDailyCheckedToday() {
        val today = (currentLocalDateOverride?.invoke() ?: LocalDate.now()).toString()
        val prefs = dailyCheckPrefsOverride ?: applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_DAILY_CHECK_DATE, today).apply()
    }

    companion object {
        private const val PREFS_NAME = "ema_api_sync_worker"
        private const val KEY_LAST_DAILY_CHECK_DATE = "lastDailyCheckDate"
        internal val DAYLIGHT_START: LocalTime = LocalTime.of(6, 0)
        internal val DAYLIGHT_END: LocalTime = LocalTime.of(22, 0)

        /** Test seam: substitutes the hourly energy source. */
        var hourlySourceOverride: HourlyEnergySyncSource? = null

        /** Test seam: substitutes the daily energy source. */
        var dailySourceOverride: DailyEnergySyncSource? = null

        /** Test seam: substitutes the widgets whose content is updated. */
        var widgetsOverride: List<GlanceAppWidget>? = null

        /** Test seam: overrides "now" (array-local time) for daylight-window gating. */
        var currentArrayLocalTimeOverride: (() -> LocalTime)? = null

        /** Test seam: overrides "today" (local date) for the once-per-day daily-check gate. */
        var currentLocalDateOverride: (() -> LocalDate)? = null

        /** Test seam: substitutes the prefs store backing the once-per-day daily-check gate. */
        var dailyCheckPrefsOverride: SharedPreferences? = null

        /** Test seam: substitutes [AppForegroundTracker]. */
        var foregroundTrackerOverride: AppForegroundTracker? = null

        internal val defaultHasConsumingWidgetPlacedAction: suspend (Context) -> Boolean = { context ->
            val manager = GlanceAppWidgetManager(context)
            manager.getGlanceIds(TodayProductionWidget::class.java).isNotEmpty() ||
                manager.getGlanceIds(ProductionSummaryWidget::class.java).isNotEmpty() ||
                manager.getGlanceIds(ProductionHistoryWidget::class.java).isNotEmpty()
        }

        /** Test seam: substitutes the real (Robolectric-incompatible) widget-placement check. */
        var hasConsumingWidgetPlacedAction: suspend (Context) -> Boolean = defaultHasConsumingWidgetPlacedAction

        internal val defaultUpdateAllAction: suspend (Context, List<GlanceAppWidget>) -> Unit =
            { context, widgets -> WidgetUpdater.updateAll(context, widgets) }

        /** Test seam: substitutes the widget-update side effect invoked after a successful fetch. */
        var updateAllAction: suspend (Context, List<GlanceAppWidget>) -> Unit = defaultUpdateAllAction
    }
}
