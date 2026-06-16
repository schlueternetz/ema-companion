package com.schlueternetz.emacompanion.core.api

import android.content.Context
import com.schlueternetz.emacompanion.core.api.log.ApiCallLog
import com.schlueternetz.emacompanion.core.api.log.ApiCallLogRepository
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository

/** What Home renders: the latest known production value (if any) and whether the last attempt failed on the network. */
data class ProductionState(
    val snapshot: ProductionSnapshot? = null,
    val networkError: Boolean = false,
)

/** The dependency Home relies on; lets tests substitute a fake without HTTP. */
interface ProductionSource {
    suspend fun refresh(): ProductionState
}

/**
 * Orchestrates a current-production fetch: enforces the 10-minute throttle (persisted via
 * [ApiUsageRepository.lastFetchEpochMs]) and, only when a request is actually issued, counts it
 * and appends a (secret-masked) log record. Home calls [refresh] and renders the returned state.
 */
class ProductionRepository(
    private val client: EmaApiClient,
    private val usage: ApiUsageRepository,
    private val log: ApiCallLogRepository,
    private val appSecretProvider: () -> String,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ProductionSource {

    // Seed from the last persisted value so the number survives Home being recreated
    // (e.g. navigating away and back) while the throttle window blocks a refetch.
    private var cached: ProductionSnapshot? =
        usage.getLastProductionWatts().takeIf { it >= 0 }?.let { ProductionSnapshot(it) }
    private var networkError = false

    override suspend fun refresh(): ProductionState {
        val now = clock()
        if (now - usage.getLastFetchEpochMs() < THROTTLE_MS) {
            return currentState()
        }

        val fetch = client.getCurrentProduction()
        if (!fetch.issued) {
            // Not configured — no request issued, so don't count, log, or start the throttle.
            return currentState()
        }

        usage.setLastFetchEpochMs(now)
        usage.recordRequest()
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

        when (val result = fetch.result) {
            is ApiResult.Success -> {
                cached = result.data
                usage.setLastProductionWatts(result.data.powerWatts)
                networkError = false
            }
            is ApiResult.NetworkError -> networkError = true
            else -> networkError = false
        }
        return currentState()
    }

    private fun currentState() = ProductionState(snapshot = cached, networkError = networkError)

    companion object {
        const val THROTTLE_MS = 10 * 60 * 1000L

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
