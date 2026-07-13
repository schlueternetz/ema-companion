package com.schlueternetz.emacompanion.feature.home

import android.content.Context
import android.os.Looper
import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.HomeTile
import com.schlueternetz.emacompanion.core.HomeWidget
import com.schlueternetz.emacompanion.core.api.ApiResult
import com.schlueternetz.emacompanion.core.api.ApiUsageRepository
import com.schlueternetz.emacompanion.core.api.BatchEnergyFetch
import com.schlueternetz.emacompanion.core.api.DailyEnergyFetch
import com.schlueternetz.emacompanion.core.api.DailyEnergySource
import com.schlueternetz.emacompanion.core.api.DailyProductionState
import com.schlueternetz.emacompanion.core.api.DailySnapshot
import com.schlueternetz.emacompanion.core.api.EmaApiClient
import com.schlueternetz.emacompanion.core.api.HourlyEnergyFetch
import com.schlueternetz.emacompanion.core.api.HourlyEnergyRepository
import com.schlueternetz.emacompanion.core.api.HourlyEnergySource
import com.schlueternetz.emacompanion.core.api.HourlyProductionState
import com.schlueternetz.emacompanion.core.api.HourlySnapshot
import com.schlueternetz.emacompanion.core.api.log.ApiCallLogRepository
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthSource
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HomeFragmentTest {
    private lateinit var appContext: Context

    private class FakeHourlySource(
        var state: HourlyProductionState = HourlyProductionState(),
    ) : HourlyEnergySource {
        var calls = 0

        override fun currentState() = state

        override suspend fun refresh(force: Boolean): HourlyProductionState {
            calls++
            return state
        }
    }

    private class FakeDailySource(
        var state: DailyProductionState = DailyProductionState(),
    ) : DailyEnergySource {
        var calls = 0

        override fun currentState() = state

        override suspend fun refresh(force: Boolean): DailyProductionState {
            calls++
            return state
        }
    }

    /** Always-succeeds fake at the [EmaApiClient] level, so real repository throttle logic runs. */
    private class FakeEmaApiClient : EmaApiClient {
        var hourlyCalls = 0

        override suspend fun getBatchInverterEnergy(date: String) = BatchEnergyFetch(ApiResult.ConfigurationError)

        override suspend fun getHourlyEnergy(date: String): HourlyEnergyFetch {
            hourlyCalls++
            return HourlyEnergyFetch(ApiResult.Success(HourlySnapshot(mapOf(6 to 1.0))), "/x", 1L, "req", "{}")
        }

        override suspend fun getDailyEnergy(
            startDate: String,
            endDate: String,
        ) = DailyEnergyFetch(ApiResult.Success(DailySnapshot(emptyMap())))
    }

    private class FakeModuleHealthSource(
        var state: ModuleHealthState = ModuleHealthState(),
    ) : ModuleHealthSource {
        var calls = 0

        override fun currentState() = state

        override suspend fun refresh(): ModuleHealthState {
            calls++
            return state
        }
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun setFlags(vararg pairs: Pair<String, Boolean>) {
        val edit =
            appContext
                .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
                .edit()
        pairs.forEach { (key, value) -> edit.putBoolean(key, value) }
        edit.apply()
    }

    private fun tileKey(tile: HomeTile) = "tileEnabled_${tile.name}"

    private fun widgetKey(widget: HomeWidget) = "widgetEnabled_${widget.name}"

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    @After
    fun tearDown() {
        HomeFragment.moduleHealthSourceOverride = null
        HomeFragment.hourlySourceOverride = null
        HomeFragment.dailySourceOverride = null
        HomeFragment.widgetUpdateAction = HomeFragment.defaultWidgetUpdateAction
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    @Test
    fun homeFragment_launchesSuccessfully() {
        launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
    }

    @Test
    fun homeFragment_hasNoAccessibilityErrors() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            AccessibilityValidator()
                .setThrowExceptionFor(AccessibilityCheckResult.AccessibilityCheckResultType.ERROR)
                .check(fragment.requireView())
        }
    }

    // ── Tile visibility ─────────────────────────────────────────────────────

    @Test
    fun disabledTile_isGoneAfterOnViewCreated() {
        setFlags(tileKey(HomeTile.HISTORY_PRODUCTION) to false)
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val history = fragment.requireView().findViewById<View>(R.id.tile_history_production)
            val today = fragment.requireView().findViewById<View>(R.id.tile_today_production)
            val moduleHealth = fragment.requireView().findViewById<View>(R.id.tile_module_health)
            assertEquals(View.GONE, history.visibility)
            assertEquals(View.VISIBLE, today.visibility)
            assertEquals(View.VISIBLE, moduleHealth.visibility)
        }
    }

    @Test
    fun reEnablingTile_becomesVisibleOnNextOnResume() {
        setFlags(tileKey(HomeTile.MODULE_HEALTH) to false)
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val moduleHealth = fragment.requireView().findViewById<View>(R.id.tile_module_health)
            assertEquals(View.GONE, moduleHealth.visibility)
        }

        setFlags(tileKey(HomeTile.MODULE_HEALTH) to true)
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        idle()

        scenario.onFragment { fragment ->
            val moduleHealth = fragment.requireView().findViewById<View>(R.id.tile_module_health)
            assertEquals(View.VISIBLE, moduleHealth.visibility)
        }
    }

    // ── Gated fetches ────────────────────────────────────────────────────────

    @Test
    fun onResume_skipsHourlyRefresh_whenNoEnabledConsumer() {
        setFlags(
            tileKey(HomeTile.TODAY_PRODUCTION) to false,
            widgetKey(HomeWidget.TODAY_PRODUCTION) to false,
            widgetKey(HomeWidget.PRODUCTION_SUMMARY) to false,
        )
        val hourly = FakeHourlySource()
        HomeFragment.hourlySourceOverride = hourly
        launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        assertEquals(0, hourly.calls)
    }

    @Test
    fun onResume_callsHourlyRefresh_whenTileEnabled() {
        val hourly = FakeHourlySource()
        HomeFragment.hourlySourceOverride = hourly
        launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        assertEquals(1, hourly.calls)
    }

    @Test
    fun onResume_skipsDailyRefresh_whenNoEnabledConsumer() {
        setFlags(
            tileKey(HomeTile.TODAY_PRODUCTION) to false,
            tileKey(HomeTile.HISTORY_PRODUCTION) to false,
            widgetKey(HomeWidget.PRODUCTION_SUMMARY) to false,
            widgetKey(HomeWidget.PRODUCTION_HISTORY) to false,
        )
        val daily = FakeDailySource()
        HomeFragment.dailySourceOverride = daily
        launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        assertEquals(0, daily.calls)
    }

    @Test
    fun onResume_keepsDailyRefreshing_whenOnlyTodayProductionTileEnabled() {
        // Today Production's best-day cards consume daily data even with History off.
        setFlags(
            tileKey(HomeTile.HISTORY_PRODUCTION) to false,
            widgetKey(HomeWidget.PRODUCTION_SUMMARY) to false,
            widgetKey(HomeWidget.PRODUCTION_HISTORY) to false,
        )
        val daily = FakeDailySource()
        HomeFragment.dailySourceOverride = daily
        launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        assertEquals(1, daily.calls)
    }

    @Test
    fun onResume_skipsModuleHealthRefresh_whenTileDisabled() {
        setFlags(tileKey(HomeTile.MODULE_HEALTH) to false)
        val moduleHealth = FakeModuleHealthSource()
        HomeFragment.moduleHealthSourceOverride = moduleHealth
        launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        assertEquals(0, moduleHealth.calls)
    }

    @Test
    fun onResume_callsModuleHealthRefresh_whenTileEnabled() {
        val moduleHealth = FakeModuleHealthSource()
        HomeFragment.moduleHealthSourceOverride = moduleHealth
        launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        assertEquals(1, moduleHealth.calls)
    }

    // ── Regression: design.md Decision 7 — re-enabling never adds an extra wait ────

    private fun realHourlyRepo(client: FakeEmaApiClient): HourlyEnergyRepository {
        val prefs =
            appContext
                .getSharedPreferences("home_fragment_test_hourly", Context.MODE_PRIVATE)
                .also { it.edit().clear().apply() }
        return HourlyEnergyRepository(
            client = client,
            usageCounter = ApiUsageRepository.create(appContext),
            log = ApiCallLogRepository.create(appContext),
            appSecretProvider = { "secret123456" },
            prefs = prefs,
        )
    }

    @Test
    fun reEnablingTile_withElapsedThrottle_fetchesImmediately() {
        setFlags(
            tileKey(HomeTile.TODAY_PRODUCTION) to false,
            widgetKey(HomeWidget.TODAY_PRODUCTION) to false,
            widgetKey(HomeWidget.PRODUCTION_SUMMARY) to false,
        )
        val client = FakeEmaApiClient()
        val repo = realHourlyRepo(client)
        // Seed a throttle timestamp well past the 1-hour window, as if it fetched once
        // before being disabled.
        appContext
            .getSharedPreferences("home_fragment_test_hourly", Context.MODE_PRIVATE)
            .edit()
            .putLong("lastFetchMs", System.currentTimeMillis() - 2 * 3_600_000L)
            .apply()
        HomeFragment.hourlySourceOverride = repo

        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        assertEquals(0, client.hourlyCalls) // disabled: gated out, no fetch attempted

        setFlags(tileKey(HomeTile.TODAY_PRODUCTION) to true)
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        idle()

        // Overdue throttle fires on the very next gated refresh — no additional wait imposed
        // by having been disabled.
        assertEquals(1, client.hourlyCalls)
    }

    @Test
    fun reEnablingTile_withinThrottleWindow_doesNotFetch() {
        setFlags(
            tileKey(HomeTile.TODAY_PRODUCTION) to false,
            widgetKey(HomeWidget.TODAY_PRODUCTION) to false,
            widgetKey(HomeWidget.PRODUCTION_SUMMARY) to false,
        )
        val client = FakeEmaApiClient()
        val repo = realHourlyRepo(client)
        // Seed a throttle timestamp well within the 1-hour window.
        appContext
            .getSharedPreferences("home_fragment_test_hourly", Context.MODE_PRIVATE)
            .edit()
            .putLong("lastFetchMs", System.currentTimeMillis() - 60_000L)
            .apply()
        HomeFragment.hourlySourceOverride = repo

        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()

        setFlags(tileKey(HomeTile.TODAY_PRODUCTION) to true)
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        idle()

        assertEquals(0, client.hourlyCalls) // throttle window hasn't elapsed: no new request
    }
}
