package com.schlueternetz.emacompanion.core.api

import android.content.Context
import com.schlueternetz.emacompanion.core.AppConfig
import com.schlueternetz.emacompanion.core.api.log.ApiCallLog
import com.schlueternetz.emacompanion.core.api.log.ApiCallLogRepository
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository

/** Why the last fetch failed, shown as a status line inside the Home tile. */
enum class FetchError {
    /** Never reached EMA (no connection, timeout). */
    NETWORK,

    /** Reached EMA but the credentials/authorization were rejected. */
    AUTH,

    /** Reached EMA but it returned some other error (bad parameters, server error, …). */
    API,
}

/**
 * What the Home tile renders: the latest known production value (if any), when that value was
 * fetched ([updatedAtEpochMs], for the "Updated …" line), and how the last attempt failed (if it did).
 */
data class ProductionState(
    val snapshot: ProductionSnapshot? = null,
    val updatedAtEpochMs: Long? = null,
    val error: FetchError? = null,
)

/** The dependency Home relies on; lets tests substitute a fake without HTTP. */
interface ProductionSource {
    /** The state to show immediately (reconstructed from persisted state), before any new fetch. */
    fun currentState(): ProductionState

    /** Fetch if due, then return the new state. */
    suspend fun refresh(): ProductionState
}

/**
 * Orchestrates a current-production fetch. Only a **successful** read counts towards the monthly
 * limit and starts the 10-minute throttle (you are billed for data access, so a failure is free
 * and retried on the next trigger). Every attempt that reaches code is logged. Home calls
 * [currentState] for an instant render and [refresh] to fetch.
 */
class ProductionRepository(
    private val client: EmaApiClient,
    private val usage: ApiUsageRepository,
    private val log: ApiCallLogRepository,
    private val appSecretProvider: () -> String,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ProductionSource {

    // Seed from persisted state so the value, its timestamp, AND the error status survive Home
    // being recreated (navigating away and back) and even process death — the rendered state is
    // purely a function of what is stored, so a recreated tile looks identical.
    private var cached: ProductionSnapshot? =
        usage.getLastProductionWatts().takeIf { it >= 0 }?.let { ProductionSnapshot(it) }
    private var cachedAtEpochMs: Long? = usage.getLastProductionEpochMs().takeIf { it > 0 }
    private var error: FetchError? = usage.getLastError()

    override suspend fun refresh(): ProductionState {
        val now = clock()
        // Only a successful read starts the throttle, so a previous failure never blocks a retry.
        if (now - usage.getLastFetchEpochMs() < AppConfig.PRODUCTION_FETCH_INTERVAL_MS) {
            return currentState()
        }

        val fetch = client.getCurrentProduction()
        when (val result = fetch.result) {
            // Not configured — no request issued: nothing to log, count, or throttle.
            is ApiResult.ConfigurationError -> Unit
            is ApiResult.Success -> {
                logCall(now, fetch)
                usage.setLastFetchEpochMs(now) // only success throttles
                usage.recordRequest() // only success counts towards the monthly limit
                cached = result.data
                cachedAtEpochMs = now
                usage.setLastProduction(result.data.powerWatts, now)
                setError(null)
            }
            // Any failure that reached code (network down, auth rejected, bad params, server error)
            // is logged and shown, but is neither counted nor throttled — it retries next trigger.
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

    private fun logCall(now: Long, fetch: ProductionFetch) {
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
        usage.setLastError(value)
    }

    override fun currentState() =
        ProductionState(snapshot = cached, updatedAtEpochMs = cachedAtEpochMs, error = error)

    companion object {
        // EMA account/authorization and token error codes (manual §4) → treated as auth failures.
        private val AUTH_CODES = setOf(2000, 2001, 2002, 2003, 2004, 3000, 3001, 3002, 3003, 3004)

        fun create(context: Context): ProductionRepository {
            val settings = SettingsRepository.create(context)
            return ProductionRepository(
                client = OkHttpEmaApiClient(settings),
                usage = ApiUsageRepository.create(context),
                log = ApiCallLogRepository.create(context),
                appSecretProvider = { settings.getEmaAppSecret() },
            )
        }
    }
}
