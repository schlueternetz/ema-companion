package com.schlueternetz.emacompanion.feature.widgets

import com.schlueternetz.emacompanion.core.api.HourlySnapshot

/** Today's running total, in kWh, for reuse by any widget backed by the hourly cache. */
fun todaysTotalKwh(snapshot: HourlySnapshot?): Double = snapshot?.hours?.values?.sum() ?: 0.0
