package com.schlueternetz.emacompanion.core.api

/** Lets tests substitute a fake hourly energy source without HTTP. */
interface HourlyEnergySource {
    fun currentState(): HourlyProductionState

    suspend fun refresh(force: Boolean = false): HourlyProductionState
}
