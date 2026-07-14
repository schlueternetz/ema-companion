package com.schlueternetz.emacompanion.core.api

import android.content.Context
import android.content.SharedPreferences
import com.schlueternetz.emacompanion.core.api.log.ApiCallLog
import com.schlueternetz.emacompanion.core.api.log.ApiCallLogRepository
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import org.json.JSONObject
import java.time.LocalDate

/** State for the today-section tile: hourly kWh breakdown, updated timestamp, and last fetch error. */
data class HourlyProductionState(
    val snapshot: HourlySnapshot? = null,
    val updatedAtEpochMs: Long? = null,
    val error: FetchError? = null,
)

/**
 * Caches today's 24 hourly kWh values in [PREFS_NAME]. Throttle: one fetch per hour (3,600 s);
 * only a successful fetch starts the throttle. A forced refresh (pull-to-refresh) bypasses the
 * throttle but does not touch the throttle timestamp on failure.
 *
 * Counting toward the monthly API budget is delegated to [ApiUsageRepository.recordRequest].
 * The per-hour throttle timestamp lives in this repo's own prefs.
 */
class HourlyEnergyRepository(
    private val client: EmaApiClient,
    private val usageCounter: ApiUsageRepository,
    private val log: ApiCallLogRepository,
    private val appSecretProvider: () -> String,
    private val today: () -> String = { LocalDate.now().toString() },
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val prefs: SharedPreferences,
) : HourlyEnergySyncSource {
    private var cached: HourlySnapshot? = loadSnapshot()
    private var cachedAtEpochMs: Long? = prefs.getLong(KEY_UPDATED_AT, 0L).takeIf { it > 0 }
    private var error: FetchError? =
        prefs
            .getString(KEY_LAST_ERROR, null)
            ?.let { runCatching { FetchError.valueOf(it) }.getOrNull() }

    override fun currentState() = HourlyProductionState(snapshot = cached, updatedAtEpochMs = cachedAtEpochMs, error = error)

    override suspend fun refresh(force: Boolean): HourlyProductionState {
        val now = clock()
        val lastFetch = prefs.getLong(KEY_LAST_FETCH, 0L)
        if (!force && now - lastFetch < THROTTLE_MS) {
            return currentState()
        }

        val fetch = client.getHourlyEnergy(today())
        when (val result = fetch.result) {
            is ApiResult.ConfigurationError -> Unit
            is ApiResult.Success -> {
                logCall(now, fetch)
                prefs.edit().putLong(KEY_LAST_FETCH, now).apply()
                usageCounter.recordRequest()
                cached = result.data
                cachedAtEpochMs = now
                saveSnapshot(result.data, now)
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
        cached = null
        cachedAtEpochMs = null
        error = null
    }

    private fun logCall(
        now: Long,
        fetch: HourlyEnergyFetch,
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

    private fun saveSnapshot(
        snapshot: HourlySnapshot,
        updatedAt: Long,
    ) {
        val json = JSONObject()
        snapshot.hours.forEach { (hour, kwh) -> json.put(hour.toString(), kwh) }
        prefs
            .edit()
            .putString(KEY_HOURS, json.toString())
            .putLong(KEY_UPDATED_AT, updatedAt)
            .apply()
    }

    private fun loadSnapshot(): HourlySnapshot? {
        val raw = prefs.getString(KEY_HOURS, null) ?: return null
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        val hours = mutableMapOf<Int, Double>()
        json.keys().forEach { key ->
            val h = key.toIntOrNull() ?: return@forEach
            hours[h] = json.getDouble(key)
        }
        return HourlySnapshot(hours)
    }

    companion object {
        const val PREFS_NAME = "ema_hourly"
        private const val KEY_HOURS = "hours"
        private const val KEY_UPDATED_AT = "updatedAtMs"
        private const val KEY_LAST_ERROR = "lastError"
        private const val KEY_LAST_FETCH = "lastFetchMs"
        private const val THROTTLE_MS = 2_700_000L
        private val AUTH_CODES = setOf(2000, 2001, 2002, 2003, 2004, 3000, 3001, 3002, 3003, 3004)

        fun create(context: Context): HourlyEnergyRepository {
            val settings = SettingsRepository.create(context)
            return HourlyEnergyRepository(
                client = OkHttpEmaApiClient(settings),
                usageCounter = ApiUsageRepository.create(context),
                log = ApiCallLogRepository.create(context),
                appSecretProvider = { settings.getEmaAppSecret() },
                prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
            )
        }
    }
}
