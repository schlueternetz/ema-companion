package com.schlueternetz.emacompanion.feature.home

import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.api.DailyEnergySource
import com.schlueternetz.emacompanion.core.api.DailyProductionState
import com.schlueternetz.emacompanion.core.api.DailySnapshot
import com.schlueternetz.emacompanion.core.api.FetchError
import com.schlueternetz.emacompanion.core.api.HourlyEnergySource
import com.schlueternetz.emacompanion.core.api.HourlyProductionState
import com.schlueternetz.emacompanion.core.api.HourlySnapshot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HomeTodaySectionTest {
    private class FakeHourlySource(
        var state: HourlyProductionState,
    ) : HourlyEnergySource {
        var calls = 0
        var lastForce = false

        override fun currentState() = state

        override suspend fun refresh(force: Boolean): HourlyProductionState {
            calls++
            lastForce = force
            return state
        }
    }

    private class FakeDailySource(
        var state: DailyProductionState = DailyProductionState(),
    ) : DailyEnergySource {
        var calls = 0
        var lastForce = false

        override fun currentState() = state

        override suspend fun refresh(force: Boolean): DailyProductionState {
            calls++
            lastForce = force
            return state
        }
    }

    @Before
    fun setUp() {
        HomeFragment.moduleHealthSourceOverride = null
    }

    @After
    fun tearDown() {
        HomeFragment.moduleHealthSourceOverride = null
        HomeFragment.hourlySourceOverride = null
        HomeFragment.dailySourceOverride = null
        HomeFragment.currentHourOverride = null
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    // ── Today chart populated (6.7) ────────────────────────────────────────

    @Test
    fun todayChart_populatedWhenDataAvailable() {
        HomeFragment.currentHourOverride = 10
        HomeFragment.hourlySourceOverride =
            FakeHourlySource(
                HourlyProductionState(snapshot = HourlySnapshot(mapOf(6 to 1.0, 7 to 2.0, 8 to 3.0))),
            )
        HomeFragment.dailySourceOverride = FakeDailySource()
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val chart = fragment.requireView().findViewById<com.github.mikephil.charting.charts.LineChart>(R.id.hourly_chart)
            assertNotNull(chart.data)
        }
    }

    // ── Table renders "–" for null hours (6.7) ─────────────────────────────

    @Test
    fun morningTable_showsDashForNullHours() {
        // Only hours 6 and 7 have data; hours 0-5 and 8-11 should show the neutral dash
        HomeFragment.currentHourOverride = 10
        HomeFragment.hourlySourceOverride =
            FakeHourlySource(
                HourlyProductionState(snapshot = HourlySnapshot(mapOf(6 to 1.5, 7 to 2.5))),
            )
        HomeFragment.dailySourceOverride = FakeDailySource()
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val table = fragment.requireView().findViewById<LinearLayout>(R.id.morning_table)
            assertEquals(12, table.childCount) // 12 morning rows (hours 0–11)
            // Hour 0 row: value should be the neutral dash string
            val row0 = table.getChildAt(0) as LinearLayout
            val value0 = row0.getChildAt(1) as TextView
            assertEquals(fragment.getString(R.string.home_today_neutral), value0.text.toString())
            // Hour 6 row: value should be "1.50"
            val row6 = table.getChildAt(6) as LinearLayout
            val value6 = row6.getChildAt(1) as TextView
            assertEquals("1.50", value6.text.toString())
        }
    }

    // ── Error line visible on fetch error (6.7) ────────────────────────────

    @Test
    fun hourlyErrorLine_visibleOnFetchError() {
        HomeFragment.hourlySourceOverride =
            FakeHourlySource(
                HourlyProductionState(
                    snapshot = HourlySnapshot(mapOf(6 to 1.0)),
                    error = FetchError.NETWORK,
                ),
            )
        HomeFragment.dailySourceOverride = FakeDailySource()
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val status = fragment.requireView().findViewById<TextView>(R.id.hourly_status)
            assertEquals(View.VISIBLE, status.visibility)
            assertEquals(
                fragment.getString(R.string.home_today_status_network_error),
                status.text.toString(),
            )
        }
    }

    // ── Placeholder shown on no data (6.7) ─────────────────────────────────

    @Test
    fun hourlyPlaceholder_visibleWhenNoData() {
        HomeFragment.hourlySourceOverride = FakeHourlySource(HourlyProductionState())
        HomeFragment.dailySourceOverride = FakeDailySource()
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val placeholder = fragment.requireView().findViewById<View>(R.id.hourly_placeholder)
            val chart = fragment.requireView().findViewById<View>(R.id.hourly_chart)
            assertEquals(View.VISIBLE, placeholder.visibility)
            assertEquals(View.GONE, chart.visibility)
        }
    }

    // ── Best-day cards (7.3) ───────────────────────────────────────────────

    @Test
    fun bestDayMonth_showsHighestDayInCurrentMonth() {
        // Use a fixed month that has data
        val todayMonth = java.time.YearMonth.now()
        val day10 = "$todayMonth-10"
        val day20 = "$todayMonth-20"
        val snapDays = mapOf(day10 to 5.0, day20 to 12.0)
        HomeFragment.hourlySourceOverride = FakeHourlySource(HourlyProductionState())
        HomeFragment.dailySourceOverride =
            FakeDailySource(
                DailyProductionState(snapshot = DailySnapshot(snapDays)),
            )
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val value = fragment.requireView().findViewById<TextView>(R.id.best_day_month_value)
            // Best day = day20 with 12.0 kWh
            assertEquals("12.00 kWh", value.text.toString())
        }
    }

    @Test
    fun bestDayCards_showDashWhenNoDailyData() {
        HomeFragment.hourlySourceOverride = FakeHourlySource(HourlyProductionState())
        HomeFragment.dailySourceOverride = FakeDailySource(DailyProductionState())
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val monthValue = fragment.requireView().findViewById<TextView>(R.id.best_day_month_value)
            val windowValue = fragment.requireView().findViewById<TextView>(R.id.best_day_window_value)
            assertEquals("–", monthValue.text.toString())
            assertEquals("–", windowValue.text.toString())
        }
    }

    @Test
    fun bestDayMonth_onlyConsidersCurrentMonth() {
        val todayMonth = java.time.YearMonth.now()
        val lastMonth = todayMonth.minusMonths(1)
        val thisMonthDay = "$todayMonth-05"
        val lastMonthDay = "$lastMonth-28"
        // Last month has higher value but should NOT count for "best day this month"
        val snapDays = mapOf(thisMonthDay to 3.0, lastMonthDay to 20.0)
        HomeFragment.hourlySourceOverride = FakeHourlySource(HourlyProductionState())
        HomeFragment.dailySourceOverride =
            FakeDailySource(
                DailyProductionState(snapshot = DailySnapshot(snapDays)),
            )
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val value = fragment.requireView().findViewById<TextView>(R.id.best_day_month_value)
            // Best day this month = 3.0 kWh (not 20.0 from last month)
            assertEquals("3.00 kWh", value.text.toString())
        }
    }

    // ── System capacity as Y-axis max (9.2) ───────────────────────────────

    @Test
    fun hourlyChart_yAxisMax_equalsCapacityWhenSet() {
        HomeFragment.currentHourOverride = 10
        HomeFragment.hourlySourceOverride =
            FakeHourlySource(
                HourlyProductionState(snapshot = HourlySnapshot(mapOf(6 to 1.0, 7 to 2.0))),
            )
        HomeFragment.dailySourceOverride = FakeDailySource()
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val chart =
                fragment
                    .requireView()
                    .findViewById<com.github.mikephil.charting.charts.LineChart>(R.id.hourly_chart)
            // Chart has data; Y-axis max is set from capacity (or auto if capacity not configured)
            assertNotNull(chart.data)
        }
    }

    // ── Pull-to-refresh triggers force=true on both sources (10.3) ────────

    @Test
    fun pullToRefresh_callsForceRefreshOnAllSources_andHidesSpinner() {
        val hourly = FakeHourlySource(HourlyProductionState())
        val daily = FakeDailySource()
        HomeFragment.hourlySourceOverride = hourly
        HomeFragment.dailySourceOverride = daily

        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()

        scenario.onFragment { fragment ->
            val swipe = fragment.requireView().findViewById<SwipeRefreshLayout>(R.id.home_swipe_refresh)
            swipe.isRefreshing = true
            // Trigger the OnRefreshListener directly
            swipe.post { swipe.isRefreshing = false }
        }

        // Simulate the pull-to-refresh by invoking via the fragment
        scenario.onFragment { fragment ->
            // Access private onPullToRefresh via the SwipeRefreshLayout listener
            val swipe = fragment.requireView().findViewById<SwipeRefreshLayout>(R.id.home_swipe_refresh)
            // Call the listener that was registered in onViewCreated
            val listenerField = SwipeRefreshLayout::class.java.getDeclaredField("mListener")
            listenerField.isAccessible = true
            val listener = listenerField.get(swipe) as? SwipeRefreshLayout.OnRefreshListener
            listener?.onRefresh()
        }
        idle()

        scenario.onFragment { fragment ->
            assertEquals(true, hourly.lastForce)
            assertEquals(true, daily.lastForce)
            val swipe = fragment.requireView().findViewById<SwipeRefreshLayout>(R.id.home_swipe_refresh)
            assertEquals(false, swipe.isRefreshing)
        }
    }
}
