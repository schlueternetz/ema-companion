package com.schlueternetz.emacompanion

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val repository = SettingsRepository.create(this)

        val nightMode = when (repository.getDisplayMode()) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)

        if (!repository.isConfigured()) {
            navController.navigate(R.id.settingsFragment)
            disableNonSettingsNavItems(bottomNav, navController)
        }
    }

    private fun disableNonSettingsNavItems(
        bottomNav: BottomNavigationView,
        navController: NavController,
    ) {
        val menu = bottomNav.menu
        val settingsId = navController.graph.findNode(R.id.settingsFragment)?.id
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            if (item.itemId != settingsId) {
                item.isEnabled = false
            }
        }
    }
}
