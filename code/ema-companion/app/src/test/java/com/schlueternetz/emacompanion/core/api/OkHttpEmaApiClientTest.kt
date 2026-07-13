package com.schlueternetz.emacompanion.core.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
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
