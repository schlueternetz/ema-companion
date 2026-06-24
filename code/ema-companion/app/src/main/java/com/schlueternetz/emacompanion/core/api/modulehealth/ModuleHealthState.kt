package com.schlueternetz.emacompanion.core.api.modulehealth

import com.schlueternetz.emacompanion.core.api.FetchError

enum class ModuleHealthStatus { UNKNOWN, GREEN, YELLOW, RED }

/**
 * The module health state rendered by the Home tile. [UNKNOWN] means no successful check has
 * run yet. [checkedAtEpochMs] is the timestamp of the last successful check (null if never run).
 * [error] is non-null when the latest fetch failed (see ADR-006).
 */
data class ModuleHealthState(
    val status: ModuleHealthStatus = ModuleHealthStatus.UNKNOWN,
    val offlineModules: List<Module> = emptyList(),
    val checkedAtEpochMs: Long? = null,
    val error: FetchError? = null,
)

/** The dependency the Home tile and background worker rely on for module health data. */
interface ModuleHealthSource {
    /** State to show immediately (from persisted store), before any network fetch. */
    fun currentState(): ModuleHealthState

    /** Fetch new data if throttle has expired, then return the updated state. */
    suspend fun refresh(): ModuleHealthState
}
