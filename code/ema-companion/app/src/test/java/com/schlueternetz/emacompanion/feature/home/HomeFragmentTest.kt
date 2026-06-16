package com.schlueternetz.emacompanion.feature.home

import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.api.ProductionSnapshot
import com.schlueternetz.emacompanion.core.api.ProductionSource
import com.schlueternetz.emacompanion.core.api.ProductionState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HomeFragmentTest {

    private class FakeSource(var state: ProductionState) : ProductionSource {
        override suspend fun refresh(): ProductionState = state
    }

    @After
    fun tearDown() {
        HomeFragment.sourceOverride = null
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun homeFragment_launchesSuccessfully() {
        launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
    }

    @Test
    fun rendersCurrentProduction() {
        HomeSourceFixture.use(ProductionState(snapshot = ProductionSnapshot(8000)))
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val text = fragment.requireView().findViewById<TextView>(R.id.text_current_production)
            assertEquals("Current Production: 8000 W", text.text.toString())
        }
    }

    @Test
    fun showsNeutralPlaceholderWhenNoSnapshot() {
        HomeSourceFixture.use(ProductionState(snapshot = null))
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val text = fragment.requireView().findViewById<TextView>(R.id.text_current_production)
            assertEquals("Current Production: — W", text.text.toString())
        }
    }

    @Test
    fun showsBannerOnNetworkErrorThenClearsOnSuccess() {
        val source = FakeSource(ProductionState(snapshot = ProductionSnapshot(8000), networkError = true))
        HomeFragment.sourceOverride = source
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val banner = fragment.requireView().findViewById<View>(R.id.network_error_banner)
            assertEquals(View.VISIBLE, banner.visibility)
        }
        // A later successful fetch clears the banner.
        source.state = ProductionState(snapshot = ProductionSnapshot(8000), networkError = false)
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        idle()
        scenario.onFragment { fragment ->
            val banner = fragment.requireView().findViewById<View>(R.id.network_error_banner)
            assertEquals(View.GONE, banner.visibility)
        }
    }

    @Test
    fun homeFragment_hasNoAccessibilityErrors() {
        HomeSourceFixture.use(ProductionState(snapshot = ProductionSnapshot(8000)))
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            AccessibilityValidator()
                .setThrowExceptionFor(AccessibilityCheckResult.AccessibilityCheckResultType.ERROR)
                .check(fragment.requireView())
        }
    }

    private object HomeSourceFixture {
        fun use(state: ProductionState) {
            HomeFragment.sourceOverride = object : ProductionSource {
                override suspend fun refresh(): ProductionState = state
            }
        }
    }
}
