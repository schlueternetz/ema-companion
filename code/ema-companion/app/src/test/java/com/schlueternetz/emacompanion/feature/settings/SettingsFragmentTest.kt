package com.schlueternetz.emacompanion.feature.settings

import android.content.Context
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.testing.launchFragmentInContainer
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
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

    @Before
    fun setUp() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
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
}
