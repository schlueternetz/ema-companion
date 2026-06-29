package com.schlueternetz.emacompanion.core.api

/** UI-independent access to the EMA API. */
interface EmaApiClient {
    suspend fun getCurrentProduction(): ProductionFetch

    /**
     * Fetches all inverters' energy totals for a single day via the batch energy endpoint
     * (API section 3.5.3). Returns a map of inverter UID → total kWh (summed across channels).
     * @param date ISO date string, e.g. "2025-07-24"
     */
    suspend fun getBatchInverterEnergy(date: String): BatchEnergyFetch
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

/**
 * The typed [result] of a batch inverter energy call plus the details needed to log it.
 * [result] maps inverter UID to total kWh (all channels summed) for the requested date.
 */
data class BatchEnergyFetch(
    val result: ApiResult<Map<String, Double>>,
    val endpoint: String = "",
    val durationMs: Long = 0,
    val requestText: String = "",
    val responseText: String = "",
) {
    val issued: Boolean get() = result !is ApiResult.ConfigurationError
}
