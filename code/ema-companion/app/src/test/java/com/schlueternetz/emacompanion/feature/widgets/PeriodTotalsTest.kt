package com.schlueternetz.emacompanion.feature.widgets

import com.schlueternetz.emacompanion.core.api.DailySnapshot
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PeriodTotalsTest {
    private val today = LocalDate.of(2026, 7, 15)

    @Test
    fun thisMonthTotalKwh_sumsOnlyCurrentMonthDays() {
        val snapshot =
            DailySnapshot(
                mapOf(
                    "2026-07-01" to 1.0,
                    "2026-07-15" to 2.0,
                    "2026-06-30" to 5.0,
                ),
            )

        assertEquals(3.0, thisMonthTotalKwh(snapshot, today), 0.0001)
    }

    @Test
    fun thisMonthTotalKwh_nullSnapshot_returnsZero() {
        assertEquals(0.0, thisMonthTotalKwh(null, today), 0.0001)
    }

    @Test
    fun last30DaysTotalKwh_sumsOnlyDaysWithinWindow() {
        val snapshot =
            DailySnapshot(
                mapOf(
                    "2026-07-15" to 1.0,
                    "2026-06-20" to 2.0, // within last 30 days of 2026-07-15
                    "2026-06-01" to 3.0, // outside window
                ),
            )

        assertEquals(3.0, last30DaysTotalKwh(snapshot, today), 0.0001)
    }

    @Test
    fun last30DaysTotalKwh_nullSnapshot_returnsZero() {
        assertEquals(0.0, last30DaysTotalKwh(null, today), 0.0001)
    }
}
