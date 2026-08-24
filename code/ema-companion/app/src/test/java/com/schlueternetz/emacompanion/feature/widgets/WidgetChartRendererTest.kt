package com.schlueternetz.emacompanion.feature.widgets

import androidx.test.core.app.ApplicationProvider
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.schlueternetz.emacompanion.core.api.HourlySnapshot
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

private val NO_GAP_DAYS =
    listOf(
        LocalDate.of(2026, 5, 20) to 5.0,
        LocalDate.of(2026, 6, 3) to 6.0,
        LocalDate.of(2026, 7, 5) to 4.0,
    )

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WidgetChartRendererTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun renderHourlyChart_returnsBitmapAtRequestedSize() {
        val snapshot = HourlySnapshot(mapOf(6 to 1.0, 7 to 2.0, 8 to 1.5))

        val bitmap =
            WidgetChartRenderer.renderHourlyChart(
                context = context,
                snapshot = snapshot,
                capacity = -1f,
                currentHour = 8,
                widthPx = 300,
                heightPx = 150,
            )

        assertEquals(300, bitmap.width)
        assertEquals(150, bitmap.height)
    }

    @Test
    fun renderHistoryChart_returnsBitmapAtRequestedSize() {
        val days =
            listOf(
                LocalDate.of(2026, 6, 30) to 5.0,
                LocalDate.of(2026, 7, 1) to 6.0,
                LocalDate.of(2026, 7, 2) to 4.5,
            )

        val bitmap =
            WidgetChartRenderer.renderHistoryChart(
                context = context,
                daysInWindow = days,
                widthPx = 300,
                heightPx = 150,
            )

        assertEquals(300, bitmap.width)
        assertEquals(150, bitmap.height)
    }

    @Test
    fun historyXAxisLabels_labelsFirstBarOfEveryMonth_evenWithoutADay1Entry() {
        val sortedDates = NO_GAP_DAYS.map { it.first }

        val labels = WidgetChartRenderer.historyXAxisLabels(sortedDates)

        assertEquals(listOf("May", "Jun", "Jul"), labels)
    }

    @Test
    fun configureValueAxis_startsAtZero_forLineChart() {
        val chart = LineChart(context)

        WidgetChartRenderer.configureValueAxis(chart, capacity = 5f)

        assertEquals(0f, chart.axisLeft.axisMinimum, 0.001f)
    }

    @Test
    fun configureValueAxis_startsAtZero_forBarChart() {
        val chart = BarChart(context)

        WidgetChartRenderer.configureValueAxis(chart, capacity = -1f)

        assertEquals(0f, chart.axisLeft.axisMinimum, 0.001f)
    }
}
