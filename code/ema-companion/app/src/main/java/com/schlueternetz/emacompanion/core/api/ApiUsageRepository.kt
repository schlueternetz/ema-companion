package com.schlueternetz.emacompanion.core.api

import android.content.Context
import android.content.SharedPreferences
import java.time.YearMonth

/**
 * Persists EMA API usage state — the per-calendar-month request count and the timestamp of
 * the last fetch attempt (for throttling) — in its own plain `SharedPreferences` file,
 * separate from the encrypted settings store. Carries no secrets, so needs no encryption.
 */
class ApiUsageRepository(
    private val prefs: SharedPreferences,
    private val monthProvider: () -> String = { YearMonth.now().toString() },
) : ThrottleResettable {

    /** The request count for the current calendar month (0 if the stored month has rolled over). */
    fun getRequestCount(): Int =
        if (prefs.getString(KEY_COUNT_MONTH, null) == monthProvider()) {
            prefs.getInt(KEY_COUNT, 0)
        } else {
            0
        }

    /** Counts one issued request, resetting to 1 when the calendar month has changed. */
    fun recordRequest() {
        val month = monthProvider()
        val next = if (prefs.getString(KEY_COUNT_MONTH, null) == month) {
            prefs.getInt(KEY_COUNT, 0) + 1
        } else {
            1
        }
        prefs.edit().putString(KEY_COUNT_MONTH, month).putInt(KEY_COUNT, next).apply()
    }

    fun getLastFetchEpochMs(): Long = prefs.getLong(KEY_LAST_FETCH, 0L)

    fun setLastFetchEpochMs(value: Long) {
        prefs.edit().putLong(KEY_LAST_FETCH, value).apply()
    }

    /** The last successfully fetched production power in watts, or -1 if none yet. */
    fun getLastProductionWatts(): Int = prefs.getInt(KEY_LAST_PRODUCTION, -1)

    /** When the last successful production value was fetched (epoch ms), or 0 if none yet. */
    fun getLastProductionEpochMs(): Long = prefs.getLong(KEY_LAST_PRODUCTION_AT, 0L)

    fun setLastProduction(watts: Int, epochMs: Long) {
        prefs.edit().putInt(KEY_LAST_PRODUCTION, watts).putLong(KEY_LAST_PRODUCTION_AT, epochMs).apply()
    }

    /** The error of the last fetch (for the Home banner), or null if the last fetch succeeded / none yet. */
    fun getLastError(): FetchError? =
        prefs.getString(KEY_LAST_ERROR, null)?.let { runCatching { FetchError.valueOf(it) }.getOrNull() }

    fun setLastError(error: FetchError?) {
        prefs.edit().apply {
            if (error == null) remove(KEY_LAST_ERROR) else putString(KEY_LAST_ERROR, error.name)
        }.apply()
    }

    override fun resetThrottle() {
        prefs.edit().putLong(KEY_LAST_FETCH, 0L).remove(KEY_LAST_ERROR).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        const val PREFS_NAME = "ema_api_usage"
        private const val KEY_COUNT_MONTH = "apiRequestCountMonth"
        private const val KEY_COUNT = "apiRequestCount"
        private const val KEY_LAST_FETCH = "lastFetchEpochMs"
        private const val KEY_LAST_PRODUCTION = "lastProductionWatts"
        private const val KEY_LAST_PRODUCTION_AT = "lastProductionEpochMs"
        private const val KEY_LAST_ERROR = "lastFetchError"

        fun create(context: Context): ApiUsageRepository =
            ApiUsageRepository(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
    }
}
