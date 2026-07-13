package com.schlueternetz.emacompanion.core.api

/** UI-independent access to the EMA API. */
interface EmaApiClient {
    /**
     * Fetches all inverters' energy totals for a single day via the batch energy endpoint
     * (API section 3.5.3). Returns a map of inverter UID → total kWh (summed across channels).
     * @param date ISO date string, e.g. "2025-07-24"
     */
    suspend fun getBatchInverterEnergy(date: String): BatchEnergyFetch

    /**
     * Fetches today's 24 hourly kWh buckets (ECU energy endpoint, energy_level=hourly).
     * Null entries (no data for that hour) are omitted from [HourlySnapshot.hours].
     * @param date ISO date string, e.g. "2025-07-24"
     */
    suspend fun getHourlyEnergy(date: String): HourlyEnergyFetch = HourlyEnergyFetch(ApiResult.ConfigurationError)

    /**
     * Fetches daily kWh totals for all calendar days in [startDate]..[endDate].
     * Makes one API call per unique calendar month in the range.
     * @param startDate ISO date string, e.g. "2025-06-25"
     * @param endDate ISO date string, e.g. "2025-07-20"
     */
    suspend fun getDailyEnergy(
        startDate: String,
        endDate: String,
    ): DailyEnergyFetch = DailyEnergyFetch(ApiResult.ConfigurationError)
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

/** Today's hourly energy breakdown: hours 0–23, null hours omitted. */
data class HourlySnapshot(
    val hours: Map<Int, Double>,
)

/**
 * The typed [result] of a [getHourlyEnergy] call plus the details needed to log it.
 * For [ApiResult.ConfigurationError] no request is issued, so the detail fields are empty.
 */
data class HourlyEnergyFetch(
    val result: ApiResult<HourlySnapshot>,
    val endpoint: String = "",
    val durationMs: Long = 0,
    val requestText: String = "",
    val responseText: String = "",
) {
    val issued: Boolean get() = result !is ApiResult.ConfigurationError
}

/** Multi-day energy totals keyed by ISO date "YYYY-MM-DD". */
data class DailySnapshot(
    val days: Map<String, Double>,
)

/**
 * The typed [result] of a [getDailyEnergy] call plus the details needed to log it.
 * For [ApiResult.ConfigurationError] no request is issued, so the detail fields are empty.
 */
data class DailyEnergyFetch(
    val result: ApiResult<DailySnapshot>,
    val endpoint: String = "",
    val durationMs: Long = 0,
    val requestText: String = "",
    val responseText: String = "",
) {
    val issued: Boolean get() = result !is ApiResult.ConfigurationError
}
