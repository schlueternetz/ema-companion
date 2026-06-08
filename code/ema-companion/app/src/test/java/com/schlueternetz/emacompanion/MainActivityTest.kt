package com.schlueternetz.emacompanion

import android.widget.TextView
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainActivityTest {

    @Test
    fun mainActivity_launchesSuccessfully() {
        Robolectric.buildActivity(MainActivity::class.java).create().get()
    }

    @Test
    fun mainActivity_showsEnglishHelloWorld() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        val textView = activity.findViewById<TextView>(R.id.text_hello)
        assertEquals("Hello World!", textView.text.toString())
    }

    @Test
    @Config(qualifiers = "de")
    fun mainActivity_showsGermanHelloWorld() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        val textView = activity.findViewById<TextView>(R.id.text_hello)
        assertEquals("Hallo Welt!", textView.text.toString())
    }

    @Test
    fun mainActivity_hasNoAccessibilityErrors() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create().start().resume().visible().get()
        // Throws AccessibilityViewCheckException if any ERROR-level violations are found
        AccessibilityValidator()
            .setThrowExceptionFor(AccessibilityCheckResult.AccessibilityCheckResultType.ERROR)
            .check(activity.window.decorView)
    }
}
