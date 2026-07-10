package com.schlueternetz.emacompanion.feature.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.api.DailyEnergyRepository
import com.schlueternetz.emacompanion.core.api.DailyEnergySource
import com.schlueternetz.emacompanion.core.api.DailyProductionState
import com.schlueternetz.emacompanion.core.api.DailySnapshot
import com.schlueternetz.emacompanion.core.api.FetchError
import com.schlueternetz.emacompanion.core.api.HourlyEnergyRepository
import com.schlueternetz.emacompanion.core.api.HourlyEnergySource
import com.schlueternetz.emacompanion.core.api.HourlyProductionState
import com.schlueternetz.emacompanion.core.api.HourlySnapshot
import com.schlueternetz.emacompanion.core.api.ProductionRepository
import com.schlueternetz.emacompanion.core.api.ProductionSnapshot
import com.schlueternetz.emacompanion.core.api.ProductionSource
import com.schlueternetz.emacompanion.core.api.ProductionState
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthRepository
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthSource
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthState
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthStatus
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import com.schlueternetz.emacompanion.feature.widgets.WidgetUpdater
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {
    private lateinit var productionView: TextView
    private lateinit var updatedView: TextView
    private lateinit var statusView: TextView
    private lateinit var source: ProductionSource

    private lateinit var moduleHealthTile: View
    private lateinit var moduleHealthIcon: ImageView
    private lateinit var moduleHealthStatusView: TextView
    private lateinit var moduleHealthCheckedView: TextView
    private lateinit var moduleHealthErrorView: TextView
    private lateinit var moduleHealthSource: ModuleHealthSource

    // Today section
    private lateinit var hourlyChart: LineChart
    private lateinit var hourlyUpdated: TextView
    private lateinit var morningTable: LinearLayout
    private lateinit var afternoonTable: LinearLayout
    private lateinit var todayTotal: TextView
    private lateinit var bestDayMonthDate: TextView
    private lateinit var bestDayMonthValue: TextView
    private lateinit var bestDayWindowLabel: TextView
    private lateinit var bestDayWindowDate: TextView
    private lateinit var bestDayWindowValue: TextView
    private lateinit var hourlyStatus: TextView
    private lateinit var hourlyPlaceholder: TextView
    private lateinit var hourlySource: HourlyEnergySource

    // History section
    private lateinit var historyChart: BarChart
    private lateinit var historyUpdated: TextView
    private lateinit var historyLegend: LinearLayout
    private lateinit var thisMonthTotal: TextView
    private lateinit var last30Total: TextView
    private lateinit var historyStatus: TextView
    private lateinit var historyPlaceholder: TextView
    private lateinit var dailySource: DailyEnergySource

    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Current production
        productionView = view.findViewById(R.id.text_current_production)
        updatedView = view.findViewById(R.id.production_updated)
        statusView = view.findViewById(R.id.production_status)
        source = sourceOverride ?: ProductionRepository.create(requireContext())

        // Module health
        moduleHealthTile = view.findViewById(R.id.tile_module_health)
        moduleHealthIcon = view.findViewById(R.id.module_health_icon)
        moduleHealthStatusView = view.findViewById(R.id.module_health_status)
        moduleHealthCheckedView = view.findViewById(R.id.module_health_checked)
        moduleHealthErrorView = view.findViewById(R.id.module_health_error)
        moduleHealthSource = moduleHealthSourceOverride ?: ModuleHealthRepository.create(requireContext())

        // Today section
        hourlyChart = view.findViewById(R.id.hourly_chart)
        hourlyUpdated = view.findViewById(R.id.hourly_updated)
        morningTable = view.findViewById(R.id.morning_table)
        afternoonTable = view.findViewById(R.id.afternoon_table)
        todayTotal = view.findViewById(R.id.today_total)
        bestDayMonthDate = view.findViewById(R.id.best_day_month_date)
        bestDayMonthValue = view.findViewById(R.id.best_day_month_value)
        bestDayWindowLabel = view.findViewById(R.id.best_day_window_label)
        bestDayWindowDate = view.findViewById(R.id.best_day_window_date)
        bestDayWindowValue = view.findViewById(R.id.best_day_window_value)
        hourlyStatus = view.findViewById(R.id.hourly_status)
        hourlyPlaceholder = view.findViewById(R.id.hourly_placeholder)
        hourlySource = hourlySourceOverride ?: HourlyEnergyRepository.create(requireContext())

        // History section
        historyChart = view.findViewById(R.id.history_chart)
        historyUpdated = view.findViewById(R.id.history_updated)
        historyLegend = view.findViewById(R.id.history_legend)
        thisMonthTotal = view.findViewById(R.id.this_month_total)
        last30Total = view.findViewById(R.id.last_30_total)
        historyStatus = view.findViewById(R.id.history_status)
        historyPlaceholder = view.findViewById(R.id.history_placeholder)
        dailySource = dailySourceOverride ?: DailyEnergyRepository.create(requireContext())

        // Pull-to-refresh
        swipeRefresh = view.findViewById(R.id.home_swipe_refresh)
        swipeRefresh.setOnRefreshListener { onPullToRefresh() }

        // Load best-day window label (needs historyDays from settings)
        val historyDays = SettingsRepository.create(requireContext()).getHistoricDataDays()
        bestDayWindowLabel.text = getString(R.string.home_best_day_window_label, historyDays)

        // Seed from persisted state immediately (no flash before fetch in onResume)
        render(source.currentState())
        renderModuleHealth(moduleHealthSource.currentState())
        bindHourlyState(hourlySource.currentState(), systemCapacity())
        bindDailyState(dailySource.currentState(), systemCapacity(), historyDays)
    }

    override fun onResume() {
        super.onResume()
        val cap = systemCapacity()
        val historyDays = SettingsRepository.create(requireContext()).getHistoricDataDays()
        viewLifecycleOwner.lifecycleScope.launch {
            render(source.refresh())
        }
        viewLifecycleOwner.lifecycleScope.launch {
            renderModuleHealth(moduleHealthSource.refresh())
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val state = hourlySource.refresh(force = false)
            bindHourlyState(state, cap)
            if (state.error == null) widgetUpdateAction(requireContext())
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val state = dailySource.refresh(force = false)
            bindDailyState(state, cap, historyDays)
            if (state.error == null) widgetUpdateAction(requireContext())
        }
    }

    private fun onPullToRefresh() {
        val cap = systemCapacity()
        val historyDays = SettingsRepository.create(requireContext()).getHistoricDataDays()
        viewLifecycleOwner.lifecycleScope.launch {
            val prod = async { source.refresh(force = true) }
            val hourly = async { hourlySource.refresh(force = true) }
            val daily = async { dailySource.refresh(force = true) }
            render(prod.await())
            val hourlyState = hourly.await()
            bindHourlyState(hourlyState, cap)
            if (hourlyState.error == null) widgetUpdateAction(requireContext())
            val dailyState = daily.await()
            bindDailyState(dailyState, cap, historyDays)
            if (dailyState.error == null) widgetUpdateAction(requireContext())
            swipeRefresh.isRefreshing = false
        }
    }

    private fun systemCapacity(): Float = SettingsRepository.create(requireContext()).getSystemCapacity()

    // ── Current production ────────────────────────────────────────────────

    private fun render(state: ProductionState) {
        val value = state.snapshot?.powerWatts?.toString() ?: getString(R.string.home_production_neutral)
        productionView.text = getString(R.string.home_production_value, value, ProductionSnapshot.UNIT)

        val updatedAt = state.updatedAtEpochMs
        if (updatedAt == null) {
            updatedView.visibility = View.GONE
        } else {
            val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(updatedAt))
            updatedView.text = getString(R.string.home_production_updated, time)
            updatedView.visibility = View.VISIBLE
        }

        val statusText =
            when (state.error) {
                FetchError.NETWORK -> getString(R.string.home_status_network_error)
                FetchError.AUTH -> getString(R.string.home_status_auth_error)
                FetchError.API -> getString(R.string.home_status_api_error)
                null -> null
            }
        if (statusText == null) {
            statusView.visibility = View.GONE
        } else {
            statusView.text = statusText
            statusView.contentDescription = statusText
            statusView.visibility = View.VISIBLE
        }
    }

    // ── Module health ─────────────────────────────────────────────────────

    private fun renderModuleHealth(state: ModuleHealthState) {
        if (state.error != null) {
            moduleHealthStatusView.text = ""
            moduleHealthStatusView.visibility = View.VISIBLE
            moduleHealthIcon.setImageResource(R.drawable.ic_help_circle)
            moduleHealthIcon.setColorFilter(Color.parseColor("#9E9E9E"))
            moduleHealthIcon.contentDescription = getString(R.string.home_module_health_status_unknown)
        } else {
            val statusText =
                when (state.status) {
                    ModuleHealthStatus.GREEN -> getString(R.string.home_module_health_status_green)
                    ModuleHealthStatus.YELLOW -> getString(R.string.home_module_health_status_yellow)
                    ModuleHealthStatus.RED -> getString(R.string.home_module_health_status_red)
                    ModuleHealthStatus.UNKNOWN -> getString(R.string.home_module_health_status_unknown)
                }
            moduleHealthStatusView.text = statusText
            moduleHealthStatusView.visibility = View.VISIBLE
            val iconRes =
                when (state.status) {
                    ModuleHealthStatus.GREEN, ModuleHealthStatus.UNKNOWN -> R.drawable.ic_check_circle
                    ModuleHealthStatus.YELLOW, ModuleHealthStatus.RED -> R.drawable.ic_warning
                }
            moduleHealthIcon.setImageResource(iconRes)
            val tintColor =
                when (state.status) {
                    ModuleHealthStatus.GREEN -> Color.parseColor("#4CAF50")
                    ModuleHealthStatus.YELLOW -> Color.parseColor("#FFC107")
                    ModuleHealthStatus.RED -> Color.parseColor("#F44336")
                    ModuleHealthStatus.UNKNOWN -> Color.parseColor("#9E9E9E")
                }
            moduleHealthIcon.setColorFilter(tintColor)
        }

        val checkedAt = state.checkedAtEpochMs
        if (checkedAt == null) {
            moduleHealthCheckedView.visibility = View.GONE
        } else {
            val date = DateFormat.getDateInstance(DateFormat.SHORT).format(Date(checkedAt))
            val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(checkedAt))
            moduleHealthCheckedView.text = getString(R.string.home_module_health_checked, date, time)
            moduleHealthCheckedView.visibility = View.VISIBLE
        }

        val errorText =
            when (state.error) {
                FetchError.NETWORK -> getString(R.string.home_module_health_error_network)
                FetchError.AUTH -> getString(R.string.home_module_health_error_auth)
                FetchError.API -> getString(R.string.home_module_health_error_api)
                null -> null
            }
        if (errorText == null) {
            moduleHealthErrorView.visibility = View.GONE
        } else {
            moduleHealthErrorView.text = errorText
            moduleHealthErrorView.contentDescription = errorText
            moduleHealthErrorView.visibility = View.VISIBLE
        }

        if (state.status == ModuleHealthStatus.GREEN || state.status == ModuleHealthStatus.UNKNOWN) {
            moduleHealthTile.isClickable = false
            moduleHealthTile.isFocusable = false
        } else {
            moduleHealthTile.isClickable = true
            moduleHealthTile.isFocusable = true
            moduleHealthTile.setOnClickListener { showModuleHealthDetail(state) }
        }
    }

    private fun showModuleHealthDetail(state: ModuleHealthState) {
        val message =
            buildString {
                state.offlineModules.forEach { module ->
                    val line =
                        if (module.offlineDays == 1) {
                            getString(R.string.home_module_health_offline_singular)
                        } else {
                            getString(R.string.home_module_health_offline_plural, module.offlineDays)
                        }
                    appendLine("${module.uid}: $line")
                }
            }.trimEnd()
        AlertDialog
            .Builder(requireContext())
            .setTitle(R.string.home_module_health_detail_title)
            .setMessage(message.ifEmpty { getString(R.string.home_module_health_status_unknown) })
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    // ── Today section (hourly) ────────────────────────────────────────────

    private fun bindHourlyState(
        state: HourlyProductionState,
        capacity: Float,
    ) {
        val snapshot = state.snapshot
        val hasData = snapshot != null && snapshot.hours.isNotEmpty()

        if (!hasData && state.error == null) {
            // No data and no error: show placeholder, hide chart content
            hourlyPlaceholder.visibility = View.VISIBLE
            hourlyChart.visibility = View.GONE
            hourlyStatus.visibility = View.GONE
            hourlyUpdated.visibility = View.GONE
            todayTotal.text = getString(R.string.home_today_neutral)
            bestDayMonthDate.text = getString(R.string.home_best_day_neutral)
            bestDayMonthValue.text = getString(R.string.home_best_day_neutral)
            bestDayWindowDate.text = getString(R.string.home_best_day_neutral)
            bestDayWindowValue.text = getString(R.string.home_best_day_neutral)
            morningTable.removeAllViews()
            afternoonTable.removeAllViews()
            return
        }

        hourlyPlaceholder.visibility = View.GONE
        hourlyChart.visibility = View.VISIBLE

        if (snapshot != null) {
            bindHourlyChart(snapshot, capacity)
            bindHourlyTables(snapshot)
            val total = snapshot.hours.values.sum()
            todayTotal.text = getString(R.string.home_today_value_kwh, total)
        }

        val updatedAt = state.updatedAtEpochMs
        if (updatedAt != null) {
            val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(updatedAt))
            hourlyUpdated.text = getString(R.string.home_today_updated, time)
            hourlyUpdated.visibility = View.VISIBLE
        } else {
            hourlyUpdated.visibility = View.GONE
        }

        val statusText =
            when (state.error) {
                FetchError.NETWORK -> getString(R.string.home_today_status_network_error)
                FetchError.AUTH -> getString(R.string.home_today_status_auth_error)
                FetchError.API -> getString(R.string.home_today_status_api_error)
                null -> null
            }
        if (statusText == null) {
            hourlyStatus.visibility = View.GONE
        } else {
            hourlyStatus.text = statusText
            hourlyStatus.contentDescription = statusText
            hourlyStatus.visibility = View.VISIBLE
        }
    }

    private fun bindHourlyChart(
        snapshot: HourlySnapshot,
        capacity: Float,
    ) {
        val currentHour =
            currentHourOverride ?: java.time.LocalTime
                .now()
                .hour

        // Plot hours 06 to currentHour
        val pastEntries = mutableListOf<Entry>()
        val currentEntries = mutableListOf<Entry>()

        for (h in 6..currentHour) {
            val kwh = snapshot.hours[h] ?: continue
            val xVal = (h - 6).toFloat()
            if (h < currentHour) {
                pastEntries.add(Entry(xVal, kwh.toFloat()))
            } else {
                // Current hour: add last past entry + current as a separate dashed dataset
                if (pastEntries.isNotEmpty()) {
                    currentEntries.add(pastEntries.last())
                }
                currentEntries.add(Entry(xVal, kwh.toFloat()))
            }
        }

        val dataSets = mutableListOf<com.github.mikephil.charting.interfaces.datasets.ILineDataSet>()

        if (pastEntries.isNotEmpty()) {
            val solidSet =
                LineDataSet(pastEntries, "").apply {
                    color = Color.parseColor("#4CAF50")
                    setDrawCircles(false)
                    lineWidth = 2f
                    setDrawValues(false)
                }
            dataSets.add(solidSet)
        }

        if (currentEntries.size >= 2) {
            val dashedSet =
                LineDataSet(currentEntries, "").apply {
                    color = Color.parseColor("#4CAF50")
                    setDrawCircles(false)
                    lineWidth = 2f
                    enableDashedLine(10f, 5f, 0f)
                    setDrawValues(false)
                }
            dataSets.add(dashedSet)
        }

        // X-axis: label every 2 hours (06, 08, 10, …)
        val xLabels = (6..currentHour).map { h -> if (h % 2 == 0) String.format("%02d", h) else "" }
        hourlyChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(xLabels)
            granularity = 1f
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
        }

        // Y-axis: max = capacity if set, else auto
        hourlyChart.axisLeft.apply {
            if (capacity > 0f) {
                axisMaximum = capacity
            } else {
                resetAxisMaximum()
            }
        }
        hourlyChart.axisRight.isEnabled = false
        hourlyChart.description.isEnabled = false
        hourlyChart.legend.isEnabled = false

        hourlyChart.data = if (dataSets.isEmpty()) null else LineData(dataSets)
        hourlyChart.invalidate()
    }

    private fun bindHourlyTables(snapshot: HourlySnapshot) {
        morningTable.removeAllViews()
        afternoonTable.removeAllViews()

        for (h in 0..11) {
            morningTable.addView(makeHourRow(h, snapshot.hours[h]))
        }
        for (h in 12..23) {
            afternoonTable.addView(makeHourRow(h, snapshot.hours[h]))
        }
    }

    private fun makeHourRow(
        hour: Int,
        kwh: Double?,
    ): View {
        val ctx = requireContext()
        val row =
            LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
            }
        val label =
            TextView(ctx).apply {
                text = String.format(Locale.getDefault(), "%02d:00", hour)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
        val value =
            TextView(ctx).apply {
                text = if (kwh != null) String.format(Locale.getDefault(), "%.2f", kwh) else getString(R.string.home_today_neutral)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
        row.addView(label)
        row.addView(value)
        return row
    }

    // ── Best-day cards (driven by daily data) ────────────────────────────

    private fun bindBestDayCards(
        state: DailyProductionState,
        historyDays: Int,
    ) {
        val days = state.snapshot?.days
        val neutral = getString(R.string.home_best_day_neutral)

        if (days.isNullOrEmpty()) {
            bestDayMonthDate.text = neutral
            bestDayMonthValue.text = neutral
            bestDayWindowDate.text = neutral
            bestDayWindowValue.text = neutral
            return
        }

        val today = LocalDate.now()
        val currentMonth = YearMonth.now()
        val fmt = DateTimeFormatter.ofPattern("EEE dd MMM", Locale.getDefault())

        // Best day this month
        val bestMonth =
            days.entries
                .filter { (dateStr, _) -> YearMonth.from(LocalDate.parse(dateStr)) == currentMonth }
                .maxByOrNull { it.value }
        if (bestMonth != null) {
            bestDayMonthDate.text = LocalDate.parse(bestMonth.key).format(fmt)
            bestDayMonthValue.text = getString(R.string.home_today_value_kwh, bestMonth.value)
        } else {
            bestDayMonthDate.text = neutral
            bestDayMonthValue.text = neutral
        }

        // Best day in history window
        val windowStart = today.minusDays(historyDays.toLong())
        val bestWindow =
            days.entries
                .filter { (dateStr, _) ->
                    val d = LocalDate.parse(dateStr)
                    !d.isBefore(windowStart) && !d.isAfter(today)
                }.maxByOrNull { it.value }
        if (bestWindow != null) {
            bestDayWindowDate.text = LocalDate.parse(bestWindow.key).format(fmt)
            bestDayWindowValue.text = getString(R.string.home_today_value_kwh, bestWindow.value)
        } else {
            bestDayWindowDate.text = neutral
            bestDayWindowValue.text = neutral
        }
    }

    // ── History section (daily) ───────────────────────────────────────────

    private fun bindDailyState(
        state: DailyProductionState,
        capacity: Float,
        historyDays: Int,
    ) {
        val snapshot = state.snapshot
        val hasData = snapshot != null && snapshot.days.isNotEmpty()

        if (!hasData && state.error == null) {
            historyPlaceholder.visibility = View.VISIBLE
            historyChart.visibility = View.GONE
            historyStatus.visibility = View.GONE
            historyUpdated.visibility = View.GONE
            thisMonthTotal.text = getString(R.string.home_history_neutral)
            last30Total.text = getString(R.string.home_history_neutral)
            historyLegend.removeAllViews()
            bindBestDayCards(state, historyDays)
            return
        }

        historyPlaceholder.visibility = View.GONE
        historyChart.visibility = View.VISIBLE

        if (snapshot != null) {
            bindHistoryChart(snapshot, capacity, historyDays)
            bindPeriodTotals(snapshot)
        }
        bindBestDayCards(state, historyDays)

        val updatedAt = state.updatedAtEpochMs
        if (updatedAt != null) {
            val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(updatedAt))
            historyUpdated.text = getString(R.string.home_history_updated, time)
            historyUpdated.visibility = View.VISIBLE
        } else {
            historyUpdated.visibility = View.GONE
        }

        val statusText =
            when (state.error) {
                FetchError.NETWORK -> getString(R.string.home_history_status_network_error)
                FetchError.AUTH -> getString(R.string.home_history_status_auth_error)
                FetchError.API -> getString(R.string.home_history_status_api_error)
                null -> null
            }
        if (statusText == null) {
            historyStatus.visibility = View.GONE
        } else {
            historyStatus.text = statusText
            historyStatus.contentDescription = statusText
            historyStatus.visibility = View.VISIBLE
        }
    }

    private fun bindHistoryChart(
        snapshot: DailySnapshot,
        capacity: Float,
        historyDays: Int,
    ) {
        val today = LocalDate.now()
        val windowStart = today.minusDays(historyDays.toLong())

        // Sort days in window, assign colours by month
        val daysInWindow =
            snapshot.days.entries
                .mapNotNull { (dateStr, kwh) ->
                    val d = LocalDate.parse(dateStr)
                    if (!d.isBefore(windowStart) && !d.isAfter(today)) Pair(d, kwh) else null
                }.sortedBy { it.first }

        if (daysInWindow.isEmpty()) {
            historyChart.clear()
            historyLegend.removeAllViews()
            return
        }

        val monthColors = buildMonthColorMap(daysInWindow.map { it.first })
        val entries = daysInWindow.mapIndexed { i, (_, kwh) -> BarEntry(i.toFloat(), kwh.toFloat()) }
        val colors = daysInWindow.map { (d, _) -> monthColors[YearMonth.from(d)] ?: MONTH_PALETTE[0] }

        val dataSet =
            BarDataSet(entries, "").apply {
                setColors(colors)
                setDrawValues(false)
            }

        // X-axis: show month abbreviation at the first bar of each new month
        val monthFmtShort = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
        val xLabels =
            daysInWindow.mapIndexed { i, (d, _) ->
                if (i == 0 || d.dayOfMonth == 1) d.format(monthFmtShort) else ""
            }
        historyChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(xLabels)
            granularity = 1f
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
        }

        historyChart.axisLeft.apply {
            resetAxisMaximum()
        }
        historyChart.axisRight.isEnabled = false
        historyChart.description.isEnabled = false
        historyChart.legend.isEnabled = false

        historyChart.data = BarData(dataSet)
        historyChart.invalidate()

        // Build legend
        buildLegend(monthColors)
    }

    private fun buildMonthColorMap(dates: List<LocalDate>): Map<YearMonth, Int> {
        val months = dates.map { YearMonth.from(it) }.distinct().sorted()
        return months.mapIndexed { i, m -> m to MONTH_PALETTE[i % MONTH_PALETTE.size] }.toMap()
    }

    private fun buildLegend(monthColors: Map<YearMonth, Int>) {
        historyLegend.removeAllViews()
        val ctx = requireContext()
        val monthFmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())
        monthColors.entries.sortedBy { it.key }.forEach { (month, color) ->
            val chip =
                LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams =
                        LinearLayout
                            .LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                            ).also { it.setMargins(0, 0, 16, 0) }
                }
            val dot =
                View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(16, 16).also { it.setMargins(0, 2, 4, 0) }
                    setBackgroundColor(color)
                }
            val label =
                TextView(ctx).apply {
                    text = month.format(monthFmt)
                    textSize = 12f
                }
            chip.addView(dot)
            chip.addView(label)
            historyLegend.addView(chip)
        }
    }

    private fun bindPeriodTotals(snapshot: DailySnapshot) {
        val today = LocalDate.now()
        val currentMonth = YearMonth.now()

        val monthSum =
            snapshot.days.entries
                .filter { (dateStr, _) -> YearMonth.from(LocalDate.parse(dateStr)) == currentMonth }
                .sumOf { it.value }
        thisMonthTotal.text = getString(R.string.home_today_value_kwh, monthSum)

        val last30Start = today.minusDays(30)
        val last30Sum =
            snapshot.days.entries
                .filter { (dateStr, _) ->
                    val d = LocalDate.parse(dateStr)
                    !d.isBefore(last30Start) && !d.isAfter(today)
                }.sumOf { it.value }
        last30Total.text = getString(R.string.home_today_value_kwh, last30Sum)
    }

    companion object {
        /** Test seam: substitutes the production source so Home can be tested without HTTP. */
        var sourceOverride: ProductionSource? = null

        /** Test seam: substitutes the module health source so the tile can be tested without HTTP. */
        var moduleHealthSourceOverride: ModuleHealthSource? = null

        /** Test seam: substitutes the hourly energy source. */
        var hourlySourceOverride: HourlyEnergySource? = null

        /** Test seam: substitutes the daily energy source. */
        var dailySourceOverride: DailyEnergySource? = null

        /** Test seam: overrides the current hour used for chart rendering (0–23). */
        var currentHourOverride: Int? = null

        internal val defaultWidgetUpdateAction: suspend (android.content.Context) -> Unit =
            { context -> WidgetUpdater.updateAll(context) }

        /** Test seam: substitutes the widget-update side effect invoked after a successful hourly/daily refresh. */
        var widgetUpdateAction: suspend (android.content.Context) -> Unit = defaultWidgetUpdateAction

        private val MONTH_PALETTE =
            listOf(
                Color.parseColor("#4CAF50"), // green
                Color.parseColor("#2196F3"), // blue
                Color.parseColor("#FF9800"), // orange
                Color.parseColor("#9C27B0"), // purple
                Color.parseColor("#F44336"), // red
                Color.parseColor("#00BCD4"), // cyan
                Color.parseColor("#FFEB3B"), // yellow
                Color.parseColor("#795548"), // brown
            )
    }
}
