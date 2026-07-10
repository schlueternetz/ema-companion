package com.schlueternetz.emacompanion.feature.home

import android.os.Looper
import androidx.fragment.app.testing.launchFragmentInContainer
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.api.DailyEnergySource
import com.schlueternetz.emacompanion.core.api.DailyProductionState
import com.schlueternetz.emacompanion.core.api.HourlyEnergySource
import com.schlueternetz.emacompanion.core.api.HourlyProductionState
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
class HomeWidgetUpdateTest {
    private class FakeProductionSource : ProductionSource {
        override fun currentState() = ProductionState()

        override suspend fun refresh(force: Boolean) = ProductionState()
    }

    private class FakeHourlySource(
        var state: HourlyProductionState,
    ) : HourlyEnergySource {
        override fun currentState() = state

        override suspend fun refresh(force: Boolean): HourlyProductionState = state
    }

    private class FakeDailySource(
        var state: DailyProductionState,
    ) : DailyEnergySource {
        override fun currentState() = state

        override suspend fun refresh(force: Boolean): DailyProductionState = state
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @After
    fun tearDown() {
        HomeFragment.sourceOverride = null
        HomeFragment.hourlySourceOverride = null
        HomeFragment.dailySourceOverride = null
        HomeFragment.widgetUpdateAction = HomeFragment.defaultWidgetUpdateAction
    }

    @Test
    fun onResume_successfulHourlyRefresh_updatesWidgets() {
        var callCount = 0
        HomeFragment.widgetUpdateAction = { callCount++ }
        HomeFragment.sourceOverride = FakeProductionSource()
        HomeFragment.hourlySourceOverride = FakeHourlySource(HourlyProductionState())
        HomeFragment.dailySourceOverride = FakeDailySource(DailyProductionState())

        launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()

        assertEquals(2, callCount) // one for hourly, one for daily
    }

    @Test
    fun pullToRefresh_successfulRefresh_updatesWidgets() {
        var callCount = 0
        HomeFragment.widgetUpdateAction = { callCount++ }
        HomeFragment.sourceOverride = FakeProductionSource()
        HomeFragment.hourlySourceOverride = FakeHourlySource(HourlyProductionState())
        HomeFragment.dailySourceOverride = FakeDailySource(DailyProductionState())

        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        val afterLaunch = callCount

        scenario.onFragment { fragment ->
            val swipe =
                fragment.requireView().findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(
                    R.id.home_swipe_refresh,
                )
            val listenerField =
                androidx.swiperefreshlayout.widget.SwipeRefreshLayout::class.java.getDeclaredField("mListener")
            listenerField.isAccessible = true
            val listener =
                listenerField.get(swipe) as? androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
            listener?.onRefresh()
        }
        idle()

        assertEquals(afterLaunch + 2, callCount) // one more for hourly, one more for daily
    }
}
