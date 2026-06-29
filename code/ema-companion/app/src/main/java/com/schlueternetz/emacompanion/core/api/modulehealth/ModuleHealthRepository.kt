package com.schlueternetz.emacompanion.core.api.modulehealth

import android.content.Context
import android.content.SharedPreferences
import com.schlueternetz.emacompanion.core.AppConfig
import com.schlueternetz.emacompanion.core.api.ApiResult
import com.schlueternetz.emacompanion.core.api.EmaApiClient
import com.schlueternetz.emacompanion.core.api.FetchError
import com.schlueternetz.emacompanion.core.api.OkHttpEmaApiClient
import com.schlueternetz.emacompanion.core.api.ThrottleResettable
import com.schlueternetz.emacompanion.core.api.log.ApiCallLog
import com.schlueternetz.emacompanion.core.api.log.ApiCallLogRepository
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate

/**
 * Fetches and persists module health status derived from daily batch inverter energy data.
 * Status is computed from a 3-day window; past days are cached to avoid redundant API calls.
 */
class ModuleHealthRepository(
    private val client: EmaApiClient,
    private val log: ApiCallLogRepository,
    private val healthPrefs: SharedPreferences,
    private val dailyPrefs: SharedPreferences,
    private val appSecretProvider: () -> String,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val today: () -> LocalDate = { LocalDate.now() },
) : ModuleHealthSource,
    ThrottleResettable {
    override fun currentState(): ModuleHealthState {
        val statusName = healthPrefs.getString(KEY_STATUS, null)
        val status =
            statusName
                ?.let { runCatching { ModuleHealthStatus.valueOf(it) }.getOrNull() }
                ?: ModuleHealthStatus.UNKNOWN
        val offlineModules = parseOfflineModules(healthPrefs.getString(KEY_OFFLINE_MODULES, null))
        val checkedAt = healthPrefs.getLong(KEY_LAST_CHECK, -1L).takeIf { it >= 0 }
        val error =
            healthPrefs
                .getString(KEY_FETCH_ERROR, null)
                ?.let { runCatching { FetchError.valueOf(it) }.getOrNull() }
        return ModuleHealthState(status, offlineModules, checkedAt, error)
    }

    override suspend fun refresh(): ModuleHealthState {
        val now = clock()
        val lastCheck = healthPrefs.getLong(KEY_LAST_CHECK, -1L)
        if (lastCheck >= 0 && now - lastCheck < AppConfig.MODULE_HEALTH_CHECK_INTERVAL_MS) {
            return currentState()
        }

        val todayDate = today()
        val dates = (WINDOW_DAYS - 1 downTo 0).map { todayDate.minusDays(it.toLong()) }

        val window = mutableMapOf<LocalDate, Map<String, Double>>()
        for (date in dates) {
            val cached =
                if (date == todayDate) {
                    null // today is always re-fetched
                } else {
                    dailyPrefs.getString("$KEY_PREFIX$date", null)?.let { parseDailyJson(it) }
                }
            if (cached != null) {
                window[date] = cached
                continue
            }
            val fetch = client.getBatchInverterEnergy(date.toString())
            if (fetch.issued) {
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
            when (val result = fetch.result) {
                is ApiResult.Success -> {
                    dailyPrefs.edit().putString("$KEY_PREFIX$date", encodeDailyJson(result.data)).apply()
                    window[date] = result.data
                }
                is ApiResult.ConfigurationError -> return currentState()
                is ApiResult.NetworkError -> {
                    setFetchError(FetchError.NETWORK)
                    return currentState()
                }
                is ApiResult.ApiError -> {
                    setFetchError(classifyApiError(result))
                    return currentState()
                }
            }
        }

        val previousStatus =
            healthPrefs
                .getString(KEY_STATUS, null)
                ?.let { runCatching { ModuleHealthStatus.valueOf(it) }.getOrNull() }
        val computed = computeStatus(window)
        val finalStatus =
            if (previousStatus == ModuleHealthStatus.RED &&
                computed.status == ModuleHealthStatus.YELLOW
            ) {
                ModuleHealthStatus.RED
            } else {
                computed.status
            }
        val state = computed.copy(status = finalStatus)

        healthPrefs
            .edit()
            .putString(KEY_STATUS, state.status.name)
            .putString(KEY_OFFLINE_MODULES, encodeOfflineModules(state.offlineModules))
            .putLong(KEY_LAST_CHECK, now)
            .remove(KEY_FETCH_ERROR)
            .apply()

        pruneCache(todayDate)

        return state.copy(checkedAtEpochMs = now)
    }

    /**
     * Pure status computation. Expected inverter set = union of all UIDs across the window.
     * Absent UID on a day is treated as 0 kWh (same as explicit 0.0).
     */
    internal fun computeStatus(window: Map<LocalDate, Map<String, Double>>): ModuleHealthState {
        if (window.isEmpty()) return ModuleHealthState(ModuleHealthStatus.UNKNOWN)

        val expectedUids = window.values.flatMap { it.keys }.toSet()
        if (expectedUids.isEmpty()) return ModuleHealthState(ModuleHealthStatus.GREEN)

        val sortedDates = window.keys.sortedDescending() // newest first
        val offlineModules = mutableListOf<Module>()

        for (uid in expectedUids) {
            var consecutive = 0
            for (date in sortedDates) {
                val kWh = window[date]?.get(uid) ?: 0.0
                if (kWh <= 0.0) consecutive++ else break
            }
            if (consecutive > 0) offlineModules.add(Module(uid, consecutive))
        }

        val status =
            when {
                offlineModules.isEmpty() -> ModuleHealthStatus.GREEN
                offlineModules.any { it.offlineDays >= WINDOW_DAYS } -> ModuleHealthStatus.RED
                else -> ModuleHealthStatus.YELLOW
            }
        return ModuleHealthState(status, offlineModules.sortedByDescending { it.offlineDays })
    }

    private fun pruneCache(todayDate: LocalDate) {
        val cutoff = todayDate.minusDays(WINDOW_DAYS.toLong())
        val toRemove =
            dailyPrefs.all.keys.filter { key ->
                key.startsWith(KEY_PREFIX) &&
                    runCatching {
                        LocalDate.parse(key.removePrefix(KEY_PREFIX)) < cutoff
                    }.getOrDefault(false)
            }
        if (toRemove.isNotEmpty()) {
            dailyPrefs.edit().apply { toRemove.forEach { remove(it) } }.apply()
        }
    }

    private fun parseDailyJson(json: String): Map<String, Double> =
        try {
            val obj = JSONObject(json)
            obj.keys().asSequence().associateWith { obj.getDouble(it) }
        } catch (e: JSONException) {
            emptyMap()
        }

    private fun encodeDailyJson(data: Map<String, Double>): String =
        JSONObject().also { obj -> data.forEach { (uid, kWh) -> obj.put(uid, kWh) } }.toString()

    private fun parseOfflineModules(json: String?): List<Module> {
        if (json == null) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map {
                val obj = arr.getJSONObject(it)
                Module(obj.getString("uid"), obj.getInt("days"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun encodeOfflineModules(modules: List<Module>): String =
        JSONArray()
            .also { arr ->
                modules.forEach { arr.put(JSONObject().put("uid", it.uid).put("days", it.offlineDays)) }
            }.toString()

    fun getLastNotifiedStatus(): ModuleHealthStatus? =
        healthPrefs
            .getString(KEY_LAST_NOTIFIED_STATUS, null)
            ?.let { runCatching { ModuleHealthStatus.valueOf(it) }.getOrNull() }

    fun setLastNotifiedStatus(status: ModuleHealthStatus) {
        healthPrefs.edit().putString(KEY_LAST_NOTIFIED_STATUS, status.name).apply()
    }

    fun getLastEmailedStatus(): ModuleHealthStatus? =
        healthPrefs
            .getString(KEY_LAST_EMAILED_STATUS, null)
            ?.let { runCatching { ModuleHealthStatus.valueOf(it) }.getOrNull() }

    fun setLastEmailedStatus(status: ModuleHealthStatus) {
        healthPrefs.edit().putString(KEY_LAST_EMAILED_STATUS, status.name).apply()
    }

    override fun resetThrottle() {
        healthPrefs
            .edit()
            .remove(KEY_LAST_CHECK)
            .remove(KEY_FETCH_ERROR)
            .remove(KEY_LAST_NOTIFIED_STATUS)
            .remove(KEY_LAST_EMAILED_STATUS)
            .apply()
    }

    private fun setFetchError(error: FetchError) {
        healthPrefs.edit().putString(KEY_FETCH_ERROR, error.name).apply()
    }

    private fun classifyApiError(error: ApiResult.ApiError): FetchError =
        if (error.code in AUTH_CODES || error.httpStatus == 401 || error.httpStatus == 403) {
            FetchError.AUTH
        } else {
            FetchError.API
        }

    companion object {
        const val PREFS_HEALTH = "ema_module_health"
        const val PREFS_DAILY = "ema_module_health_daily"
        private const val KEY_STATUS = "status"
        private const val KEY_OFFLINE_MODULES = "offlineModules"
        private const val KEY_LAST_CHECK = "lastCheckEpochMs"
        private const val KEY_FETCH_ERROR = "fetchError"
        const val KEY_LAST_NOTIFIED_STATUS = "lastNotifiedStatus"
        const val KEY_LAST_EMAILED_STATUS = "lastEmailedStatus"
        private const val KEY_PREFIX = "daily_"
        private const val WINDOW_DAYS = 3
        private val AUTH_CODES = setOf(2000, 2001, 2002, 2003, 2004, 3000, 3001, 3002, 3003, 3004)

        fun create(context: Context): ModuleHealthRepository {
            val settings = SettingsRepository.create(context)
            return ModuleHealthRepository(
                client = OkHttpEmaApiClient(settings),
                log = ApiCallLogRepository.create(context),
                healthPrefs = context.getSharedPreferences(PREFS_HEALTH, Context.MODE_PRIVATE),
                dailyPrefs = context.getSharedPreferences(PREFS_DAILY, Context.MODE_PRIVATE),
                appSecretProvider = { settings.getEmaAppSecret() },
            )
        }

        /** For tests: inject a fake client and controllable clock/date. */
        fun forTest(
            context: Context,
            client: EmaApiClient,
            clock: () -> Long = { System.currentTimeMillis() },
            today: () -> LocalDate = { LocalDate.now() },
        ): ModuleHealthRepository =
            ModuleHealthRepository(
                client = client,
                log = ApiCallLogRepository.create(context),
                healthPrefs = context.getSharedPreferences(PREFS_HEALTH, Context.MODE_PRIVATE),
                dailyPrefs = context.getSharedPreferences(PREFS_DAILY, Context.MODE_PRIVATE),
                appSecretProvider = { "" },
                clock = clock,
                today = today,
            )
    }
}
