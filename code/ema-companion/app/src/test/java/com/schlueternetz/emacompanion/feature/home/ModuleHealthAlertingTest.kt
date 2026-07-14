package com.schlueternetz.emacompanion.feature.home

import com.schlueternetz.emacompanion.core.AlertLevel
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleHealthAlertingTest {
    @Test
    fun off_neverAlerts_onChange() {
        assertFalse(shouldAlert(AlertLevel.OFF, ModuleHealthStatus.GREEN, ModuleHealthStatus.YELLOW))
    }

    @Test
    fun off_neverAlerts_whenPreviousUnset() {
        assertFalse(shouldAlert(AlertLevel.OFF, null, ModuleHealthStatus.YELLOW))
    }

    @Test
    fun alertsOnly_firesOnDegradation() {
        assertTrue(shouldAlert(AlertLevel.ALERTS_ONLY, ModuleHealthStatus.GREEN, ModuleHealthStatus.YELLOW))
    }

    @Test
    fun alertsOnly_firesOnRecovery() {
        assertTrue(shouldAlert(AlertLevel.ALERTS_ONLY, ModuleHealthStatus.RED, ModuleHealthStatus.GREEN))
    }

    @Test
    fun alertsOnly_doesNotFireWhenUnchanged() {
        assertFalse(shouldAlert(AlertLevel.ALERTS_ONLY, ModuleHealthStatus.GREEN, ModuleHealthStatus.GREEN))
    }

    @Test
    fun alertsOnly_firesWhenPreviousUnset() {
        assertTrue(shouldAlert(AlertLevel.ALERTS_ONLY, null, ModuleHealthStatus.GREEN))
    }

    @Test
    fun all_firesEvenWhenUnchanged() {
        assertTrue(shouldAlert(AlertLevel.ALL, ModuleHealthStatus.GREEN, ModuleHealthStatus.GREEN))
    }

    @Test
    fun all_firesOnChange() {
        assertTrue(shouldAlert(AlertLevel.ALL, ModuleHealthStatus.GREEN, ModuleHealthStatus.YELLOW))
    }
}
