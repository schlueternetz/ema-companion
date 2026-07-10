package com.schlueternetz.emacompanion.feature.widgets

import com.schlueternetz.emacompanion.core.api.DailySnapshot
import java.time.LocalDate
import java.time.YearMonth

/** Current calendar month's total, in kWh, for the Production Summary widget. */
fun thisMonthTotalKwh(
    snapshot: DailySnapshot?,
    today: LocalDate = LocalDate.now(),
): Double {
    val currentMonth = YearMonth.from(today)
    return snapshot
        ?.days
        ?.entries
        ?.filter { (dateStr, _) -> YearMonth.from(LocalDate.parse(dateStr)) == currentMonth }
        ?.sumOf { it.value } ?: 0.0
}

/** Trailing 30-day total, in kWh, for the Production Summary widget. */
fun last30DaysTotalKwh(
    snapshot: DailySnapshot?,
    today: LocalDate = LocalDate.now(),
): Double {
    val start = today.minusDays(30)
    return snapshot
        ?.days
        ?.entries
        ?.filter { (dateStr, _) ->
            val d = LocalDate.parse(dateStr)
            !d.isBefore(start) && !d.isAfter(today)
        }?.sumOf { it.value } ?: 0.0
}
