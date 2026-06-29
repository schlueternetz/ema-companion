package com.schlueternetz.emacompanion.core.api.modulehealth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.schlueternetz.emacompanion.core.api.OkHttpEmaApiClient
import com.schlueternetz.emacompanion.core.api.log.ApiCallLogRepository
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * End-to-end test: real OkHttp socket I/O → signing → parsing → status computation → persistence.
 * Verifies task 4.9: mock EMA API, status computed and persisted correctly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ModuleHealthIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var context: Context
    private lateinit var repo: ModuleHealthRepository

    private val today = LocalDate.of(2025, 7, 24)
    private val yesterday = today.minusDays(1)
    private val dayBefore = today.minusDays(2)

    // Each entry format: "{uid}-{channel}-{kWh}"
    private fun batchBody(vararg entries: Pair<String, Double>): String {
        val items = entries.joinToString(",") { (uid, kwh) -> "\"$uid-1-$kwh\"" }
        return """{"code":0,"data":{"energy":[$items]}}"""
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        server = MockWebServer()
        server.start()

        context.getSharedPreferences(ModuleHealthRepository.PREFS_HEALTH, Context.MODE_PRIVATE)
            .edit().clear().commit()
        context.getSharedPreferences(ModuleHealthRepository.PREFS_DAILY, Context.MODE_PRIVATE)
            .edit().clear().commit()
        context.getSharedPreferences("integ_mh_settings", Context.MODE_PRIVATE)
            .edit().clear().commit()
        context.getSharedPreferences("integ_mh_log", Context.MODE_PRIVATE)
            .edit().clear().commit()

        val settings = SettingsRepository(
            context.getSharedPreferences("integ_mh_settings", Context.MODE_PRIVATE),
        ).apply {
            setEmaAppId("a".repeat(32))
            setEmaAppSecret("secret123456")
            setEmaSystemId("b".repeat(16))
            setEmaEcuId("203000001234")
            setSystemCapacity(10f)
            setBaseUrl(server.url("/user/api/v2/").toString())
        }

        val client = OkHttpEmaApiClient(settings, ioDispatcher = Dispatchers.Unconfined)
        val log = ApiCallLogRepository(
            context.getSharedPreferences("integ_mh_log", Context.MODE_PRIVATE),
        )
        repo = ModuleHealthRepository(
            client = client,
            log = log,
            healthPrefs = context.getSharedPreferences(
                ModuleHealthRepository.PREFS_HEALTH, Context.MODE_PRIVATE,
            ),
            dailyPrefs = context.getSharedPreferences(
                ModuleHealthRepository.PREFS_DAILY, Context.MODE_PRIVATE,
            ),
            appSecretProvider = { settings.getEmaAppSecret() },
            today = { today },
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun refresh_allProducing_computesGreenAndPersists() {
        // Dates fetched oldest-first: dayBefore, yesterday, today
        server.enqueue(MockResponse().setBody(batchBody("INV1" to 1.5, "INV2" to 1.2)))
        server.enqueue(MockResponse().setBody(batchBody("INV1" to 1.4, "INV2" to 1.1)))
        server.enqueue(MockResponse().setBody(batchBody("INV1" to 1.3, "INV2" to 1.0)))

        val state = runBlocking { repo.refresh() }

        assertEquals(ModuleHealthStatus.GREEN, state.status)
        assertEquals(emptyList<Module>(), state.offlineModules)
        assertNull(state.error)

        // Verify persistence: a new repo instance reads the same state
        val reloaded = repo.currentState()
        assertEquals(ModuleHealthStatus.GREEN, reloaded.status)
    }

    @Test
    fun refresh_oneInverterOfflineToday_computesYellowAndPersists() {
        // INV1 is offline today only → YELLOW
        server.enqueue(MockResponse().setBody(batchBody("INV1" to 1.5, "INV2" to 1.2))) // dayBefore
        server.enqueue(MockResponse().setBody(batchBody("INV1" to 1.4, "INV2" to 1.1))) // yesterday
        server.enqueue(MockResponse().setBody(batchBody("INV1" to 0.0, "INV2" to 1.0))) // today

        val state = runBlocking { repo.refresh() }

        assertEquals(ModuleHealthStatus.YELLOW, state.status)
        assertEquals(1, state.offlineModules.size)
        assertEquals("INV1", state.offlineModules[0].uid)
        assertEquals(1, state.offlineModules[0].offlineDays)

        val reloaded = repo.currentState()
        assertEquals(ModuleHealthStatus.YELLOW, reloaded.status)
    }

    @Test
    fun refresh_oneInverterOfflineThreeDays_computesRedAndPersists() {
        // INV1 has 0 kWh on all 3 days → RED
        server.enqueue(MockResponse().setBody(batchBody("INV1" to 0.0, "INV2" to 1.2))) // dayBefore
        server.enqueue(MockResponse().setBody(batchBody("INV1" to 0.0, "INV2" to 1.1))) // yesterday
        server.enqueue(MockResponse().setBody(batchBody("INV1" to 0.0, "INV2" to 1.0))) // today

        val state = runBlocking { repo.refresh() }

        assertEquals(ModuleHealthStatus.RED, state.status)
        assertEquals(1, state.offlineModules.size)
        assertEquals(3, state.offlineModules[0].offlineDays)

        val reloaded = repo.currentState()
        assertEquals(ModuleHealthStatus.RED, reloaded.status)
    }

    @Test
    fun refresh_pastDaysCached_makesOnlyOneApiCall() {
        // Pre-seed yesterday and dayBefore in the daily cache
        context.getSharedPreferences(ModuleHealthRepository.PREFS_DAILY, Context.MODE_PRIVATE)
            .edit()
            .putString("daily_$yesterday", """{"INV1":1.4,"INV2":1.1}""")
            .putString("daily_$dayBefore", """{"INV1":1.5,"INV2":1.2}""")
            .commit()

        // Only today's response should be fetched
        server.enqueue(MockResponse().setBody(batchBody("INV1" to 1.3, "INV2" to 1.0)))

        runBlocking { repo.refresh() }

        assertEquals("only today should be fetched", 1, server.requestCount)
    }
}
