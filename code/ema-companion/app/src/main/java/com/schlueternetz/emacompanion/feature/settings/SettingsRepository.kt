package com.schlueternetz.emacompanion.feature.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.json.JSONException
import org.json.JSONObject
import java.util.TimeZone

class SettingsRepository(
    private val prefs: SharedPreferences,
) {
    companion object {
        const val LANGUAGE_KEY = "language"
        const val LANGUAGE_DEFAULT = "system"

        const val EMA_APP_ID_KEY = "emaAppId"
        const val EMA_APP_SECRET_KEY = "emaAppSecret"
        const val EMA_SYSTEM_ID_KEY = "emaSystemId"
        const val EMA_ECU_ID_KEY = "emaEcuId"
        const val SYSTEM_CAPACITY_KEY = "systemCapacity"
        const val HISTORIC_DATA_DAYS_KEY = "historicDataDays"
        const val API_REQUEST_LIMIT_KEY = "apiRequestLimit"
        const val NOTIFICATIONS_ENABLED_KEY = "notificationsEnabled"
        const val EMAIL_ALERTS_ENABLED_KEY = "emailAlertsEnabled"
        const val EMAIL_ADDRESS_KEY = "emailAddress"
        const val EMAIL_APP_PASSWORD_KEY = "emailAppPassword"
        const val BASE_URL_KEY = "baseUrl"
        const val DISPLAY_MODE_KEY = "displayMode"
        const val ARRAY_TIMEZONE_KEY = "arrayTimezone"

        const val BASE_URL_DEFAULT = "https://api.apsystemsema.com:9282/user/api/v2/"
        const val DISPLAY_MODE_DEFAULT = "system"
        const val API_REQUEST_LIMIT_DEFAULT = 1000
        const val HISTORIC_DATA_DAYS_DEFAULT = 30
        const val API_REQUEST_LIMIT_MAX_PER_MONTH = 1 * 60 * 60 * 24 * 31 // 1 req/sec * 60s * 60m * 24h * 31d = 2,678,400
        const val SYSTEM_CAPACITY_MAX_KW = 2_000f // 2,000 kW
        const val BASE_URL_MAX_LENGTH = 2048

        @Volatile
        private var cached: SettingsRepository? = null

        @Volatile
        private var cachedAppContext: Context? = null

        // Cache the instance per application context so the keystore-backed
        // EncryptedSharedPreferences (MasterKeys + crypto init) is built once, not on every
        // Activity/Fragment creation — that work runs on the main thread and is a noticeable
        // cost on older devices, repeated on every tab switch. Keyed on the application context
        // so each Robolectric test (fresh Application per test) gets a fresh instance.
        fun create(context: Context): SettingsRepository {
            val appContext = context.applicationContext
            cached?.takeIf { cachedAppContext === appContext }?.let { return it }
            val instance =
                try {
                    val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                    SettingsRepository(
                        EncryptedSharedPreferences.create(
                            "ema_companion_settings",
                            masterKeyAlias,
                            appContext,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                        ),
                    )
                } catch (e: Exception) {
                    // Keystore unavailable (e.g., corrupted store or test environment);
                    // fall back to plain SharedPreferences so the app remains functional.
                    SettingsRepository(
                        appContext.getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE),
                    )
                }
            cached = instance
            cachedAppContext = appContext
            return instance
        }
    }

    fun getLanguage(): String =
        try {
            prefs.getString(LANGUAGE_KEY, LANGUAGE_DEFAULT) ?: LANGUAGE_DEFAULT
        } catch (e: SecurityException) {
            LANGUAGE_DEFAULT
        }

    fun setLanguage(language: String) {
        prefs.edit().putString(LANGUAGE_KEY, language).apply()
    }

    fun getEmaAppId(): String = prefs.getString(EMA_APP_ID_KEY, "") ?: ""

    fun setEmaAppId(value: String) {
        prefs.edit().putString(EMA_APP_ID_KEY, value).apply()
    }

    fun getEmaAppSecret(): String = prefs.getString(EMA_APP_SECRET_KEY, "") ?: ""

    fun setEmaAppSecret(value: String) {
        prefs.edit().putString(EMA_APP_SECRET_KEY, value).apply()
    }

    fun getEmaSystemId(): String = prefs.getString(EMA_SYSTEM_ID_KEY, "") ?: ""

    fun setEmaSystemId(value: String) {
        prefs.edit().putString(EMA_SYSTEM_ID_KEY, value).apply()
    }

    fun getEmaEcuId(): String = prefs.getString(EMA_ECU_ID_KEY, "") ?: ""

    fun setEmaEcuId(value: String) {
        prefs.edit().putString(EMA_ECU_ID_KEY, value).apply()
    }

    fun getSystemCapacity(): Float = prefs.getFloat(SYSTEM_CAPACITY_KEY, -1f)

    fun setSystemCapacity(value: Float) {
        prefs.edit().putFloat(SYSTEM_CAPACITY_KEY, value).apply()
    }

    fun getHistoricDataDays(): Int = prefs.getInt(HISTORIC_DATA_DAYS_KEY, HISTORIC_DATA_DAYS_DEFAULT)

    fun setHistoricDataDays(value: Int) {
        prefs.edit().putInt(HISTORIC_DATA_DAYS_KEY, value).apply()
    }

    fun getApiRequestLimit(): Int = prefs.getInt(API_REQUEST_LIMIT_KEY, API_REQUEST_LIMIT_DEFAULT)

    fun setApiRequestLimit(value: Int) {
        prefs.edit().putInt(API_REQUEST_LIMIT_KEY, value).apply()
    }

    fun getNotificationsEnabled(): Boolean = prefs.getBoolean(NOTIFICATIONS_ENABLED_KEY, true)

    fun setNotificationsEnabled(value: Boolean) {
        prefs.edit().putBoolean(NOTIFICATIONS_ENABLED_KEY, value).apply()
    }

    fun getEmailAlertsEnabled(): Boolean = prefs.getBoolean(EMAIL_ALERTS_ENABLED_KEY, false)

    fun setEmailAlertsEnabled(value: Boolean) {
        prefs.edit().putBoolean(EMAIL_ALERTS_ENABLED_KEY, value).apply()
    }

    fun getEmailAddress(): String = prefs.getString(EMAIL_ADDRESS_KEY, "") ?: ""

    fun setEmailAddress(value: String) {
        prefs.edit().putString(EMAIL_ADDRESS_KEY, value).apply()
    }

    fun getEmailAppPassword(): String = prefs.getString(EMAIL_APP_PASSWORD_KEY, "") ?: ""

    fun setEmailAppPassword(value: String) {
        prefs.edit().putString(EMAIL_APP_PASSWORD_KEY, value).apply()
    }

    fun deleteEmailCredentials() {
        prefs
            .edit()
            .remove(EMAIL_ADDRESS_KEY)
            .remove(EMAIL_APP_PASSWORD_KEY)
            .apply()
    }

    fun isEmailConfigured(): Boolean = getEmailAddress().isNotEmpty() && getEmailAppPassword().isNotEmpty()

    fun getBaseUrl(): String = prefs.getString(BASE_URL_KEY, BASE_URL_DEFAULT) ?: BASE_URL_DEFAULT

    fun setBaseUrl(value: String) {
        prefs.edit().putString(BASE_URL_KEY, value).apply()
    }

    fun getDisplayMode(): String = prefs.getString(DISPLAY_MODE_KEY, DISPLAY_MODE_DEFAULT) ?: DISPLAY_MODE_DEFAULT

    fun setDisplayMode(value: String) {
        prefs.edit().putString(DISPLAY_MODE_KEY, value).apply()
    }

    fun getArrayTimezone(): String = prefs.getString(ARRAY_TIMEZONE_KEY, null) ?: TimeZone.getDefault().id

    fun setArrayTimezone(value: String) {
        prefs.edit().putString(ARRAY_TIMEZONE_KEY, value).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun isConfigured(): Boolean =
        getEmaAppId().isNotEmpty() &&
            getEmaAppSecret().isNotEmpty() &&
            getEmaSystemId().isNotEmpty() &&
            getEmaEcuId().isNotEmpty() &&
            getSystemCapacity() != -1f

    fun exportToJson(): String =
        JSONObject()
            .apply {
                put(LANGUAGE_KEY, getLanguage())
                put(EMA_APP_ID_KEY, getEmaAppId())
                put(EMA_APP_SECRET_KEY, getEmaAppSecret())
                put(EMA_SYSTEM_ID_KEY, getEmaSystemId())
                put(EMA_ECU_ID_KEY, getEmaEcuId())
                put(SYSTEM_CAPACITY_KEY, getSystemCapacity().toDouble())
                put(HISTORIC_DATA_DAYS_KEY, getHistoricDataDays())
                put(API_REQUEST_LIMIT_KEY, getApiRequestLimit())
                put(NOTIFICATIONS_ENABLED_KEY, getNotificationsEnabled())
                put(BASE_URL_KEY, getBaseUrl())
                put(DISPLAY_MODE_KEY, getDisplayMode())
                put(ARRAY_TIMEZONE_KEY, getArrayTimezone())
            }.toString()

    fun importFromJson(json: String) {
        val obj =
            try {
                JSONObject(json)
            } catch (e: JSONException) {
                throw IllegalArgumentException("Malformed JSON: ${e.message}", e)
            }
        val edit = prefs.edit()
        if (obj.has(LANGUAGE_KEY)) edit.putString(LANGUAGE_KEY, obj.getString(LANGUAGE_KEY))
        if (obj.has(EMA_APP_ID_KEY)) edit.putString(EMA_APP_ID_KEY, obj.getString(EMA_APP_ID_KEY))
        if (obj.has(EMA_APP_SECRET_KEY)) edit.putString(EMA_APP_SECRET_KEY, obj.getString(EMA_APP_SECRET_KEY))
        if (obj.has(EMA_SYSTEM_ID_KEY)) edit.putString(EMA_SYSTEM_ID_KEY, obj.getString(EMA_SYSTEM_ID_KEY))
        if (obj.has(EMA_ECU_ID_KEY)) edit.putString(EMA_ECU_ID_KEY, obj.getString(EMA_ECU_ID_KEY))
        if (obj.has(SYSTEM_CAPACITY_KEY)) edit.putFloat(SYSTEM_CAPACITY_KEY, obj.getDouble(SYSTEM_CAPACITY_KEY).toFloat())
        if (obj.has(HISTORIC_DATA_DAYS_KEY)) edit.putInt(HISTORIC_DATA_DAYS_KEY, obj.getInt(HISTORIC_DATA_DAYS_KEY))
        if (obj.has(API_REQUEST_LIMIT_KEY)) edit.putInt(API_REQUEST_LIMIT_KEY, obj.getInt(API_REQUEST_LIMIT_KEY))
        if (obj.has(NOTIFICATIONS_ENABLED_KEY)) edit.putBoolean(NOTIFICATIONS_ENABLED_KEY, obj.getBoolean(NOTIFICATIONS_ENABLED_KEY))
        if (obj.has(BASE_URL_KEY)) edit.putString(BASE_URL_KEY, obj.getString(BASE_URL_KEY))
        if (obj.has(DISPLAY_MODE_KEY)) edit.putString(DISPLAY_MODE_KEY, obj.getString(DISPLAY_MODE_KEY))
        if (obj.has(ARRAY_TIMEZONE_KEY)) edit.putString(ARRAY_TIMEZONE_KEY, obj.getString(ARRAY_TIMEZONE_KEY))
        edit.apply()
    }
}
