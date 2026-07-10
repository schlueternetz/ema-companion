package com.schlueternetz.emacompanion.feature.widgets

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.color.ColorProviders
import androidx.glance.material3.ColorProviders

/**
 * `null` means "use Glance's own day/night-aware default" (the `"system"` preference); a non-null
 * value forces that fixed scheme regardless of the device's current system theme (`"light"`/`"dark"`).
 */
fun widgetColorProviders(displayMode: String): ColorProviders? =
    when (displayMode) {
        "light" -> ColorProviders(lightColorScheme())
        "dark" -> ColorProviders(darkColorScheme())
        else -> null
    }

@Composable
fun WidgetTheme(
    displayMode: String,
    content: @Composable () -> Unit,
) {
    val colors = widgetColorProviders(displayMode)
    if (colors != null) {
        GlanceTheme(colors = colors, content = content)
    } else {
        GlanceTheme(content = content)
    }
}
