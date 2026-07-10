package com.schlueternetz.emacompanion.feature.widgets

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.schlueternetz.emacompanion.core.api.ApiResult
import com.schlueternetz.emacompanion.core.api.ApiUsageRepository
import com.schlueternetz.emacompanion.core.api.BatchEnergyFetch
import com.schlueternetz.emacompanion.core.api.EmaApiClient
import com.schlueternetz.emacompanion.core.api.HourlyEnergyFetch
import com.schlueternetz.emacompanion.core.api.HourlyEnergyRepository
import com.schlueternetz.emacompanion.core.api.HourlySnapshot
import com.schlueternetz.emacompanion.core.api.ProductionFetch
import com.schlueternetz.emacompanion.core.api.log.ApiCallLogRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Proves the architectural claim behind background refresh (design.md Decision 5): the worker and
 * `HomeFragment` are two callers of the SAME persisted repository cache, not separate fetch paths.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WidgetRefreshWorkerHomeIntegrationTest {
    private lateinit var context: Context
    private lateinit var client: FakeClient
    private var now = 3_700_000L

    private class FakeClient : EmaApiClient {
        var calls = 0

        override suspend fun getCurrentProduction() = ProductionFetch(ApiResult.ConfigurationError)

        override suspend fun getBatchInverterEnergy(date: String) = BatchEnergyFetch(ApiResult.ConfigurationError)

        override suspend fun getHourlyEnergy(date: String): HourlyEnergyFetch {
            calls++
            return HourlyEnergyFetch(ApiResult.Success(HourlySnapshot(mapOf(6 to 1.0, 7 to 2.0))), "/x", 5L, "req", "{}")
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context
            .getSharedPreferences(HourlyEnergyRepository.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        client = FakeClient()
    }

    private fun newRepo() =
        HourlyEnergyRepository(
            client = client,
            usageCounter = ApiUsageRepository.create(context),
            log = ApiCallLogRepository.create(context),
            appSecretProvider = { "secret" },
            today = { "2026-07-15" },
            clock = { now },
            prefs = context.getSharedPreferences(HourlyEnergyRepository.PREFS_NAME, Context.MODE_PRIVATE),
        )

    @Test
    fun backgroundRefresh_isVisibleToANewlyConstructedRepositoryInstance() =
        runBlocking {
            // Simulates WidgetRefreshWorker running in one process.
            val workerRepo = newRepo()
            workerRepo.refresh(force = false)
            assertEquals(1, client.calls)

            // Simulates HomeFragment constructing its own repository instance after reopening the app.
            val homeRepo = newRepo()
            val seededState = homeRepo.currentState()
            assertEquals(HourlySnapshot(mapOf(6 to 1.0, 7 to 2.0)), seededState.snapshot)

            // Its own onResume refresh is throttled into a no-op: no second network call.
            homeRepo.refresh(force = false)
            assertEquals(1, client.calls)
        }
}
