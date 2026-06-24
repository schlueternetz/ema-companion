package com.schlueternetz.emacompanion.core.api

/**
 * Implemented by tile repositories that hold a fetch throttle. [SettingsFragment] calls
 * [resetThrottle] on all registered tiles whenever connection settings change (per-field
 * edit, import, or factory reset) so the next Home visit fetches fresh data immediately.
 */
interface ThrottleResettable {
    fun resetThrottle()
}
