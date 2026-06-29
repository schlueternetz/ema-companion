package com.schlueternetz.emacompanion.core.api.modulehealth

/** A single inverter that has not produced energy for one or more consecutive days. */
data class Module(
    val uid: String,
    /** Number of consecutive days with 0 kWh, starting from today and going back. Range: 1–3. */
    val offlineDays: Int,
)
