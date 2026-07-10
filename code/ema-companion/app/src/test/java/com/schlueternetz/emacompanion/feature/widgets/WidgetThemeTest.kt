package com.schlueternetz.emacompanion.feature.widgets

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WidgetThemeTest {
    @Test
    fun systemMode_usesDayNightDefault_noFixedColorsReturned() {
        assertNull(widgetColorProviders("system"))
    }

    @Test
    fun lightMode_returnsFixedLightColorProviders() {
        assertNotNull(widgetColorProviders("light"))
    }

    @Test
    fun darkMode_returnsFixedDarkColorProviders() {
        assertNotNull(widgetColorProviders("dark"))
    }
}
