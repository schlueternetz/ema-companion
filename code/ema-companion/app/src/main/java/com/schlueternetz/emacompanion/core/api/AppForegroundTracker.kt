package com.schlueternetz.emacompanion.core.api

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Persists whether the app was recently in the foreground, so [ApiSyncWorker]'s periodic poll
 * can apply its "or app foreground" gating condition even when it runs in a freshly-restarted
 * process with no Activity ever having started in that process instance.
 */
class AppForegroundTracker(
    private val prefs: SharedPreferences,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
        prefs.edit().putBoolean(KEY_IS_STARTED, true).putLong(KEY_LAST_TRANSITION, clock()).apply()
    }

    override fun onStop(owner: LifecycleOwner) {
        prefs.edit().putBoolean(KEY_IS_STARTED, false).putLong(KEY_LAST_TRANSITION, clock()).apply()
    }

    /** True while currently foregrounded, or within [GRACE_WINDOW_MS] of having last left the foreground. */
    fun isRecentlyForegrounded(): Boolean {
        if (prefs.getBoolean(KEY_IS_STARTED, false)) return true
        val lastTransition = prefs.getLong(KEY_LAST_TRANSITION, 0L)
        return clock() - lastTransition < GRACE_WINDOW_MS
    }

    companion object {
        private const val PREFS_NAME = "ema_app_foreground"
        private const val KEY_IS_STARTED = "isStarted"
        private const val KEY_LAST_TRANSITION = "lastTransitionEpochMs"
        const val GRACE_WINDOW_MS = 5 * 60 * 1000L

        fun create(context: Context): AppForegroundTracker =
            AppForegroundTracker(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
    }
}
