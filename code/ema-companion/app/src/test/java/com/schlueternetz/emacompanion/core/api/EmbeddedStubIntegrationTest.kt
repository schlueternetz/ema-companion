package com.schlueternetz.emacompanion.core.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.schlueternetz.emaapistub.MatchingEngine
import com.schlueternetz.emaapistub.ScenarioLoader
import com.schlueternetz.emaapistub.stubModule
import com.schlueternetz.emacompanion.core.api.log.ApiCallLogRepository
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Drives [OkHttpEmaApiClient] / [DailyEnergyRepository] over a real socket against the actual
 * `ema-api-stub` MatchingEngine (embedded in-process on an ephemeral port, not a real deployed
 * server) — so these tests exercise the same request matching/sequencing the stub itself uses,
 * not a hand-typed mock body.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EmbeddedStubIntegrationTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val ecuId = "203000001234"
    private var server: EmbeddedServer<*, *>? = null

    @After
    fun tearDown() {
        server?.stop(0, 0)
    }

    private fun startStub(json: String): Int {
        tempFolder.newFile("scenario.json").writeText(json)
        val engine = MatchingEngine(ScenarioLoader.loadFromDirectory(tempFolder.root))
        val embedded = embeddedServer(CIO, port = 0) { stubModule(engine) }
        server = embedded
        embedded.start(wait = false)
        return runBlocking {
            embedded.engine
                .resolvedConnectors()
                .first()
                .port
        }
    }

    private fun settingsFor(
        port: Int,
        context: Context = ApplicationProvider.getApplicationContext(),
    ) = SettingsRepository(
        context.getSharedPreferences("embedded_stub_settings_${System.nanoTime()}", Context.MODE_PRIVATE),
    ).apply {
        setEmaAppId("a".repeat(32))
        setEmaAppSecret("secret123456")
        setEmaSystemId("b".repeat(16))
        setEmaEcuId(ecuId)
        setSystemCapacity(10f)
        setBaseUrl("http://localhost:$port/user/api/v2/")
    }

    private fun clientFor(settings: SettingsRepository) = OkHttpEmaApiClient(settings)

    @Test
    fun hourlyEnergy_matchesEmbeddedStubResponse() =
        runBlocking {
            val port =
                startStub(
                    """
                    {
                      "ecuId": "$ecuId",
                      "description": "Hourly-only embedded-stub fixture",
                      "interactions": [
                        {
                          "request": {
                            "method": "GET",
                            "path": "/user/api/v2/systems/{sid}/devices/ecu/energy/{eid}",
                            "pathParams": { "eid": "$ecuId" },
                            "query": { "energy_level": "hourly" }
                          },
                          "response": { "body": { "code": 0, "data": [null, null, "1.50", "2.50", null] } }
                        }
                      ]
                    }
                    """.trimIndent(),
                )

            val client = clientFor(settingsFor(port))
            val fetch = client.getHourlyEnergy("2025-07-20")

            val result = fetch.result
            check(result is ApiResult.Success)
            assertEquals(mapOf(2 to 1.50, 3 to 2.50), result.data.hours)
        }

    @Test
    fun dailyEnergyRepository_mergesTwoCalendarMonthsIntoOneSnapshot() =
        runBlocking {
            val mayDays = (1..31).map { "\"5.${it.toString().padStart(2, '0')}\"" }.joinToString(",")
            val juneDays = (1..30).map { "\"6.${it.toString().padStart(2, '0')}\"" }.joinToString(",")
            val port =
                startStub(
                    """
                    {
                      "ecuId": "$ecuId",
                      "description": "Two-calendar-month daily embedded-stub fixture (May then June)",
                      "interactions": [
                        {
                          "request": {
                            "method": "GET",
                            "path": "/user/api/v2/systems/{sid}/devices/ecu/energy/{eid}",
                            "pathParams": { "eid": "$ecuId" },
                            "query": { "energy_level": "daily" }
                          },
                          "response": { "body": { "code": 0, "data": [$mayDays] } }
                        },
                        {
                          "request": {
                            "method": "GET",
                            "path": "/user/api/v2/systems/{sid}/devices/ecu/energy/{eid}",
                            "pathParams": { "eid": "$ecuId" },
                            "query": { "energy_level": "daily" }
                          },
                          "response": { "body": { "code": 0, "data": [$juneDays] } }
                        }
                      ]
                    }
                    """.trimIndent(),
                )

            val context = ApplicationProvider.getApplicationContext<Context>()
            val settings = settingsFor(port, context)
            val client = clientFor(settings)
            val prefs = context.getSharedPreferences("embedded_stub_daily_${System.nanoTime()}", Context.MODE_PRIVATE)
            val repo =
                DailyEnergyRepository(
                    client = client,
                    usageCounter = ApiUsageRepository.create(context),
                    log = ApiCallLogRepository.create(context),
                    appSecretProvider = { settings.getEmaAppSecret() },
                    today = { "2025-06-05" },
                    historyDays = { 20 },
                    prefs = prefs,
                )

            val state = repo.refresh(force = true)

            val days = state.snapshot!!.days
            assertEquals(21, days.size) // 2025-05-16..2025-06-05 inclusive
            assertEquals(5.16, days.getValue("2025-05-16"), 0.0)
            assertEquals(6.05, days.getValue("2025-06-05"), 0.0)
        }

    @Test
    fun noDataFixture_returnsEmptySuccessForBothHourlyAndDaily() =
        runBlocking {
            val port =
                startStub(
                    """
                    {
                      "ecuId": "$ecuId",
                      "description": "No-data (code 1001) embedded-stub fixture for hourly then daily",
                      "interactions": [
                        {
                          "request": {
                            "method": "GET",
                            "path": "/user/api/v2/systems/{sid}/devices/ecu/energy/{eid}",
                            "pathParams": { "eid": "$ecuId" },
                            "query": { "energy_level": "hourly" }
                          },
                          "response": { "body": { "code": 1001 } }
                        },
                        {
                          "request": {
                            "method": "GET",
                            "path": "/user/api/v2/systems/{sid}/devices/ecu/energy/{eid}",
                            "pathParams": { "eid": "$ecuId" },
                            "query": { "energy_level": "daily" }
                          },
                          "response": { "body": { "code": 1001 } }
                        }
                      ]
                    }
                    """.trimIndent(),
                )

            val client = clientFor(settingsFor(port))

            val hourly = client.getHourlyEnergy("2025-07-20").result
            val daily = client.getDailyEnergy("2025-07-01", "2025-07-01").result

            check(hourly is ApiResult.Success)
            check(daily is ApiResult.Success)
            assertEquals(emptyMap<Int, Double>(), hourly.data.hours)
            assertEquals(emptyMap<String, Double>(), daily.data.days)
        }
}
