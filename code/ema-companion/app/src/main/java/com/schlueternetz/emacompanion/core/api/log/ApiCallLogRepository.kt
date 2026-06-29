package com.schlueternetz.emacompanion.core.api.log

import android.content.Context
import android.content.SharedPreferences
import com.schlueternetz.emacompanion.core.Masking
import org.json.JSONArray
import org.json.JSONObject

/**
 * Bounded (newest-first, cap [MAX_RECORDS]) store of API call records in its own plain
 * `SharedPreferences` file, separate from the encrypted settings store. Read lazily — only
 * on append or when the Logs screen opens — so it never touches the app/settings start path.
 *
 * Secrets are masked at append time, so the App Secret can never be reconstructed from a log.
 */
class ApiCallLogRepository(private val prefs: SharedPreferences) {

    fun append(log: ApiCallLog, secret: String = "") {
        val safe = if (secret.isNotEmpty()) {
            val masked = Masking.mask(secret)
            log.copy(
                requestText = log.requestText.replace(secret, masked),
                responseText = log.responseText.replace(secret, masked),
            )
        } else {
            log
        }
        val all = getAll().toMutableList()
        all.add(0, safe)
        while (all.size > MAX_RECORDS) all.removeAt(all.size - 1)
        prefs.edit().putString(KEY_LOG, serialize(all)).apply()
    }

    fun getAll(): List<ApiCallLog> {
        val raw = prefs.getString(KEY_LOG, null) ?: return emptyList()
        val array = JSONArray(raw)
        return (0 until array.length()).map { deserialize(array.getJSONObject(it)) }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun serialize(logs: List<ApiCallLog>): String {
        val array = JSONArray()
        logs.forEach { log ->
            array.put(
                JSONObject()
                    .put("timestampMs", log.timestampMs)
                    .put("endpoint", log.endpoint)
                    .put("durationMs", log.durationMs)
                    .put("success", log.success)
                    .put("requestText", log.requestText)
                    .put("responseText", log.responseText),
            )
        }
        return array.toString()
    }

    private fun deserialize(obj: JSONObject): ApiCallLog = ApiCallLog(
        timestampMs = obj.getLong("timestampMs"),
        endpoint = obj.getString("endpoint"),
        durationMs = obj.getLong("durationMs"),
        success = obj.getBoolean("success"),
        requestText = obj.getString("requestText"),
        responseText = obj.getString("responseText"),
    )

    companion object {
        const val PREFS_NAME = "ema_api_log"
        const val MAX_RECORDS = 100
        private const val KEY_LOG = "log"

        fun create(context: Context): ApiCallLogRepository =
            ApiCallLogRepository(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
    }
}
