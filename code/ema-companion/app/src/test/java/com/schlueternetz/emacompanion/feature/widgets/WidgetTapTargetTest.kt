package com.schlueternetz.emacompanion.feature.widgets

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetTapTargetTest {
    @Test
    fun normalState_targetsHome() {
        assertEquals(
            WidgetTapTarget.TARGET_HOME,
            WidgetTapTarget.target(configured = true, hasError = false),
        )
    }

    @Test
    fun notConfigured_targetsSettings() {
        assertEquals(
            WidgetTapTarget.TARGET_SETTINGS,
            WidgetTapTarget.target(configured = false, hasError = false),
        )
    }

    @Test
    fun showingError_targetsSettings() {
        assertEquals(
            WidgetTapTarget.TARGET_SETTINGS,
            WidgetTapTarget.target(configured = true, hasError = true),
        )
    }

    @Test
    fun notConfiguredAndError_targetsSettings() {
        assertEquals(
            WidgetTapTarget.TARGET_SETTINGS,
            WidgetTapTarget.target(configured = false, hasError = true),
        )
    }
}
