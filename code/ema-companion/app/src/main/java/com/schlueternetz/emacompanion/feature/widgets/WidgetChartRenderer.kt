package com.schlueternetz.emacompanion.feature.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
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
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.schlueternetz.emacompanion.core.api.HourlySnapshot
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Renders the same charts [com.schlueternetz.emacompanion.feature.home.HomeFragment] shows live,
 * but off-screen into a [Bitmap] — Glance/RemoteViews cannot host a live custom `View`. Styling
 * (colors, month palette) is kept in sync with `HomeFragment` by eye so widget charts visually
 * match the in-app ones.
 */
object WidgetChartRenderer {
    private const val HOURLY_LINE_COLOR = "#4CAF50"

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

    fun renderHourlyChart(
        context: Context,
        snapshot: HourlySnapshot,
        capacity: Float,
        currentHour: Int,
        widthPx: Int,
        heightPx: Int,
    ): Bitmap {
        val chart = LineChart(context)

        val pastEntries = mutableListOf<Entry>()
        val currentEntries = mutableListOf<Entry>()
        for (h in 6..currentHour) {
            val kwh = snapshot.hours[h] ?: continue
            val xVal = (h - 6).toFloat()
            if (h < currentHour) {
                pastEntries.add(Entry(xVal, kwh.toFloat()))
            } else {
                if (pastEntries.isNotEmpty()) currentEntries.add(pastEntries.last())
                currentEntries.add(Entry(xVal, kwh.toFloat()))
            }
        }

        val dataSets = mutableListOf<ILineDataSet>()
        if (pastEntries.isNotEmpty()) {
            dataSets.add(
                LineDataSet(pastEntries, "").apply {
                    color = Color.parseColor(HOURLY_LINE_COLOR)
                    setDrawCircles(false)
                    lineWidth = 2f
                    setDrawValues(false)
                },
            )
        }
        if (currentEntries.size >= 2) {
            dataSets.add(
                LineDataSet(currentEntries, "").apply {
                    color = Color.parseColor(HOURLY_LINE_COLOR)
                    setDrawCircles(false)
                    lineWidth = 2f
                    enableDashedLine(10f, 5f, 0f)
                    setDrawValues(false)
                },
            )
        }

        val xLabels = (6..currentHour).map { h -> if (h % 2 == 0) String.format(Locale.ROOT, "%02d", h) else "" }
        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(xLabels)
            granularity = 1f
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
        }
        chart.axisLeft.apply {
            if (capacity > 0f) axisMaximum = capacity else resetAxisMaximum()
        }
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.data = if (dataSets.isEmpty()) null else LineData(dataSets)

        return chart.drawToBitmap(widthPx, heightPx)
    }

    fun renderHistoryChart(
        context: Context,
        daysInWindow: List<Pair<LocalDate, Double>>,
        capacity: Float,
        widthPx: Int,
        heightPx: Int,
    ): Bitmap {
        val chart = BarChart(context)
        val sorted = daysInWindow.sortedBy { it.first }

        val monthColors = buildMonthColorMap(sorted.map { it.first })
        val entries = sorted.mapIndexed { i, (_, kwh) -> BarEntry(i.toFloat(), kwh.toFloat()) }
        val colors = sorted.map { (d, _) -> monthColors[YearMonth.from(d)] ?: MONTH_PALETTE[0] }

        val monthFmtShort = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
        val xLabels =
            sorted.mapIndexed { i, (d, _) ->
                if (i == 0 || d.dayOfMonth == 1) d.format(monthFmtShort) else ""
            }
        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(xLabels)
            granularity = 1f
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
        }
        chart.axisLeft.apply {
            if (capacity > 0f) axisMaximum = capacity else resetAxisMaximum()
        }
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.data =
            if (entries.isEmpty()) {
                null
            } else {
                BarData(BarDataSet(entries, "").apply { setColors(colors) }.apply { setDrawValues(false) })
            }

        return chart.drawToBitmap(widthPx, heightPx)
    }

    private fun buildMonthColorMap(dates: List<LocalDate>): Map<YearMonth, Int> {
        val months = dates.map { YearMonth.from(it) }.distinct().sorted()
        return months.mapIndexed { i, m -> m to MONTH_PALETTE[i % MONTH_PALETTE.size] }.toMap()
    }

    private fun View.drawToBitmap(
        widthPx: Int,
        heightPx: Int,
    ): Bitmap {
        measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY),
        )
        layout(0, 0, widthPx, heightPx)
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap))
        return bitmap
    }
}
