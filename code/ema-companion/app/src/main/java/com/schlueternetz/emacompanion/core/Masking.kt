package com.schlueternetz.emacompanion.core

/** Shared masking for sensitive values: keep the last 4 characters, replace the rest with dots. */
object Masking {
    fun mask(raw: String): String {
        if (raw.isEmpty()) return raw
        val visible = minOf(4, raw.length)
        return "•".repeat(raw.length - visible) + raw.takeLast(visible)
    }
}
