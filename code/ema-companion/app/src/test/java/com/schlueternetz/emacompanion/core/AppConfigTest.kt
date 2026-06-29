package com.schlueternetz.emacompanion.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AppConfigTest {

    @Test
    fun productionFetchInterval_isTenMinutes() {
        assertEquals(10 * 60 * 1000L, AppConfig.PRODUCTION_FETCH_INTERVAL_MS)
    }

    @Test
    fun moduleHealthCheckInterval_is24Hours() {
        assertEquals(24 * 60 * 60 * 1000L, AppConfig.MODULE_HEALTH_CHECK_INTERVAL_MS)
    }
}
