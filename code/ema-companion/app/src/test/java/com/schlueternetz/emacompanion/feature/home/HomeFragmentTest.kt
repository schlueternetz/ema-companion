package com.schlueternetz.emacompanion.feature.home

import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.api.FetchError
import com.schlueternetz.emacompanion.core.api.ProductionSnapshot
import com.schlueternetz.emacompanion.core.api.ProductionSource
import com.schlueternetz.emacompanion.core.api.ProductionState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HomeFragmentTest {

    private class FakeSource(var state: ProductionState) : ProductionSource {
        override fun currentState(): ProductionState = state
        override suspend fun refresh(): ProductionState = state
    }

    @After
    fun tearDown() {
        HomeFragment.sourceOverride = null
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun use(state: ProductionState) {
        HomeFragment.sourceOverride = FakeSource(state)
    }

    @Test
    fun homeFragment_launchesSuccessfully() {
        launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
    }

    @Test
    fun rendersCurrentProductionValue() {
        use(ProductionState(snapshot = ProductionSnapshot(8000), updatedAtEpochMs = 1_700_000_000_000))
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val value = fragment.requireView().findViewById<TextView>(R.id.text_current_production)
            assertEquals("8000 W", value.text.toString())
        }
    }

    @Test
    fun showsNeutralValueWhenNoSnapshot() {
        use(ProductionState(snapshot = null))
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val value = fragment.requireView().findViewById<TextView>(R.id.text_current_production)
            assertEquals("— W", value.text.toString())
            // No successful fetch yet → no "Updated …" line.
            val updated = fragment.requireView().findViewById<View>(R.id.production_updated)
            assertEquals(View.GONE, updated.visibility)
        }
    }

    @Test
    fun showsUpdatedTimestamp_whenValuePresent() {
        use(ProductionState(snapshot = ProductionSnapshot(8000), updatedAtEpochMs = 1_700_000_000_000))
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val updated = fragment.requireView().findViewById<TextView>(R.id.production_updated)
            assertEquals(View.VISIBLE, updated.visibility)
            assertTrue(updated.text.toString().startsWith("Updated"))
        }
    }

    @Test
    fun showsNetworkStatusOnTile_withoutTap_thenClearsOnSuccess() {
        val source = FakeSource(ProductionState(snapshot = ProductionSnapshot(8000), error = FetchError.NETWORK))
        HomeFragment.sourceOverride = source
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val status = fragment.requireView().findViewById<TextView>(R.id.production_status)
            // Visible immediately on the tile — no interaction required.
            assertEquals(View.VISIBLE, status.visibility)
            assertEquals(
                fragment.getString(R.string.home_status_network_error),
                status.text.toString(),
            )
        }
        source.state = ProductionState(snapshot = ProductionSnapshot(8000), updatedAtEpochMs = 1L, error = null)
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        idle()
        scenario.onFragment { fragment ->
            val status = fragment.requireView().findViewById<View>(R.id.production_status)
            assertEquals(View.GONE, status.visibility)
        }
    }

    @Test
    fun showsApiErrorStatus_keepingLastValue() {
        use(ProductionState(snapshot = ProductionSnapshot(8000), updatedAtEpochMs = 1L, error = FetchError.API))
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val status = fragment.requireView().findViewById<TextView>(R.id.production_status)
            assertEquals(View.VISIBLE, status.visibility)
            assertEquals(
                fragment.getString(R.string.home_status_api_error),
                status.text.toString(),
            )
            val value = fragment.requireView().findViewById<TextView>(R.id.text_current_production)
            assertEquals("8000 W", value.text.toString())
        }
    }

    @Test
    fun showsAuthStatus_whenCredentialsRejected() {
        use(ProductionState(snapshot = ProductionSnapshot(8000), updatedAtEpochMs = 1L, error = FetchError.AUTH))
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val status = fragment.requireView().findViewById<TextView>(R.id.production_status)
            assertEquals(View.VISIBLE, status.visibility)
            assertEquals(
                fragment.getString(R.string.home_status_auth_error),
                status.text.toString(),
            )
        }
    }

    @Test
    fun homeFragment_hasNoAccessibilityErrors() {
        use(ProductionState(snapshot = ProductionSnapshot(8000), updatedAtEpochMs = 1L, error = FetchError.API))
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            AccessibilityValidator()
                .setThrowExceptionFor(AccessibilityCheckResult.AccessibilityCheckResultType.ERROR)
                .check(fragment.requireView())
        }
    }
}
