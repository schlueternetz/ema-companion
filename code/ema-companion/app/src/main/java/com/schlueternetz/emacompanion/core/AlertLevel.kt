package com.schlueternetz.emacompanion.core

/** Controls when a module health alert (push notification or email) is dispatched. */
enum class AlertLevel {
    OFF,
    ALERTS_ONLY,
    ALL,
}
