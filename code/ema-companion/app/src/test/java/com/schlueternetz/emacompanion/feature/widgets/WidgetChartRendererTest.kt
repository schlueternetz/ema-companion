package com.schlueternetz.emacompanion.feature.widgets

import androidx.test.core.app.ApplicationProvider
import com.schlueternetz.emacompanion.core.api.HourlySnapshot
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

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
                capacity = -1f,
                widthPx = 300,
                heightPx = 150,
            )

        assertEquals(300, bitmap.width)
        assertEquals(150, bitmap.height)
    }
}
