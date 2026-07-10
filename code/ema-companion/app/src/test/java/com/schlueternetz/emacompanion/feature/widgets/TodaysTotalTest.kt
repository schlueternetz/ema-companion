package com.schlueternetz.emacompanion.feature.widgets

import com.schlueternetz.emacompanion.core.api.HourlySnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class TodaysTotalTest {
    @Test
    fun todaysTotalKwh_sumsAllCachedHours() {
        val snapshot = HourlySnapshot(mapOf(6 to 1.0, 7 to 2.5, 8 to 0.5))

        assertEquals(4.0, todaysTotalKwh(snapshot), 0.0001)
    }

    @Test
    fun todaysTotalKwh_nullSnapshot_returnsZero() {
        assertEquals(0.0, todaysTotalKwh(null), 0.0001)
    }

    @Test
    fun todaysTotalKwh_emptyHours_returnsZero() {
        assertEquals(0.0, todaysTotalKwh(HourlySnapshot(emptyMap())), 0.0001)
    }
}
