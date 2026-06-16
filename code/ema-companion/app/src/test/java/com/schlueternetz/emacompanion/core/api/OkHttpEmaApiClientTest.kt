package com.schlueternetz.emacompanion.core.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OkHttpEmaApiClientTest {

    private lateinit var server: MockWebServer
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("api_client_test", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        settings = SettingsRepository(prefs).apply {
            setEmaAppId("a".repeat(32))
            setEmaAppSecret("secret123456")
            setEmaSystemId("b".repeat(16))
            setEmaEcuId("203000001234")
            setSystemCapacity(10f)
            setBaseUrl(server.url("/user/api/v2/").toString())
        }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client() = OkHttpEmaApiClient(settings)

    @Test
    fun success_returnsLastPowerSample() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {"code":0,"data":{
                  "today":"2.64","time":["06:00","06:05"],
                  "power":[1500,3200,5000,6500,7400,8000],"energy":["0.1","0.2"]
                }}
                """.trimIndent(),
            ),
        )
        val fetch = client().getCurrentProduction()
        assertEquals(ApiResult.Success(ProductionSnapshot(8000)), fetch.result)
    }

    @Test
    fun success_sendsSignedHeaders() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"code":0,"data":{"power":[8000]}}"""))
        client().getCurrentProduction()
        val recorded = server.takeRequest()
        assertTrue(recorded.getHeader("X-CA-AppId")!!.isNotEmpty())
        assertTrue(recorded.getHeader("X-CA-Timestamp")!!.isNotEmpty())
        assertEquals(32, recorded.getHeader("X-CA-Nonce")!!.length)
        assertEquals("HmacSHA256", recorded.getHeader("X-CA-Signature-Method"))
        assertTrue(recorded.getHeader("X-CA-Signature")!!.isNotEmpty())
        assertEquals("203000001234", recorded.requestUrl!!.pathSegments.last())
        assertEquals("minutely", recorded.requestUrl!!.queryParameter("energy_level"))
    }

    @Test
    fun emptyPower_returnsZeroProduction() = runBlocking {
        // No samples (e.g. night / before dawn) is "not producing", i.e. 0 W — a successful
        // read, not an error: the solar owner must not see a red error status every evening.
        server.enqueue(MockResponse().setBody("""{"code":0,"data":{"power":[]}}"""))
        val fetch = client().getCurrentProduction()
        assertEquals(ApiResult.Success(ProductionSnapshot(0)), fetch.result)
    }

    @Test
    fun missingPowerArray_returnsApiError() = runBlocking {
        // code 0 but no power array at all is an unexpected shape, not "0 W" — keep it an error.
        server.enqueue(MockResponse().setBody("""{"code":0,"data":{}}"""))
        val fetch = client().getCurrentProduction()
        assertTrue(fetch.result is ApiResult.ApiError)
    }

    @Test
    fun nonZeroCode_returnsApiError() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"code":1001,"data":{}}"""))
        val fetch = client().getCurrentProduction()
        assertEquals(ApiResult.ApiError(code = 1001), fetch.result)
    }

    @Test
    fun httpError_returnsApiError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("oops"))
        val fetch = client().getCurrentProduction()
        assertEquals(ApiResult.ApiError(httpStatus = 500), fetch.result)
    }

    @Test
    fun unreachableServer_returnsNetworkError() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        val fetch = client().getCurrentProduction()
        assertEquals(ApiResult.NetworkError, fetch.result)
    }

    @Test
    fun runtimeExceptionDuringCall_returnsNetworkErrorNotCrash() = runBlocking {
        // Reproduces the missing-INTERNET-permission crash: the call threw a SecurityException
        // (a RuntimeException), which must be surfaced as a network error, not propagated.
        val throwingClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor { throw SecurityException("Permission denied (missing INTERNET permission?)") }
            .build()
        val fetch = OkHttpEmaApiClient(settings, httpClient = throwingClient).getCurrentProduction()
        assertEquals(ApiResult.NetworkError, fetch.result)
    }

    @Test
    fun notConfigured_returnsConfigErrorWithoutRequest() = runBlocking {
        settings.setEmaAppId("")
        val fetch = client().getCurrentProduction()
        assertEquals(ApiResult.ConfigurationError, fetch.result)
        assertEquals(0, server.requestCount)
    }
}
