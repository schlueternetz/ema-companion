package com.schlueternetz.emacompanion.feature.home

import android.content.Context
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.api.FetchError
import com.schlueternetz.emacompanion.core.api.modulehealth.Module
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthSource
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthState
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ModuleHealthTileTest {
    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            ApplicationProvider.getApplicationContext<Context>(),
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
    }

    @After
    fun tearDown() {
        HomeFragment.moduleHealthSourceOverride = null
    }

    private fun launchWithState(state: ModuleHealthState): View {
        HomeFragment.moduleHealthSourceOverride =
            object : ModuleHealthSource {
                override fun currentState() = state

                override suspend fun refresh() = state
            }
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        var fragmentView: View? = null
        scenario.onFragment { fragment ->
            fragmentView = fragment.requireView()
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        return fragmentView!!
    }

    @Test
    fun greenState_showsGreenStatus() {
        val view = launchWithState(ModuleHealthState(ModuleHealthStatus.GREEN))
        val statusView = view.findViewById<TextView>(R.id.module_health_status)
        assertEquals(View.VISIBLE, statusView.visibility)
        assert(statusView.text.contains("All modules", ignoreCase = true)) {
            "Expected green status text, got: ${statusView.text}"
        }
    }

    @Test
    fun greenState_tileIsNotClickable() {
        val view = launchWithState(ModuleHealthState(ModuleHealthStatus.GREEN))
        val tile = view.findViewById<View>(R.id.tile_module_health)
        assertEquals(false, tile.isClickable)
    }

    @Test
    fun yellowState_showsYellowStatus() {
        val state =
            ModuleHealthState(
                status = ModuleHealthStatus.YELLOW,
                offlineModules = listOf(Module("INV1", 1)),
            )
        val view = launchWithState(state)
        val statusView = view.findViewById<TextView>(R.id.module_health_status)
        assert(statusView.text.contains("offline", ignoreCase = true)) {
            "Expected offline status text, got: ${statusView.text}"
        }
    }

    @Test
    fun yellowState_tileIsClickable() {
        val state =
            ModuleHealthState(
                status = ModuleHealthStatus.YELLOW,
                offlineModules = listOf(Module("INV1", 1)),
            )
        val view = launchWithState(state)
        val tile = view.findViewById<View>(R.id.tile_module_health)
        assertEquals(true, tile.isClickable)
    }

    @Test
    fun yellowState_tapShowsDetailDialog() {
        val state =
            ModuleHealthState(
                status = ModuleHealthStatus.YELLOW,
                offlineModules = listOf(Module("INV1", 2)),
            )
        val view = launchWithState(state)
        val tile = view.findViewById<View>(R.id.tile_module_health)
        tile.performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val dialog = ShadowDialog.getLatestDialog()
        assert(dialog != null && dialog.isShowing) { "Expected dialog to be showing" }
    }

    @Test
    fun checkedTimestamp_showsCheckedLine() {
        val state =
            ModuleHealthState(
                status = ModuleHealthStatus.GREEN,
                checkedAtEpochMs = System.currentTimeMillis(),
            )
        val view = launchWithState(state)
        val checkedView = view.findViewById<TextView>(R.id.module_health_checked)
        assertEquals(View.VISIBLE, checkedView.visibility)
    }

    @Test
    fun unknownState_checkedLineIsGone() {
        val view = launchWithState(ModuleHealthState(ModuleHealthStatus.UNKNOWN))
        val checkedView = view.findViewById<TextView>(R.id.module_health_checked)
        assertEquals(View.GONE, checkedView.visibility)
    }

    @Test
    fun redState_showsRedStatus() {
        val state =
            ModuleHealthState(
                status = ModuleHealthStatus.RED,
                offlineModules = listOf(Module("INV1", 3)),
            )
        val view = launchWithState(state)
        val statusView = view.findViewById<TextView>(R.id.module_health_status)
        assert(statusView.text.contains("action needed", ignoreCase = true)) {
            "Expected red status text, got: ${statusView.text}"
        }
    }

    @Test
    fun networkError_showsErrorLine() {
        val state =
            ModuleHealthState(
                status = ModuleHealthStatus.UNKNOWN,
                error = FetchError.NETWORK,
            )
        val view = launchWithState(state)
        val errorView = view.findViewById<TextView>(R.id.module_health_error)
        assertEquals(View.VISIBLE, errorView.visibility)
        assert(errorView.text.isNotEmpty()) { "error line should have non-empty text" }
    }

    @Test
    fun noError_errorLineIsGone() {
        val view = launchWithState(ModuleHealthState(ModuleHealthStatus.GREEN))
        val errorView = view.findViewById<TextView>(R.id.module_health_error)
        assertEquals(View.GONE, errorView.visibility)
    }

    @Test
    fun fetchError_statusLabelIsEmpty() {
        // When a fetch error occurs the label next to the icon should be blank —
        // the error line below provides the detail.
        val state =
            ModuleHealthState(
                status = ModuleHealthStatus.GREEN,
                error = FetchError.NETWORK,
            )
        val view = launchWithState(state)
        val statusView = view.findViewById<TextView>(R.id.module_health_status)
        assertEquals("status label should be empty on fetch error", "", statusView.text.toString())
    }

    @Test
    fun fetchError_iconHasContentDescription() {
        // Accessibility: the question-mark icon must have a content description so
        // screen readers can convey the unknown state when the label is empty.
        val state =
            ModuleHealthState(
                status = ModuleHealthStatus.GREEN,
                error = FetchError.API,
            )
        val view = launchWithState(state)
        val iconView = view.findViewById<android.widget.ImageView>(R.id.module_health_icon)
        assert(!iconView.contentDescription.isNullOrEmpty()) {
            "icon must have a content description when label is hidden due to fetch error"
        }
    }
}
