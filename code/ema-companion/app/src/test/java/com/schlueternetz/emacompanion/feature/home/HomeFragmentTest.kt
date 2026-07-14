package com.schlueternetz.emacompanion.feature.home

import android.content.Context
import android.os.Looper
import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.HomeTile
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

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            appContext,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
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
        HomeFragment.requestOpportunisticSyncAction = HomeFragment.defaultRequestOpportunisticSyncAction
        HomeFragment.requestForcedSyncAction = HomeFragment.defaultRequestForcedSyncAction
        HomeFragment.observeCompletionAction = HomeFragment.defaultObserveCompletionAction
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

    // ── Delegates to ApiSyncScheduler instead of fetching directly (ADR-010) ───
    //
    // Gating (isXDataNeeded, throttle) now lives entirely in ApiSyncWorker — see
    // ApiSyncWorkerTest for that coverage. HomeFragment's own responsibility is just to ask.

    @Test
    fun onResume_requestsOpportunisticSync() {
        var requestCount = 0
        HomeFragment.requestOpportunisticSyncAction = { requestCount++ }
        launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        assertEquals(1, requestCount)
    }

    @Test
    fun onResume_rendersModuleHealthFromCurrentState_withoutFetching() {
        val moduleHealth = FakeModuleHealthSource()
        HomeFragment.moduleHealthSourceOverride = moduleHealth
        launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        // Module Health has no on-demand fetch of its own — ModuleHealthWorker's daily
        // schedule is the only trigger (ADR-010's always-on-alerting invariant).
        assertEquals(0, moduleHealth.calls)
    }
}
