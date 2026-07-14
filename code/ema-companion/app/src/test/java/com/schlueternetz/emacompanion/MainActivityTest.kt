package com.schlueternetz.emacompanion

import android.content.Context
import android.view.View
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.schlueternetz.emacompanion.feature.settings.SettingsFragment
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
        WorkManagerTestInitHelper.initializeTestWorkManager(appContext)
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    private fun configureSettings() {
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
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
        val activity =
            Robolectric
                .buildActivity(MainActivity::class.java)
                .create()
                .start()
                .resume()
                .visible()
                .get()
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
        val alwaysEnabled = setOf(R.id.settingsFragment, R.id.userGuideFragment, R.id.supportFragment)
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            if (item.itemId in alwaysEnabled) {
                assertTrue("${item.title} should be enabled when unconfigured", item.isEnabled)
            } else {
                assertFalse("${item.title} should be disabled when unconfigured", item.isEnabled)
            }
        }
    }

    @Test
    fun factoryReset_relockNavigation() {
        // Start unconfigured so SettingsFragment is immediately the primary fragment
        val activity =
            Robolectric
                .buildActivity(MainActivity::class.java)
                .create()
                .start()
                .resume()
                .visible()
                .get()
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottom_nav)

        // Simulate nav being enabled (as it would be after configuring the app)
        for (i in 0 until bottomNav.menu.size()) bottomNav.menu.getItem(i).isEnabled = true
        assertTrue("Home should appear enabled before factory reset", bottomNav.menu.findItem(R.id.homeFragment).isEnabled)

        // Get the SettingsFragment and simulate factory reset: clear prefs then re-evaluate nav
        val navHost = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val settingsFragment = navHost?.childFragmentManager?.primaryNavigationFragment as? SettingsFragment
        assertNotNull("SettingsFragment must be the primary fragment", settingsFragment)
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        settingsFragment?.checkConfigurationAndUpdateNav()

        // Home nav should now be disabled
        assertFalse("Home nav should be disabled after factory reset", bottomNav.menu.findItem(R.id.homeFragment).isEnabled)
    }

    @Test
    fun afterImport_withValidSettings_homeNavIsUnlocked() {
        val activity =
            Robolectric
                .buildActivity(MainActivity::class.java)
                .create()
                .start()
                .resume()
                .visible()
                .get()
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottom_nav)
        assertFalse("Home should be disabled when unconfigured", bottomNav.menu.findItem(R.id.homeFragment).isEnabled)

        configureSettings()

        val navHost = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val settingsFragment = navHost?.childFragmentManager?.primaryNavigationFragment as? SettingsFragment
        assertNotNull("SettingsFragment must be present", settingsFragment)
        settingsFragment!!.checkConfigurationAndUpdateNav()

        assertTrue("Home nav should be enabled after import with valid settings", bottomNav.menu.findItem(R.id.homeFragment).isEnabled)
    }

    @Test
    fun afterImport_withValidSettings_canTapHomeToNavigate() {
        // Reproduces the on-device bug: after importing valid settings while the app
        // started unconfigured, tapping Home in the bottom nav does nothing. This drives
        // the exact NavigationUI code path the bottom nav uses (onNavDestinationSelected),
        // which applies popUpTo/saveState/restoreState options that a bare navigate() does not.
        val activity =
            Robolectric
                .buildActivity(MainActivity::class.java)
                .create()
                .start()
                .resume()
                .visible()
                .get()
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottom_nav)
        val navController = (activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

        // Verify starting state: unconfigured app sits on settings with home on the back stack
        assertEquals("Should start at settingsFragment when unconfigured", R.id.settingsFragment, navController.currentDestination?.id)

        configureSettings()
        val navHost = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val settingsFragment = navHost?.childFragmentManager?.primaryNavigationFragment as? SettingsFragment
        settingsFragment!!.checkConfigurationAndUpdateNav()

        assertTrue("Home should be enabled", bottomNav.menu.findItem(R.id.homeFragment).isEnabled)

        // Simulate a user tap on Home through the same path BottomNavigationView uses.
        val homeItem = bottomNav.menu.findItem(R.id.homeFragment)
        NavigationUI.onNavDestinationSelected(homeItem, navController)

        assertEquals("Tapping Home should navigate to homeFragment", R.id.homeFragment, navController.currentDestination?.id)
    }

    @Test
    fun mainActivity_unconfigured_userGuideNavItemIsEnabled() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottom_nav)
        assertTrue(
            "User Guide nav item should be enabled even when unconfigured",
            bottomNav.menu.findItem(R.id.userGuideFragment).isEnabled,
        )
    }

    @Test
    fun mainActivity_unconfigured_supportNavItemIsEnabled() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottom_nav)
        assertTrue(
            "Support nav item should be enabled even when unconfigured",
            bottomNav.menu.findItem(R.id.supportFragment).isEnabled,
        )
    }

    @Test
    fun mainActivity_unconfigured_homeNavItemIsDisabled() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottom_nav)
        assertFalse(
            "Home nav item should be disabled when unconfigured",
            bottomNav.menu.findItem(R.id.homeFragment).isEnabled,
        )
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
