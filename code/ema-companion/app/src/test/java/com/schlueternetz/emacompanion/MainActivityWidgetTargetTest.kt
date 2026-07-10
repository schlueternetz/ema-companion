package com.schlueternetz.emacompanion

import android.content.Context
import android.content.Intent
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.schlueternetz.emacompanion.feature.widgets.WidgetTapTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainActivityWidgetTargetTest {
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
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("emaAppId", "a".repeat(32))
            .putString("emaAppSecret", "b".repeat(12))
            .putString("emaSystemId", "c".repeat(16))
            .putString("emaEcuId", "1".repeat(12))
            .putFloat("systemCapacity", 4.5f)
            .apply()
    }

    private fun intentWithTarget(target: String): Intent =
        Intent(appContext, MainActivity::class.java).putExtra(WidgetTapTarget.EXTRA_WIDGET_TARGET, target)

    @Test
    fun widgetTargetSettings_selectsSettingsDestination() {
        val activity =
            Robolectric
                .buildActivity(MainActivity::class.java, intentWithTarget(WidgetTapTarget.TARGET_SETTINGS))
                .create()
                .get()
        val navController = (activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

        assertEquals(R.id.settingsFragment, navController.currentDestination?.id)
    }

    @Test
    fun widgetTargetHome_selectsHomeDestination() {
        val activity =
            Robolectric
                .buildActivity(MainActivity::class.java, intentWithTarget(WidgetTapTarget.TARGET_HOME))
                .create()
                .get()
        val navController = (activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

        assertEquals(R.id.homeFragment, navController.currentDestination?.id)
    }

    @Test
    fun widgetTargetExtra_isConsumedOnce_recreationDoesNotReNavigate() {
        val controller =
            Robolectric
                .buildActivity(MainActivity::class.java, intentWithTarget(WidgetTapTarget.TARGET_SETTINGS))
                .create()
        val activity = controller.get()
        val navController = (activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
        assertEquals(R.id.settingsFragment, navController.currentDestination?.id)
        assertNull(activity.intent.getStringExtra(WidgetTapTarget.EXTRA_WIDGET_TARGET))

        // User navigates elsewhere, then the Activity is recreated (e.g. rotation).
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottom_nav)
        NavigationUI.onNavDestinationSelected(bottomNav.menu.findItem(R.id.homeFragment), navController)
        assertEquals(R.id.homeFragment, navController.currentDestination?.id)

        controller.recreate()
        val recreatedActivity = controller.get()

        val recreatedNavController =
            (recreatedActivity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
        assertEquals(
            "recreation must not re-apply a consumed widget-target extra",
            R.id.homeFragment,
            recreatedNavController.currentDestination?.id,
        )
    }
}
