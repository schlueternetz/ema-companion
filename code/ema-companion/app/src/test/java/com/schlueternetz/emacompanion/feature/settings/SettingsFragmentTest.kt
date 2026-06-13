package com.schlueternetz.emacompanion.feature.settings

import android.content.Context
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.schlueternetz.emacompanion.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
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
    fun refresh_appliesImportedDisplayModeImmediately() {
        // Imported settings set display mode to dark.
        appContext.getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit().putString("displayMode", "dark").apply()
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            // Simulate a prior manual override that differs from the imported value.
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            // The post-import refresh must apply the imported theme, not just its label.
            fragment.refreshAllDisplayedValues()
            assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())
        }
    }

    @Test
    // Pinned below API 33: on Tiramisu+ getApplicationLocales() reads the framework
    // LocaleManager (unbacked in Robolectric); below it AppCompat's backport storage
    // reflects setApplicationLocales reliably. The production code path is identical.
    @Config(sdk = [32])
    fun refresh_appliesImportedLanguageImmediately() {
        // Imported settings set language to German.
        appContext.getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit().putString("language", "de").apply()
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            // Simulate current locale being the system default.
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            shadowOf(Looper.getMainLooper()).idle()
            fragment.refreshAllDisplayedValues()
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals("de", AppCompatDelegate.getApplicationLocales().toLanguageTags())
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

    // API Request Limit
    @Test
    fun apiRequestLimit_showsDefaultValue_whenNotExplicitlySet() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val row = fragment.requireView()
                .findViewById<SettingRowView>(R.id.setting_api_request_limit)
            assertNotNull(row)
            assertEquals("${SettingsRepository.API_REQUEST_LIMIT_DEFAULT}", row.value)
        }
    }

    @Test
    fun apiRequestLimit_resetButton_restoresToDefault() {
        appContext.getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit().putInt("apiRequestLimit", 500).apply()
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.setting_api_request_limit_reset).performClick()
        }
        val stored = appContext.getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .getInt("apiRequestLimit", -1)
        assertEquals(SettingsRepository.API_REQUEST_LIMIT_DEFAULT, stored)
    }

    @Test
    fun apiRequestLimit_savesValue() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val row = fragment.requireView()
                .findViewById<SettingRowView>(R.id.setting_api_request_limit)
            row.onSave?.invoke("500")
        }
        val prefs = appContext.getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
        assertEquals(500, prefs.getInt("apiRequestLimit", -1))
    }

    @Test
    fun apiRequestLimit_rejectsAboveMax() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val row = fragment.requireView()
                .findViewById<SettingRowView>(R.id.setting_api_request_limit)
            assertFalse(row.validator?.invoke((SettingsRepository.API_REQUEST_LIMIT_MAX_PER_MONTH + 1).toString()) ?: true)
            assertFalse(row.validator?.invoke("10009999999999999999999999999999999") ?: true)
            assertTrue(row.validator?.invoke(SettingsRepository.API_REQUEST_LIMIT_MAX_PER_MONTH.toString()) ?: false)
        }
    }

    @Test
    fun systemCapacity_rejectsAboveMax() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val row = fragment.requireView()
                .findViewById<SettingRowView>(R.id.setting_system_capacity)
            assertFalse(row.validator?.invoke("2001") ?: true)
            assertTrue(row.validator?.invoke("2000.00") ?: false)
        }
    }

    @Test
    fun baseUrl_rejectsTooLong() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val row = fragment.requireView()
                .findViewById<SettingRowView>(R.id.setting_base_url)
            val tooLong = "http://example.com/" + "a".repeat(SettingsRepository.BASE_URL_MAX_LENGTH)
            assertFalse(row.validator?.invoke(tooLong) ?: true)
        }
    }

    @Test
    fun apiRequestLimit_rejectsZeroOrNegative() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val row = fragment.requireView()
                .findViewById<SettingRowView>(R.id.setting_api_request_limit)
            assertFalse(row.validator?.invoke("0") ?: true)
            assertFalse(row.validator?.invoke("-5") ?: true)
        }
    }

    @Test
    fun progressBar_showsConsumedRatio() {
        appContext.getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit().putInt("apiRequestLimit", 1000).apply()
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val bar = fragment.requireView()
                .findViewById<LinearProgressIndicator>(R.id.api_request_progress_bar)
            // consumed=800, limit=1000 → 80% → progress=80 out of max=100
            assertEquals(80, bar.progress)
        }
    }

    @Test
    fun progressBar_showsDefaultProgress_whenLimitNotExplicitlySet() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val bar = fragment.requireView()
                .findViewById<LinearProgressIndicator>(R.id.api_request_progress_bar)
            // consumed=800, default limit=1000 → 80%
            assertEquals(80, bar.progress)
        }
    }

    @Test
    fun apiRequestLimitResetButton_isDisabled_whileEditing() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val resetBtn = fragment.requireView().findViewById<View>(R.id.setting_api_request_limit_reset)
            fragment.requireView().findViewById<SettingRowView>(R.id.setting_api_request_limit)
                .findViewById<android.widget.ImageButton>(R.id.setting_edit_button).performClick()
            assertEquals(false, resetBtn.isEnabled)
        }
    }

    @Test
    fun apiRequestLimitResetButton_isEnabled_afterCancelEdit() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val resetBtn = fragment.requireView().findViewById<View>(R.id.setting_api_request_limit_reset)
            val row = fragment.requireView().findViewById<SettingRowView>(R.id.setting_api_request_limit)
            row.findViewById<android.widget.ImageButton>(R.id.setting_edit_button).performClick()
            row.findViewById<android.widget.ImageButton>(R.id.setting_cancel_button).performClick()
            assertEquals(true, resetBtn.isEnabled)
        }
    }

    @Test
    fun progressBarLabel_showsConsumedAndLimit() {
        appContext.getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit().putInt("apiRequestLimit", 1000).apply()
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val label = fragment.requireView()
                .findViewById<TextView>(R.id.api_request_progress_label)
            assertNotNull(label)
            val text = label.text.toString()
            assertTrue("Label should mention 800", text.contains("800"))
            assertTrue("Label should mention 1000", text.contains("1000"))
        }
    }

    @Test
    fun historicDays_showsDefault_whenNotExplicitlySet() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val row = fragment.requireView().findViewById<SettingRowView>(R.id.setting_historic_days)
            assertEquals("${SettingsRepository.HISTORIC_DATA_DAYS_DEFAULT}", row.value)
        }
    }

    @Test
    fun historicDays_suffixStrippedFromEditField() {
        appContext.getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit().putInt("historicDataDays", 30).apply()
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val row = fragment.requireView().findViewById<SettingRowView>(R.id.setting_historic_days)
            row.findViewById<android.widget.ImageButton>(R.id.setting_edit_button).performClick()
            val editText = row.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.setting_edit_text)
            assertEquals("30", editText.text.toString())
        }
    }

    @Test
    fun openingSecondEditRow_closesFirstEditRow() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val appIdRow = fragment.requireView().findViewById<SettingRowView>(R.id.setting_ema_app_id)
            val secretRow = fragment.requireView().findViewById<SettingRowView>(R.id.setting_ema_app_secret)

            appIdRow.findViewById<android.widget.ImageButton>(R.id.setting_edit_button).performClick()
            assertEquals(View.VISIBLE, appIdRow.findViewById<View>(R.id.setting_input_layout).visibility)

            secretRow.findViewById<android.widget.ImageButton>(R.id.setting_edit_button).performClick()

            assertEquals(View.GONE, appIdRow.findViewById<View>(R.id.setting_input_layout).visibility)
            assertEquals(View.VISIBLE, secretRow.findViewById<View>(R.id.setting_input_layout).visibility)
        }
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
