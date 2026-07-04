package com.schlueternetz.emacompanion.core.api

/** Lets tests substitute a fake daily energy source without HTTP. */
interface DailyEnergySource {
    fun currentState(): DailyProductionState

    suspend fun refresh(force: Boolean = false): DailyProductionState
}
