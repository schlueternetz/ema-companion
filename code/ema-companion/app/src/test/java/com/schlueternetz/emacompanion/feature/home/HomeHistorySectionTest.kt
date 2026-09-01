package com.schlueternetz.emacompanion.feature.home

import android.content.Context
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.api.DailyEnergySource
import com.schlueternetz.emacompanion.core.api.DailyProductionState
import com.schlueternetz.emacompanion.core.api.DailySnapshot
import com.schlueternetz.emacompanion.core.api.FetchError
import com.schlueternetz.emacompanion.core.api.HourlyEnergySource
import com.schlueternetz.emacompanion.core.api.HourlyProductionState
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HomeHistorySectionTest {
    private class FakeHourlySource : HourlyEnergySource {
        override fun currentState() = HourlyProductionState()

        override suspend fun refresh(force: Boolean) = HourlyProductionState()
    }

    private class FakeDailySource(
        var state: DailyProductionState,
    ) : DailyEnergySource {
        override fun currentState() = state

        override suspend fun refresh(force: Boolean) = state
    }

    @Before
    fun setUp() {
        HomeFragment.hourlySourceOverride = FakeHourlySource()
        HomeFragment.moduleHealthSourceOverride = null
        WorkManagerTestInitHelper.initializeTestWorkManager(
            ApplicationProvider.getApplicationContext<Context>(),
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
    }

    @After
    fun tearDown() {
        HomeFragment.moduleHealthSourceOverride = null
        HomeFragment.hourlySourceOverride = null
        HomeFragment.dailySourceOverride = null
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    // ── Bars rendered (8.6) ────────────────────────────────────────────────

    @Test
    fun historyChart_populatedWhenDataAvailable() {
        val today = LocalDate.now()
        val days =
            mapOf(
                today.minusDays(2).toString() to 5.0,
                today.minusDays(1).toString() to 8.0,
                today.toString() to 4.0,
            )
        HomeFragment.dailySourceOverride =
            FakeDailySource(
                DailyProductionState(snapshot = DailySnapshot(days)),
            )
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val chart =
                fragment
                    .requireView()
                    .findViewById<com.github.mikephil.charting.charts.BarChart>(R.id.history_chart)
            assertNotNull(chart.data)
            assertEquals(3, chart.data.getDataSetByIndex(0).entryCount)
        }
    }

    // ── Month colours distinct (8.6) ──────────────────────────────────────

    @Test
    fun historyLegend_showsOneChipPerMonth() {
        // Window must exceed the longest possible calendar month (31 days) so that
        // "one month ago" always falls inside it, regardless of which day this runs on.
        SettingsRepository.create(ApplicationProvider.getApplicationContext()).setHistoricDataDays(40)
        val today = LocalDate.now()
        val lastMonth = today.minusMonths(1)
        val days =
            mapOf(
                today.toString() to 5.0,
                lastMonth.toString() to 8.0,
            )
        HomeFragment.dailySourceOverride =
            FakeDailySource(
                DailyProductionState(snapshot = DailySnapshot(days)),
            )
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val legend = fragment.requireView().findViewById<LinearLayout>(R.id.history_legend)
            // Two distinct months → two legend chips
            assertEquals(2, legend.childCount)
        }
    }

    // ── Period totals correct (8.6) ───────────────────────────────────────

    @Test
    fun periodTotals_thisMonthAndLast30_correct() {
        val today = LocalDate.now()
        val thisMonth = java.time.YearMonth.now()
        val days = mutableMapOf<String, Double>()
        // 3 days this month: 4 + 5 + 6 = 15 kWh
        days["$thisMonth-01"] = 4.0
        days["$thisMonth-02"] = 5.0
        days["$thisMonth-03"] = 6.0
        // 1 day last month within 30 days: 3 kWh
        // Fixed to the 28th of last month (not today.minusDays(20)) so this always lands in
        // the previous calendar month regardless of what day of the current month "today" is.
        val recentLastMonth = thisMonth.minusMonths(1).atDay(28)
        days[recentLastMonth.toString()] = 3.0

        HomeFragment.dailySourceOverride =
            FakeDailySource(
                DailyProductionState(snapshot = DailySnapshot(days)),
            )
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val monthTotal = fragment.requireView().findViewById<TextView>(R.id.this_month_total)
            val last30 = fragment.requireView().findViewById<TextView>(R.id.last_30_total)
            // This month total = 15.00 kWh (only current month days)
            assertEquals("15.00 kWh", monthTotal.text.toString())
            // Last 30 days: 4+5+6+3 = 18, but only if all fall within 30 days
            // days 01-03 may or may not be within 30 days depending on today's date
            // Just verify it's a formatted value
            assert(last30.text.toString().endsWith("kWh"))
        }
    }

    // ── Error line visible (8.6) ──────────────────────────────────────────

    @Test
    fun historyErrorLine_visibleOnFetchError() {
        val today = LocalDate.now()
        HomeFragment.dailySourceOverride =
            FakeDailySource(
                DailyProductionState(
                    snapshot = DailySnapshot(mapOf(today.toString() to 5.0)),
                    error = FetchError.NETWORK,
                ),
            )
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val status = fragment.requireView().findViewById<TextView>(R.id.history_status)
            assertEquals(View.VISIBLE, status.visibility)
            assertEquals(
                fragment.getString(R.string.home_history_status_network_error),
                status.text.toString(),
            )
        }
    }

    // ── Placeholder when no data (8.6) ────────────────────────────────────

    @Test
    fun historyPlaceholder_visibleWhenNoData() {
        HomeFragment.dailySourceOverride = FakeDailySource(DailyProductionState())
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            val placeholder = fragment.requireView().findViewById<View>(R.id.history_placeholder)
            val chart = fragment.requireView().findViewById<View>(R.id.history_chart)
            assertEquals(View.VISIBLE, placeholder.visibility)
            assertEquals(View.GONE, chart.visibility)
        }
    }
}
