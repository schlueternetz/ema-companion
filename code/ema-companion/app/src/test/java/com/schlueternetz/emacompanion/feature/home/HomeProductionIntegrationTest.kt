package com.schlueternetz.emacompanion.feature.home

import android.content.Context
import android.os.Looper
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import com.schlueternetz.emaapistub.Scenario
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.api.ApiUsageRepository
import com.schlueternetz.emacompanion.core.api.OkHttpEmaApiClient
import com.schlueternetz.emacompanion.core.api.ProductionRepository
import com.schlueternetz.emacompanion.core.api.log.ApiCallLogRepository
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Drives the real HTTP path — real socket I/O + signing + parsing + rendering — through
 * `HomeFragment`, seeded with the canonical body from the stub's `203000001234.json`
 * "Good Data" scenario (the stub scenario is the source of truth for this fixture).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HomeProductionIntegrationTest {
    // Read straight from the stub's own bundled scenario file, so this fixture cannot
    // silently drift from what the real stub actually serves. Read as a classpath resource
    // (not ScenarioLoader.loadDefault(), which requires an exploded directory and cannot list
    // a packaged jar's contents) since the composite-build dependency supplies a jar here.
    private val canonicalBody =
        javaClass.classLoader!!
            .getResourceAsStream("scenarios/203000001234.json")!!
            .bufferedReader()
            .readText()
            .let { Json { ignoreUnknownKeys = true }.decodeFromString<Scenario>(it) }
            .interactions
            .first()
            .response.body
            .toString()

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
        HomeFragment.sourceOverride = null
    }

    @Test
    fun homeDisplaysProductionFromRealHttp() {
        server.enqueue(MockResponse().setBody(canonicalBody))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings =
            SettingsRepository(
                context.getSharedPreferences("integ_settings", Context.MODE_PRIVATE).also { it.edit().clear().apply() },
            ).apply {
                setEmaAppId("a".repeat(32))
                setEmaAppSecret("secret123456")
                setEmaSystemId("b".repeat(16))
                setEmaEcuId("203000001234")
                setSystemCapacity(10f)
                setBaseUrl(server.url("/user/api/v2/").toString())
            }
        val usage =
            ApiUsageRepository(
                context.getSharedPreferences("integ_usage", Context.MODE_PRIVATE).also { it.edit().clear().apply() },
            )
        val log =
            ApiCallLogRepository(
                context.getSharedPreferences("integ_log", Context.MODE_PRIVATE).also { it.edit().clear().apply() },
            )
        // Unconfined IO dispatcher → the socket call runs inline so the test is deterministic.
        val client = OkHttpEmaApiClient(settings, ioDispatcher = Dispatchers.Unconfined)
        HomeFragment.sourceOverride =
            ProductionRepository(
                client = client,
                usage = usage,
                log = log,
                appSecretProvider = { settings.getEmaAppSecret() },
            )

        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        shadowOf(Looper.getMainLooper()).idle()

        scenario.onFragment { fragment ->
            val text = fragment.requireView().findViewById<TextView>(R.id.text_current_production)
            assertEquals("8000 W", text.text.toString())
        }
        assertEquals(1, usage.getRequestCount())
        assertEquals(1, log.getAll().size)
    }
}
