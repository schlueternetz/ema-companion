package com.schlueternetz.emacompanion.core.api

import android.content.Context
import android.content.SharedPreferences
import com.schlueternetz.emacompanion.core.api.log.ApiCallLog
import com.schlueternetz.emacompanion.core.api.log.ApiCallLogRepository
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import java.time.LocalDate

/** State for the history-section tile: per-day kWh totals, updated timestamp, and last fetch error. */
data class DailyProductionState(
    val snapshot: DailySnapshot? = null,
    val updatedAtEpochMs: Long? = null,
    val error: FetchError? = null,
)

/**
 * Caches daily kWh totals in [PREFS_NAME]. Caching rules:
 * - Past days are immutable — once persisted they are never re-fetched.
 * - The current calendar day is re-fetched on each trigger, subject to a 1-hour throttle.
 * - On first fetch (no past-day cache), one full-window call is issued.
 * - On subsequent fetches, only today is fetched.
 * - [resetThrottle] clears only the today-throttle; the per-day cache is untouched.
 * - [clear] removes all cached data (factory reset).
 */
class DailyEnergyRepository(
    private val client: EmaApiClient,
    private val usageCounter: ApiUsageRepository,
    private val log: ApiCallLogRepository,
    private val appSecretProvider: () -> String,
    private val today: () -> String = { LocalDate.now().toString() },
    private val historyDays: () -> Int = { 45 },
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val prefs: SharedPreferences,
) : DailyEnergySource,
    ThrottleResettable {
    private var cachedDays: MutableMap<String, Double> = loadDays()
    private var cachedAtEpochMs: Long? = prefs.getLong(KEY_UPDATED_AT, 0L).takeIf { it > 0 }
    private var error: FetchError? =
        prefs
            .getString(KEY_LAST_ERROR, null)
            ?.let { runCatching { FetchError.valueOf(it) }.getOrNull() }

    override fun currentState() =
        DailyProductionState(
            snapshot = if (cachedDays.isEmpty()) null else DailySnapshot(cachedDays.toMap()),
            updatedAtEpochMs = cachedAtEpochMs,
            error = error,
        )

    override suspend fun refresh(force: Boolean): DailyProductionState {
        val now = clock()
        val lastFetch = prefs.getLong(KEY_LAST_FETCH, 0L)
        if (!force && now - lastFetch < THROTTLE_MS) {
            return currentState()
        }

        val todayStr = today()
        val windowStart = LocalDate.parse(todayStr).minusDays(historyDays().toLong()).toString()

        // Determine missing past days in the window (excluding today).
        val startDate = LocalDate.parse(windowStart)
        val todayDate = LocalDate.parse(todayStr)
        val hasMissingPastDays =
            generateSequence(startDate) { d ->
                val next = d.plusDays(1)
                if (next.isBefore(todayDate)) next else null
            }.any { !cachedDays.containsKey(it.toString()) }

        val fetchStart = if (hasMissingPastDays) windowStart else todayStr
        val fetchEnd = todayStr

        val fetch = client.getDailyEnergy(fetchStart, fetchEnd)
        when (val result = fetch.result) {
            is ApiResult.ConfigurationError -> Unit
            is ApiResult.Success -> {
                logCall(now, fetch)
                prefs.edit().putLong(KEY_LAST_FETCH, now).apply()
                usageCounter.recordRequest()
                cachedDays.putAll(result.data.days)
                cachedAtEpochMs = now
                saveDays(cachedDays, now)
                setError(null)
            }
            is ApiResult.NetworkError -> {
                logCall(now, fetch)
                setError(FetchError.NETWORK)
            }
            is ApiResult.ApiError -> {
                logCall(now, fetch)
                setError(classify(result))
            }
        }
        return currentState()
    }

    override fun resetThrottle() {
        prefs
            .edit()
            .putLong(KEY_LAST_FETCH, 0L)
            .remove(KEY_LAST_ERROR)
            .apply()
        error = null
    }

    fun clear() {
        prefs.edit().clear().apply()
        cachedDays = mutableMapOf()
        cachedAtEpochMs = null
        error = null
    }

    private fun logCall(
        now: Long,
        fetch: DailyEnergyFetch,
    ) {
        log.append(
            ApiCallLog(
                timestampMs = now,
                endpoint = fetch.endpoint,
                durationMs = fetch.durationMs,
                success = fetch.result is ApiResult.Success,
                requestText = fetch.requestText,
                responseText = fetch.responseText,
            ),
            secret = appSecretProvider(),
        )
    }

    private fun classify(error: ApiResult.ApiError): FetchError =
        if (error.code in AUTH_CODES || error.httpStatus == 401 || error.httpStatus == 403) {
            FetchError.AUTH
        } else {
            FetchError.API
        }

    private fun setError(value: FetchError?) {
        error = value
        prefs
            .edit()
            .apply {
                if (value == null) remove(KEY_LAST_ERROR) else putString(KEY_LAST_ERROR, value.name)
            }.apply()
    }

    private fun saveDays(
        days: Map<String, Double>,
        updatedAt: Long,
    ) {
        val edit = prefs.edit().putLong(KEY_UPDATED_AT, updatedAt)
        days.forEach { (date, kwh) -> edit.putString("day_$date", kwh.toString()) }
        edit.apply()
    }

    private fun loadDays(): MutableMap<String, Double> {
        val all = prefs.all
        val result = mutableMapOf<String, Double>()
        all.forEach { (key, value) ->
            if (key.startsWith("day_")) {
                val date = key.removePrefix("day_")
                val kwh = value.toString().toDoubleOrNull() ?: return@forEach
                result[date] = kwh
            }
        }
        return result
    }

    companion object {
        const val PREFS_NAME = "ema_daily"
        private const val KEY_UPDATED_AT = "updatedAtMs"
        private const val KEY_LAST_ERROR = "lastError"
        private const val KEY_LAST_FETCH = "lastFetchMs"
        private const val THROTTLE_MS = 3_600_000L
        private val AUTH_CODES = setOf(2000, 2001, 2002, 2003, 2004, 3000, 3001, 3002, 3003, 3004)

        fun create(context: Context): DailyEnergyRepository {
            val settings = SettingsRepository.create(context)
            return DailyEnergyRepository(
                client = OkHttpEmaApiClient(settings),
                usageCounter = ApiUsageRepository.create(context),
                log = ApiCallLogRepository.create(context),
                appSecretProvider = { settings.getEmaAppSecret() },
                historyDays = { settings.getHistoricDataDays() },
                prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
            )
        }
    }
}
