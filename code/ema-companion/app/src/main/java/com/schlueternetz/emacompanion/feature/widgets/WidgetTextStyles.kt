package com.schlueternetz.emacompanion.feature.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceTheme
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle

/** Shared type scale for widget label/value pairs and titles, kept consistent across the three home-screen widgets. */
object WidgetTextStyles {
    val header: TextStyle
        @Composable get() = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)

    val value: TextStyle
        @Composable get() =
            TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)

    val title: TextStyle
        @Composable get() =
            TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
}
