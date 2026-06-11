package com.schlueternetz.emacompanion

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainActivityTest {

    private lateinit var appContext: Context

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        appContext.getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    private fun configureSettings() {
        appContext.getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("emaAppId", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
            .putString("emaAppSecret", "bbbbbbbbbbbb")
            .putString("emaSystemId", "CCCCCCCCCCCCCCCC")
            .putString("emaEcuId", "123456789012")
            .putFloat("systemCapacity", 4.5f)
            .putInt("historicDataDays", 30)
            .apply()
    }

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
    fun mainActivity_bottomNavStartsOnHome_whenConfigured() {
        configureSettings()
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

    @Test
    fun mainActivity_unconfigured_navigatesToSettings() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottom_nav)
        assertEquals(R.id.settingsFragment, bottomNav.selectedItemId)
    }

    @Test
    fun mainActivity_unconfigured_disablesNonSettingsNavItems() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottom_nav)
        val menu = bottomNav.menu
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            if (item.itemId == R.id.settingsFragment) {
                assertTrue("Settings item should be enabled", item.isEnabled)
            } else {
                assertFalse("Non-settings item ${item.title} should be disabled", item.isEnabled)
            }
        }
    }

    @Test
    fun mainActivity_configured_leavesNavItemsEnabled() {
        configureSettings()
        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottom_nav)
        val menu = bottomNav.menu
        for (i in 0 until menu.size()) {
            assertTrue("Nav item ${menu.getItem(i).title} should be enabled", menu.getItem(i).isEnabled)
        }
    }
}
