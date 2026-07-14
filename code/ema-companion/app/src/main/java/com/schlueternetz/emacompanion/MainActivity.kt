package com.schlueternetz.emacompanion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.schlueternetz.emacompanion.core.api.ApiSyncScheduler
import com.schlueternetz.emacompanion.feature.home.ModuleHealthNotifier
import com.schlueternetz.emacompanion.feature.home.ModuleHealthWorker
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import com.schlueternetz.emacompanion.feature.widgets.WidgetTapTarget

class MainActivity : AppCompatActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Result is intentional no-op: the background check and status persist regardless.
            // The user will simply not receive notifications if they deny.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val repository = SettingsRepository.create(this)

        val nightMode =
            when (repository.getDisplayMode()) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        AppCompatDelegate.setDefaultNightMode(nightMode)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val configured = repository.isConfigured()
        if (!configured) {
            // Make Settings the start destination so the back stack is just [settings].
            // Pushing settings on top of the home start destination instead leaves an
            // orphaned [home, settings] stack that breaks bottom-nav navigation back to
            // Home once the app is configured.
            val graph = navController.navInflater.inflate(R.navigation.nav_graph)
            graph.setStartDestination(R.id.settingsFragment)
            navController.graph = graph
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = systemBars.bottom)
            insets
        }

        if (!configured) {
            applyUnconfiguredNavState(bottomNav)
        }

        applyWidgetTargetExtra(bottomNav, navController)

        ModuleHealthNotifier.ensureChannelCreated(this)
        ModuleHealthWorker.schedule(this, repository.getArrayTimezone())
        ApiSyncScheduler.schedulePeriodic(this)
        requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun applyUnconfiguredNavState(bottomNav: BottomNavigationView) {
        val menu = bottomNav.menu
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            item.isEnabled =
                item.itemId == R.id.settingsFragment ||
                item.itemId == R.id.userGuideFragment ||
                item.itemId == R.id.supportFragment
        }
    }

    // Consumed exactly once: cleared immediately after reading so a later recreation
    // (e.g. rotation) does not re-trigger this navigation.
    private fun applyWidgetTargetExtra(
        bottomNav: BottomNavigationView,
        navController: NavController,
    ) {
        val target = intent.getStringExtra(WidgetTapTarget.EXTRA_WIDGET_TARGET) ?: return
        intent.removeExtra(WidgetTapTarget.EXTRA_WIDGET_TARGET)
        val itemId =
            when (target) {
                WidgetTapTarget.TARGET_SETTINGS -> R.id.settingsFragment
                WidgetTapTarget.TARGET_HOME -> R.id.homeFragment
                else -> return
            }
        val item = bottomNav.menu.findItem(itemId) ?: return
        NavigationUI.onNavDestinationSelected(item, navController)
        bottomNav.selectedItemId = itemId
    }
}
