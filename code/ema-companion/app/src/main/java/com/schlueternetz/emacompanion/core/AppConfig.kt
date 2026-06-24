package com.schlueternetz.emacompanion.core

import java.util.concurrent.TimeUnit

/** Developer-tunable timing constants. Change these to adjust polling frequency. */
object AppConfig {
    val PRODUCTION_FETCH_INTERVAL_MS: Long = TimeUnit.MINUTES.toMillis(10)
    val MODULE_HEALTH_CHECK_INTERVAL_MS: Long = TimeUnit.HOURS.toMillis(24)
}
