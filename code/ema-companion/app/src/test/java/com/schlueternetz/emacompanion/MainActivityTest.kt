package com.schlueternetz.emacompanion

import android.view.View
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun mainActivity_hasNavHostFragment() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        val navHost = activity.findViewById<View>(R.id.nav_host_fragment)
        assertNotNull(navHost)
    }

    @Test
    fun mainActivity_hasBottomNavigationView() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottom_nav)
        assertNotNull(bottomNav)
    }

    @Test
    fun mainActivity_bottomNavStartsOnHome() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottom_nav)
        assertEquals(R.id.homeFragment, bottomNav.selectedItemId)
    }

    @Test
    fun mainActivity_hasNoAccessibilityErrors() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create().start().resume().visible().get()
        AccessibilityValidator()
            .setThrowExceptionFor(AccessibilityCheckResult.AccessibilityCheckResultType.ERROR)
            .check(activity.window.decorView)
    }
}
