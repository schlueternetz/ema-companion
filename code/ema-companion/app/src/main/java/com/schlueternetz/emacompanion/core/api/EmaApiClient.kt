package com.schlueternetz.emacompanion.core.api

/** UI-independent access to the EMA API. */
interface EmaApiClient {
    suspend fun getCurrentProduction(): ProductionFetch
}

/**
 * The typed [result] of a current-production call plus the details needed to log it.
 * For [ApiResult.ConfigurationError] no request is issued, so the detail fields are empty.
 */
data class ProductionFetch(
    val result: ApiResult<ProductionSnapshot>,
    val endpoint: String = "",
    val durationMs: Long = 0,
    val requestText: String = "",
    val responseText: String = "",
) {
    val issued: Boolean get() = result !is ApiResult.ConfigurationError
}
