package com.schlueternetz.emacompanion.core.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.schlueternetz.emacompanion.core.api.log.ApiCallLogRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProductionRepositoryTest {

    private lateinit var usage: ApiUsageRepository
    private lateinit var log: ApiCallLogRepository
    private var now = 1_000_000L

    private class FakeClient(var fetch: ProductionFetch) : EmaApiClient {
        var calls = 0
        override suspend fun getCurrentProduction(): ProductionFetch {
            calls++
            return fetch
        }

        override suspend fun getBatchInverterEnergy(date: String): BatchEnergyFetch =
            BatchEnergyFetch(ApiResult.ConfigurationError)
    }

    private fun successFetch(power: Int = 8000) = ProductionFetch(
        result = ApiResult.Success(ProductionSnapshot(power)),
        endpoint = "/ecu/energy",
        durationMs = 5L,
        requestText = "GET",
        responseText = "{}",
    )

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.getSharedPreferences("usage_t", Context.MODE_PRIVATE).edit().clear().apply()
        ctx.getSharedPreferences("log_t", Context.MODE_PRIVATE).edit().clear().apply()
        usage = ApiUsageRepository(
            ctx.getSharedPreferences("usage_t", Context.MODE_PRIVATE),
            monthProvider = { "2026-06" },
        )
        log = ApiCallLogRepository(ctx.getSharedPreferences("log_t", Context.MODE_PRIVATE))
    }

    private fun repo(client: EmaApiClient) = ProductionRepository(
        client = client,
        usage = usage,
        log = log,
        appSecretProvider = { "secret123456" },
        clock = { now },
    )

    @Test
    fun fetchesWhenWindowElapsed() = runBlocking {
        val client = FakeClient(successFetch())
        val state = repo(client).refresh()
        assertEquals(ProductionSnapshot(8000), state.snapshot)
        assertEquals(1, client.calls)
        assertEquals(1, usage.getRequestCount())
        assertEquals(1, log.getAll().size)
    }

    @Test
    fun newInstanceWithinThrottle_showsLastPersistedSnapshot() = runBlocking {
        // First open: success, value persisted along with the throttle timestamp.
        repo(FakeClient(successFetch(8000))).refresh()
        // Navigate away and back within 10 min: Home + its repository are recreated.
        now += 2 * 60 * 1000L
        val freshClient = FakeClient(successFetch(9999))
        val state = repo(freshClient).refresh()
        assertEquals(0, freshClient.calls) // throttled — no new request
        assertEquals(ProductionSnapshot(8000), state.snapshot) // last value survives recreation
    }

    @Test
    fun noOpWithinThrottleWindow() = runBlocking {
        val client = FakeClient(successFetch())
        val repository = repo(client)
        repository.refresh()
        now += 5 * 60 * 1000L // 5 minutes later, within the 10-minute window
        val state = repository.refresh()
        assertEquals(1, client.calls) // no second call
        assertEquals(1, usage.getRequestCount()) // not incremented
        assertEquals(1, log.getAll().size) // not logged
        assertEquals(ProductionSnapshot(8000), state.snapshot) // cached value still shown
    }

    @Test
    fun fetchesAgainAfterWindowElapses() = runBlocking {
        val client = FakeClient(successFetch())
        val repository = repo(client)
        repository.refresh()
        now += 10 * 60 * 1000L // exactly 10 minutes later
        repository.refresh()
        assertEquals(2, client.calls)
        assertEquals(2, usage.getRequestCount())
        assertEquals(2, log.getAll().size)
    }

    @Test
    fun networkError_setsBannerAndKeepsCachedSnapshot() = runBlocking {
        val client = FakeClient(successFetch())
        val repository = repo(client)
        repository.refresh() // success, cached
        now += 11 * 60 * 1000L
        client.fetch = ProductionFetch(ApiResult.NetworkError, "/ecu/energy", 1L, "GET", "")
        val state = repository.refresh()
        assertEquals(FetchError.NETWORK, state.error)
        assertEquals(ProductionSnapshot(8000), state.snapshot)
    }

    @Test
    fun apiError_setsBannerAndKeepsCachedSnapshot() = runBlocking {
        val client = FakeClient(successFetch())
        val repository = repo(client)
        repository.refresh() // success, cached
        now += 11 * 60 * 1000L
        client.fetch = ProductionFetch(ApiResult.ApiError(code = 4000), "/ecu/energy", 1L, "GET", """{"code":4000}""")
        val state = repository.refresh()
        assertEquals(FetchError.API, state.error)
        assertEquals(ProductionSnapshot(8000), state.snapshot)
    }

    @Test
    fun success_clearsBannerAfterError() = runBlocking {
        val client = FakeClient(ProductionFetch(ApiResult.NetworkError, "/ecu/energy", 1L, "GET", ""))
        val repository = repo(client)
        repository.refresh() // network error
        now += 11 * 60 * 1000L
        client.fetch = successFetch()
        val state = repository.refresh()
        assertEquals(null, state.error)
        assertEquals(ProductionSnapshot(8000), state.snapshot)
    }

    @Test
    fun configurationError_doesNotCountOrLogOrThrottle() = runBlocking {
        val client = FakeClient(ProductionFetch(ApiResult.ConfigurationError))
        val repository = repo(client)
        val state = repository.refresh()
        assertEquals(null, state.snapshot)
        assertEquals(0, usage.getRequestCount())
        assertEquals(0, log.getAll().size)
        assertEquals(0L, usage.getLastFetchEpochMs()) // not throttled by a non-issued call
    }

    @Test
    fun networkError_isLoggedButNotCountedAndDoesNotThrottle() = runBlocking {
        val client = FakeClient(ProductionFetch(ApiResult.NetworkError, "/ecu/energy", 1L, "GET", ""))
        repo(client).refresh()
        // Never reached the API → not counted, throttle not started, but still logged.
        assertEquals(0, usage.getRequestCount())
        assertEquals(0L, usage.getLastFetchEpochMs())
        assertEquals(1, log.getAll().size)
        assertFalse(log.getAll()[0].success)
    }

    @Test
    fun networkError_doesNotBlockNextAttempt() = runBlocking {
        val client = FakeClient(ProductionFetch(ApiResult.NetworkError, "/ecu/energy", 1L, "GET", ""))
        val repository = repo(client)
        repository.refresh() // network failure — no throttle started
        now += 1000L // only 1 second later, well inside the 10-minute window
        repository.refresh()
        assertEquals(2, client.calls) // retried immediately, not throttled
    }

    @Test
    fun success_persistsValueTimestampForNextInstance() = runBlocking {
        repo(FakeClient(successFetch(8000))).refresh() // success at `now`
        val fetchedAt = now
        now += 2 * 60 * 1000L // recreate within throttle window
        val state = repo(FakeClient(successFetch(9999))).refresh()
        assertEquals(fetchedAt, state.updatedAtEpochMs) // timestamp reconstructed from store
        assertEquals(ProductionSnapshot(8000), state.snapshot)
    }

    @Test
    fun errorAndValuePersistViaCurrentState() = runBlocking {
        repo(FakeClient(successFetch(8000))).refresh() // success persists the value
        now += 11 * 60 * 1000L
        repo(FakeClient(ProductionFetch(ApiResult.ApiError(code = 4000), "/e", 1L, "GET", "{}"))).refresh()
        // A recreated tile reconstructs value + error from persisted state, with no fetch.
        val state = repo(FakeClient(successFetch(9999))).currentState()
        assertEquals(FetchError.API, state.error)
        assertEquals(ProductionSnapshot(8000), state.snapshot)
    }

    @Test
    fun success_clearsPersistedErrorForNextInstance() = runBlocking {
        repo(FakeClient(ProductionFetch(ApiResult.ApiError(code = 4000), "/e", 1L, "GET", "{}"))).refresh()
        now += 11 * 60 * 1000L
        repo(FakeClient(successFetch(8000))).refresh() // success clears the persisted error
        now += 11 * 60 * 1000L
        // A fresh instance must not resurrect the old banner.
        val state = repo(FakeClient(successFetch(8000))).refresh()
        assertEquals(null, state.error)
    }

    @Test
    fun apiError_isNotCountedDoesNotThrottleAndRetries() = runBlocking {
        val client = FakeClient(ProductionFetch(ApiResult.ApiError(code = 4000), "/ecu/energy", 1L, "GET", """{"code":4000}"""))
        val repository = repo(client)
        repository.refresh() // failure — not billed, not throttled
        assertEquals(0, usage.getRequestCount())
        assertEquals(0L, usage.getLastFetchEpochMs())
        now += 1000L
        repository.refresh() // retries immediately, even within the window
        assertEquals(2, client.calls)
    }

    @Test
    fun authError_isClassifiedAndNotCounted() = runBlocking {
        val client = FakeClient(ProductionFetch(ApiResult.ApiError(code = 2001), "/e", 1L, "GET", """{"code":2001}"""))
        val state = repo(client).refresh()
        assertEquals(FetchError.AUTH, state.error)
        assertEquals(0, usage.getRequestCount())
        assertEquals(0L, usage.getLastFetchEpochMs())
    }

    @Test
    fun onlySuccessCounts_failureThenSuccess() = runBlocking {
        val client = FakeClient(ProductionFetch(ApiResult.ApiError(code = 4000), "/e", 1L, "GET", "{}"))
        val repository = repo(client)
        repository.refresh() // failure: count stays 0
        now += 1000L
        client.fetch = successFetch(8000)
        repository.refresh() // success: counts once
        assertEquals(1, usage.getRequestCount())
    }
}
