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
        settings =
            SettingsRepository(prefs).apply {
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
    fun success_returnsLastPowerSample() =
        runBlocking {
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
    fun success_sendsSignedHeaders() =
        runBlocking {
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
    fun emptyPower_returnsZeroProduction() =
        runBlocking {
            // No samples (e.g. night / before dawn) is "not producing", i.e. 0 W — a successful
            // read, not an error: the solar owner must not see a red error status every evening.
            server.enqueue(MockResponse().setBody("""{"code":0,"data":{"power":[]}}"""))
            val fetch = client().getCurrentProduction()
            assertEquals(ApiResult.Success(ProductionSnapshot(0)), fetch.result)
        }

    @Test
    fun missingPowerArray_returnsApiError() =
        runBlocking {
            // code 0 but no power array at all is an unexpected shape, not "0 W" — keep it an error.
            server.enqueue(MockResponse().setBody("""{"code":0,"data":{}}"""))
            val fetch = client().getCurrentProduction()
            assertTrue(fetch.result is ApiResult.ApiError)
        }

    @Test
    fun nonZeroCode_returnsApiError() =
        runBlocking {
            server.enqueue(MockResponse().setBody("""{"code":1001,"data":{}}"""))
            val fetch = client().getCurrentProduction()
            assertEquals(ApiResult.ApiError(code = 1001), fetch.result)
        }

    @Test
    fun httpError_returnsApiError() =
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(500).setBody("oops"))
            val fetch = client().getCurrentProduction()
            assertEquals(ApiResult.ApiError(httpStatus = 500), fetch.result)
        }

    @Test
    fun unreachableServer_returnsNetworkError() =
        runBlocking {
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
            val fetch = client().getCurrentProduction()
            assertEquals(ApiResult.NetworkError, fetch.result)
        }

    @Test
    fun runtimeExceptionDuringCall_returnsNetworkErrorNotCrash() =
        runBlocking {
            // Reproduces the missing-INTERNET-permission crash: the call threw a SecurityException
            // (a RuntimeException), which must be surfaced as a network error, not propagated.
            val throwingClient =
                okhttp3.OkHttpClient
                    .Builder()
                    .addInterceptor { throw SecurityException("Permission denied (missing INTERNET permission?)") }
                    .build()
            val fetch = OkHttpEmaApiClient(settings, httpClient = throwingClient).getCurrentProduction()
            assertEquals(ApiResult.NetworkError, fetch.result)
        }

    @Test
    fun notConfigured_returnsConfigErrorWithoutRequest() =
        runBlocking {
            settings.setEmaAppId("")
            val fetch = client().getCurrentProduction()
            assertEquals(ApiResult.ConfigurationError, fetch.result)
            assertEquals(0, server.requestCount)
        }

    // ── getHourlyEnergy ────────────────────────────────────────────────────

    @Test
    fun getHourlyEnergy_success_parsesNonNullEntries() =
        runBlocking {
            val data =
                buildString {
                    append("[")
                    for (h in 0 until 24) {
                        if (h > 0) append(",")
                        if (h in 6..18) append("\"$h.12\"") else append("null")
                    }
                    append("]")
                }
            server.enqueue(MockResponse().setBody("""{"code":0,"data":$data}"""))
            val fetch = client().getHourlyEnergy("2026-07-01")
            val result = fetch.result as ApiResult.Success
            assertEquals(13, result.data.hours.size)
            assertEquals(6.12, result.data.hours[6]!!, 0.001)
            assertEquals(18.12, result.data.hours[18]!!, 0.001)
        }

    @Test
    fun getHourlyEnergy_success_sendsSignedHeadersAndCorrectParams() =
        runBlocking {
            server.enqueue(MockResponse().setBody("""{"code":0,"data":[]}"""))
            client().getHourlyEnergy("2026-07-01")
            val req = server.takeRequest()
            assertTrue(req.getHeader("X-CA-AppId")!!.isNotEmpty())
            assertEquals("HmacSHA256", req.getHeader("X-CA-Signature-Method"))
            assertEquals("hourly", req.requestUrl!!.queryParameter("energy_level"))
            assertEquals("2026-07-01", req.requestUrl!!.queryParameter("date_range"))
            assertEquals("203000001234", req.requestUrl!!.pathSegments.last())
        }

    @Test
    fun getHourlyEnergy_code1001_treatedAsEmptySuccess() =
        runBlocking {
            // 1001 = "No data" — a day with no hourly production is not an error.
            server.enqueue(MockResponse().setBody("""{"code":1001}"""))
            val fetch = client().getHourlyEnergy("2026-07-01")
            assertEquals(ApiResult.Success(HourlySnapshot(emptyMap())), fetch.result)
        }

    @Test
    fun getHourlyEnergy_apiError_returnsApiError() =
        runBlocking {
            server.enqueue(MockResponse().setBody("""{"code":4000}"""))
            val fetch = client().getHourlyEnergy("2026-07-01")
            assertEquals(ApiResult.ApiError(code = 4000), fetch.result)
        }

    @Test
    fun getHourlyEnergy_httpError_returnsApiError() =
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(500).setBody("oops"))
            val fetch = client().getHourlyEnergy("2026-07-01")
            assertEquals(ApiResult.ApiError(httpStatus = 500), fetch.result)
        }

    @Test
    fun getHourlyEnergy_notConfigured_returnsConfigErrorWithoutRequest() =
        runBlocking {
            settings.setEmaAppId("")
            val fetch = client().getHourlyEnergy("2026-07-01")
            assertEquals(ApiResult.ConfigurationError, fetch.result)
            assertEquals(0, server.requestCount)
        }

    // ── getDailyEnergy ─────────────────────────────────────────────────────

    @Test
    fun getDailyEnergy_singleMonth_parsesAllDays() =
        runBlocking {
            val julyData = (1..31).joinToString(",") { "\"$it.00\"" }
            server.enqueue(MockResponse().setBody("""{"code":0,"data":[$julyData]}"""))
            val fetch = client().getDailyEnergy("2026-07-01", "2026-07-31")
            val result = fetch.result as ApiResult.Success
            assertEquals(31, result.data.days.size)
            assertEquals(1.0, result.data.days["2026-07-01"]!!, 0.001)
            assertEquals(31.0, result.data.days["2026-07-31"]!!, 0.001)
        }

    @Test
    fun getDailyEnergy_dateRangeFilters_excludesDaysOutsideRange() =
        runBlocking {
            val julyData = (1..31).joinToString(",") { "\"$it.00\"" }
            server.enqueue(MockResponse().setBody("""{"code":0,"data":[$julyData]}"""))
            val fetch = client().getDailyEnergy("2026-07-10", "2026-07-20")
            val result = fetch.result as ApiResult.Success
            assertEquals(11, result.data.days.size)
            assertEquals(10.0, result.data.days["2026-07-10"]!!, 0.001)
            assertEquals(20.0, result.data.days["2026-07-20"]!!, 0.001)
            assertEquals(null, result.data.days["2026-07-09"])
            assertEquals(null, result.data.days["2026-07-21"])
        }

    @Test
    fun getDailyEnergy_multiMonth_makesOneCallPerMonth() =
        runBlocking {
            val juneData = (1..30).joinToString(",") { "\"$it.0\"" }
            val julyData = (1..31).joinToString(",") { "\"$it.0\"" }
            server.enqueue(MockResponse().setBody("""{"code":0,"data":[$juneData]}"""))
            server.enqueue(MockResponse().setBody("""{"code":0,"data":[$julyData]}"""))
            val fetch = client().getDailyEnergy("2026-06-25", "2026-07-05")
            val result = fetch.result as ApiResult.Success
            assertEquals(2, server.requestCount)
            assertEquals(11, result.data.days.size) // 6 June days + 5 July days
            assertEquals(25.0, result.data.days["2026-06-25"]!!, 0.001)
            assertEquals(5.0, result.data.days["2026-07-05"]!!, 0.001)
            assertEquals(null, result.data.days["2026-06-24"])
            assertEquals(null, result.data.days["2026-07-06"])
        }

    @Test
    fun getDailyEnergy_success_sendsSignedHeadersAndCorrectParams() =
        runBlocking {
            server.enqueue(MockResponse().setBody("""{"code":0,"data":[]}"""))
            client().getDailyEnergy("2026-07-01", "2026-07-20")
            val req = server.takeRequest()
            assertTrue(req.getHeader("X-CA-AppId")!!.isNotEmpty())
            assertEquals("HmacSHA256", req.getHeader("X-CA-Signature-Method"))
            assertEquals("daily", req.requestUrl!!.queryParameter("energy_level"))
            assertEquals("2026-07", req.requestUrl!!.queryParameter("date_range"))
        }

    @Test
    fun getDailyEnergy_code1001_treatedAsEmptySuccess() =
        runBlocking {
            // 1001 = "No data" — a month with no production (e.g. before installation) is not an
            // error; treat it as zero days so the chart shows nothing rather than an error banner.
            server.enqueue(MockResponse().setBody("""{"code":1001}"""))
            val fetch = client().getDailyEnergy("2026-07-01", "2026-07-31")
            assertEquals(ApiResult.Success(DailySnapshot(emptyMap())), fetch.result)
        }

    @Test
    fun getDailyEnergy_code1001_thenCode0_mergesResults() =
        runBlocking {
            // Multi-month span: June has no data yet, July has real data.
            server.enqueue(MockResponse().setBody("""{"code":1001}"""))
            server.enqueue(MockResponse().setBody("""{"code":0,"data":["5.0","6.0","7.0"]}"""))
            val fetch = client().getDailyEnergy("2026-06-30", "2026-07-03")
            val result = fetch.result as ApiResult.Success
            assertEquals(3, result.data.days.size) // 0 June days + 3 July days
            assertEquals(5.0, result.data.days["2026-07-01"]!!, 0.001)
        }

    @Test
    fun getDailyEnergy_apiError_returnsApiError() =
        runBlocking {
            server.enqueue(MockResponse().setBody("""{"code":4000}"""))
            val fetch = client().getDailyEnergy("2026-07-01", "2026-07-31")
            assertTrue(fetch.result is ApiResult.ApiError)
        }

    @Test
    fun getDailyEnergy_notConfigured_returnsConfigErrorWithoutRequest() =
        runBlocking {
            settings.setEmaAppId("")
            val fetch = client().getDailyEnergy("2026-07-01", "2026-07-31")
            assertEquals(ApiResult.ConfigurationError, fetch.result)
            assertEquals(0, server.requestCount)
        }
}
