package com.schlueternetz.emacompanion.core.api.log

/** A single recorded EMA API call, shown in the Settings Logs section. */
data class ApiCallLog(
    val timestampMs: Long,
    val endpoint: String,
    val durationMs: Long,
    val success: Boolean,
    val requestText: String,
    val responseText: String,
)
