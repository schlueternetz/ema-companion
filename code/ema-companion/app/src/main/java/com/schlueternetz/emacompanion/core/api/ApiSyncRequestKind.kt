package com.schlueternetz.emacompanion.core.api

/** The distinct request kinds `ApiSyncScheduler` accepts — see ADR-010. */
enum class ApiSyncRequestKind {
    /** A screen became visible; respects each data source's own throttle. */
    OPPORTUNISTIC,

    /** Pull-to-refresh; bypasses throttle. */
    FORCED,

    /** A credential or Base URL edit; resets throttle so a burst of edits coalesces. */
    SETTINGS_CHANGED,

    /** The unattended background poll; subject to daylight-window/placement/foreground gating. */
    PERIODIC,
}
