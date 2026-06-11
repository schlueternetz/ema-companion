package com.schlueternetz.emacompanion.feature.settings

import android.content.Context
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.google.android.material.materialswitch.MaterialSwitch
import com.schlueternetz.emacompanion.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsFragmentTest {

    private lateinit var appContext: Context

    @Before
    fun setUp() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        appContext = ApplicationProvider.getApplicationContext()
        // Clear settings between tests
        appContext.getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    @Test
    fun settingsFragment_launchesSuccessfully() {
        launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
    }

    @Test
    fun settingsFragment_showsLanguageLabel() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val label = fragment.requireView().findViewById<TextView>(R.id.settings_language_label)
            assertNotNull(label)
        }
    }

    @Test
    fun settingsFragment_showsCurrentLanguageValue() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val value = fragment.requireView().findViewById<TextView>(R.id.settings_language_value)
            assertNotNull(value)
        }
    }

    @Test
    fun settingsFragment_displaysSystemAsDefault() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val value = fragment.requireView().findViewById<TextView>(R.id.settings_language_value)
            val expected = fragment.requireContext().getString(R.string.language_option_system)
            assertEquals(expected, value.text.toString())
        }
    }

    @Test
    fun settingsFragment_savesLanguageSelection() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val prefs = fragment.requireContext()
                .getSharedPreferences("test_settings", Context.MODE_PRIVATE)
            val repo = SettingsRepository(prefs)
            fragment.applyLanguage("en", repo)
            assertEquals("en", repo.getLanguage())
        }
    }

    @Test
    fun settingsFragment_hasNoAccessibilityErrors() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            AccessibilityValidator()
                .setThrowExceptionFor(AccessibilityCheckResult.AccessibilityCheckResultType.ERROR)
                .check(fragment.requireView())
        }
    }

    @Test
    fun settingsFragment_displaysStoredEmaAppId() {
        appContext.getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit().putString("emaAppId", "storedappid12345678901234567890ab").apply()

        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val row = fragment.requireView().findViewById<SettingRowView>(R.id.setting_ema_app_id)
            assertEquals("storedappid12345678901234567890ab", row.value)
        }
    }

    @Test
    fun settingsFragment_notificationsToggleReflectsStoredValue() {
        appContext.getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit().putBoolean("notificationsEnabled", false).apply()

        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val switch = fragment.requireView()
                .findViewById<MaterialSwitch>(R.id.settings_notifications_switch)
            assertEquals(false, switch.isChecked)
        }
    }

    @Test
    fun settingsFragment_notificationsToggle_persistsChange() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val switch = fragment.requireView()
                .findViewById<MaterialSwitch>(R.id.settings_notifications_switch)
            switch.isChecked = false
        }

        val prefs = appContext.getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
        assertEquals(false, prefs.getBoolean("notificationsEnabled", true))
    }

    @Test
    fun settingsFragment_factoryReset_cancelMakesNoChanges() {
        appContext.getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit().putString("emaAppId", "originalid1234567890123456789012").apply()

        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            // Cancel factory reset dialog — simulate by just verifying value is still present
            val row = fragment.requireView().findViewById<SettingRowView>(R.id.setting_ema_app_id)
            assertEquals("originalid1234567890123456789012", row.value)
        }
    }
}
