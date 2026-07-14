package com.schlueternetz.emacompanion.core.api

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import java.util.concurrent.TimeUnit

/**
 * The single entry point every frontend (Home, Settings, widgets) uses to request an EMA API
 * sync — see ADR-010. No Fragment, Activity, or Worker calls a tile repository's `refresh()`
 * directly; they call one of the `request*Sync` methods here instead.
 *
 * Each request kind is enqueued under its own unique work name via
 * `WorkManager.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, ...)`, so repeated requests of
 * the *same* kind coalesce into a single execution (a not-yet-started duplicate is replaced
 * outright; an in-flight one is cancelled via ordinary coroutine cancellation) while different
 * kinds don't cancel each other out of turn.
 */
object ApiSyncScheduler {
    internal const val KEY_REQUEST_KIND = "request_kind"

    internal const val WORK_NAME_OPPORTUNISTIC = "api_sync_opportunistic"
    internal const val WORK_NAME_FORCED = "api_sync_forced"
    internal const val WORK_NAME_SETTINGS_CHANGED = "api_sync_settings_changed"
    internal const val WORK_NAME_PERIODIC = "api_sync_periodic"
    private val PERIODIC_INTERVAL_MS = TimeUnit.MINUTES.toMillis(45)

    fun requestOpportunisticSync(context: Context) {
        enqueue(context, WORK_NAME_OPPORTUNISTIC, ApiSyncRequestKind.OPPORTUNISTIC)
        // Safety net: re-arms the periodic background poll if a widget-placement callback was
        // ever missed (KEEP policy — a no-op if it's already running; see design.md's risk list).
        schedulePeriodic(context)
    }

    fun requestForcedSync(context: Context) = enqueue(context, WORK_NAME_FORCED, ApiSyncRequestKind.FORCED)

    /** Test seam: substitutes the module-health throttle-reset target. */
    internal var moduleHealthResettableFactory: (Context) -> ThrottleResettable = { ctx -> ModuleHealthRepository.create(ctx) }

    /**
     * Also resets Module Health's throttle (synchronously — it's a cheap prefs write, unlike
     * hourly/daily's throttle reset which goes through the coalesced worker) so the *next*
     * scheduled check runs against fresh credentials. Does NOT force an immediate module-health
     * check — that stays on `ModuleHealthWorker`'s own daily schedule (ADR-010).
     */
    fun requestResyncAfterSettingsChange(context: Context) {
        moduleHealthResettableFactory(context).resetThrottle()
        enqueue(context, WORK_NAME_SETTINGS_CHANGED, ApiSyncRequestKind.SETTINGS_CHANGED)
    }

    /**
     * Emits whenever a requesting screen's most recent opportunistic or forced sync completes
     * successfully, so it can re-render from the repositories' updated `currentState()`.
     */
    fun observeCompletion(context: Context): Flow<Unit> {
        val workManager = WorkManager.getInstance(context)
        val opportunistic = workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME_OPPORTUNISTIC)
        val forced = workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME_FORCED)
        return merge(opportunistic, forced)
            .filter { infos -> infos.any { it.state == WorkInfo.State.SUCCEEDED } }
            .map { }
    }

    /**
     * (Re-)arms the unattended periodic background poll (`ApiSyncRequestKind.PERIODIC`), at
     * [PERIODIC_INTERVAL_MS]. Called both from widget placement hooks (`onEnabled()`) and as a
     * safety-net re-check from [requestOpportunisticSync] — [ExistingPeriodicWorkPolicy.KEEP] by
     * default so an already-running schedule is never disturbed.
     */
    fun schedulePeriodic(
        context: Context,
        policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP,
    ) {
        val data = Data.Builder().putString(KEY_REQUEST_KIND, ApiSyncRequestKind.PERIODIC.name).build()
        val request =
            PeriodicWorkRequestBuilder<ApiSyncWorker>(PERIODIC_INTERVAL_MS, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME_PERIODIC, policy, request)
    }

    /** Stops the periodic background poll — called once no placed widget of any type remains. */
    fun cancelPeriodic(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC)
    }

    private fun enqueue(
        context: Context,
        workName: String,
        kind: ApiSyncRequestKind,
    ) {
        val data = Data.Builder().putString(KEY_REQUEST_KIND, kind.name).build()
        val request =
            OneTimeWorkRequestBuilder<ApiSyncWorker>()
                .setInputData(data)
                .build()
        WorkManager.getInstance(context).enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, request)
    }
}
