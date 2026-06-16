package com.schlueternetz.emacompanion.core.api

/** Current production power, in watts. */
data class ProductionSnapshot(val powerWatts: Int) {
    companion object {
        const val UNIT = "W"
    }
}
