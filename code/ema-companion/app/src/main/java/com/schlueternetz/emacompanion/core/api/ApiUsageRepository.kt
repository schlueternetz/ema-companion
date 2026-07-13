package com.schlueternetz.emacompanion.core.api

import android.content.Context
import android.content.SharedPreferences
import java.time.YearMonth

/**
 * Persists the per-calendar-month successful EMA API request count, shared across every
 * repository so the API Request Limit progress bar reflects total usage. Its own plain
 * `SharedPreferences` file, separate from the encrypted settings store — carries no secrets,
 * so needs no encryption.
 */
class ApiUsageRepository(
    private val prefs: SharedPreferences,
    private val monthProvider: () -> String = { YearMonth.now().toString() },
) {
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
        val next =
            if (prefs.getString(KEY_COUNT_MONTH, null) == month) {
                prefs.getInt(KEY_COUNT, 0) + 1
            } else {
                1
            }
        prefs
            .edit()
            .putString(KEY_COUNT_MONTH, month)
            .putInt(KEY_COUNT, next)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        const val PREFS_NAME = "ema_api_usage"
        private const val KEY_COUNT_MONTH = "apiRequestCountMonth"
        private const val KEY_COUNT = "apiRequestCount"

        fun create(context: Context): ApiUsageRepository =
            ApiUsageRepository(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
    }
}
