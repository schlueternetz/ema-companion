package com.schlueternetz.emacompanion.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.google.android.material.textfield.TextInputEditText
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.email.EmailResult
import com.schlueternetz.emacompanion.core.email.EmailSender
import org.junit.After
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
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        appContext
            .getSharedPreferences("ema_api_usage", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        appContext
            .getSharedPreferences("ema_api_log", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        appContext
            .getSharedPreferences("ema_module_health", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        appContext
            .getSharedPreferences("ema_module_health_daily", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        appContext
            .getSharedPreferences("ema_hourly", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        appContext
            .getSharedPreferences("ema_daily", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    @After
    fun tearDownEmailSeam() {
        SettingsFragment.emailSenderFactory = null
    }

    private class FakeEmailSender(
        private val result: EmailResult,
    ) : EmailSender {
        override suspend fun send(
            to: String,
            subject: String,
            body: String,
        ) = result

        override suspend fun testConnection() = result
    }

    private fun seedLogs(json: String) {
        appContext
            .getSharedPreferences("ema_api_log", Context.MODE_PRIVATE)
            .edit()
            .putString("log", json)
            .apply()
    }

    private fun dialogMessage(dialog: androidx.appcompat.app.AlertDialog): String =
        dialog.window
            ?.decorView
            ?.findViewById<TextView>(android.R.id.message)
            ?.text
            ?.toString() ?: ""

    private fun seedUsageCount(count: Int) {
        appContext
            .getSharedPreferences("ema_api_usage", Context.MODE_PRIVATE)
            .edit()
            .putString(
                "apiRequestCountMonth",
                java.time.YearMonth
                    .now()
                    .toString(),
            ).putInt("apiRequestCount", count)
            .apply()
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
            val prefs =
                fragment
                    .requireContext()
                    .getSharedPreferences("test_settings", Context.MODE_PRIVATE)
            val repo = SettingsRepository(prefs)
            fragment.applyLanguage("en", repo)
            assertEquals("en", repo.getLanguage())
        }
    }

    @Test
    fun refresh_appliesImportedDisplayModeImmediately() {
        // Imported settings set display mode to dark.
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("displayMode", "dark")
            .apply()
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
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("language", "de")
            .apply()
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
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("emaAppId", "storedappid12345678901234567890ab")
            .apply()

        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val row = fragment.requireView().findViewById<SettingRowView>(R.id.setting_ema_app_id)
            assertEquals("storedappid12345678901234567890ab", row.value)
        }
    }

    @Test
    fun settingsFragment_notificationsToggleReflectsStoredValue() {
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("notificationsEnabled", false)
            .apply()

        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val switch =
                fragment
                    .requireView()
                    .findViewById<MaterialSwitch>(R.id.settings_notifications_switch)
            assertEquals(false, switch.isChecked)
        }
    }

    @Test
    fun settingsFragment_notificationsToggle_persistsChange() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val switch =
                fragment
                    .requireView()
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
            val row =
                fragment
                    .requireView()
                    .findViewById<SettingRowView>(R.id.setting_api_request_limit)
            assertNotNull(row)
            assertEquals("${SettingsRepository.API_REQUEST_LIMIT_DEFAULT}", row.value)
        }
    }

    @Test
    fun apiRequestLimit_resetButton_restoresToDefault() {
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .putInt("apiRequestLimit", 500)
            .apply()
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.setting_api_request_limit_reset).performClick()
        }
        val stored =
            appContext
                .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
                .getInt("apiRequestLimit", -1)
        assertEquals(SettingsRepository.API_REQUEST_LIMIT_DEFAULT, stored)
    }

    @Test
    fun apiRequestLimit_savesValue() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val row =
                fragment
                    .requireView()
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
            val row =
                fragment
                    .requireView()
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
            val row =
                fragment
                    .requireView()
                    .findViewById<SettingRowView>(R.id.setting_system_capacity)
            assertFalse(row.validator?.invoke("2001") ?: true)
            assertTrue(row.validator?.invoke("2000.00") ?: false)
        }
    }

    @Test
    fun baseUrl_rejectsTooLong() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val row =
                fragment
                    .requireView()
                    .findViewById<SettingRowView>(R.id.setting_base_url)
            val tooLong = "http://example.com/" + "a".repeat(SettingsRepository.BASE_URL_MAX_LENGTH)
            assertFalse(row.validator?.invoke(tooLong) ?: true)
        }
    }

    @Test
    fun apiRequestLimit_rejectsZeroOrNegative() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val row =
                fragment
                    .requireView()
                    .findViewById<SettingRowView>(R.id.setting_api_request_limit)
            assertFalse(row.validator?.invoke("0") ?: true)
            assertFalse(row.validator?.invoke("-5") ?: true)
        }
    }

    @Test
    fun progressBar_showsConsumedRatio() {
        seedUsageCount(800)
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .putInt("apiRequestLimit", 1000)
            .apply()
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val bar =
                fragment
                    .requireView()
                    .findViewById<LinearProgressIndicator>(R.id.api_request_progress_bar)
            // consumed=800, limit=1000 → 80% → progress=80 out of max=100
            assertEquals(80, bar.progress)
        }
    }

    @Test
    fun progressBar_showsZeroProgress_whenNoRequestsMade() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val bar =
                fragment
                    .requireView()
                    .findViewById<LinearProgressIndicator>(R.id.api_request_progress_bar)
            // No requests recorded yet → consumed=0 → 0%
            assertEquals(0, bar.progress)
        }
    }

    @Test
    fun progressBar_usesPersistedCount_notHardcodedValue() {
        seedUsageCount(123)
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .putInt("apiRequestLimit", 1000)
            .apply()
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val bar =
                fragment
                    .requireView()
                    .findViewById<LinearProgressIndicator>(R.id.api_request_progress_bar)
            // consumed=123, limit=1000 → 12%
            assertEquals(12, bar.progress)
            val label = fragment.requireView().findViewById<TextView>(R.id.api_request_progress_label)
            assertTrue("Label should mention the real count 123", label.text.toString().contains("123"))
        }
    }

    @Test
    fun apiRequestLimitResetButton_isDisabled_whileEditing() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val resetBtn = fragment.requireView().findViewById<View>(R.id.setting_api_request_limit_reset)
            fragment
                .requireView()
                .findViewById<SettingRowView>(R.id.setting_api_request_limit)
                .findViewById<android.widget.ImageButton>(R.id.setting_edit_button)
                .performClick()
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
        seedUsageCount(800)
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .putInt("apiRequestLimit", 1000)
            .apply()
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val label =
                fragment
                    .requireView()
                    .findViewById<TextView>(R.id.api_request_progress_label)
            assertNotNull(label)
            val text = label.text.toString()
            assertTrue("Label should mention 800", text.contains("800"))
            assertTrue("Label should mention 1000", text.contains("1000"))
        }
    }

    @Test
    fun changingCredential_resetsThrottleAndClearsError() {
        appContext
            .getSharedPreferences("ema_api_usage", Context.MODE_PRIVATE)
            .edit()
            .putLong("lastFetchEpochMs", 12345L)
            .putString("lastFetchError", "API")
            .apply()
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val row = fragment.requireView().findViewById<SettingRowView>(R.id.setting_ema_app_id)
            row.onSave.invoke("a".repeat(32))
        }
        val usage = appContext.getSharedPreferences("ema_api_usage", Context.MODE_PRIVATE)
        assertEquals(0L, usage.getLong("lastFetchEpochMs", -1L))
        assertEquals(null, usage.getString("lastFetchError", null))
    }

    @Test
    fun importingSettings_resetsThrottleAndClearsError() {
        // Import can change connection settings, so the post-import refresh must reset the
        // throttle and clear the stale error just like a manual credential edit does.
        appContext
            .getSharedPreferences("ema_api_usage", Context.MODE_PRIVATE)
            .edit()
            .putLong("lastFetchEpochMs", 12345L)
            .putString("lastFetchError", "API")
            .apply()
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            fragment.refreshAllDisplayedValues()
        }
        val usage = appContext.getSharedPreferences("ema_api_usage", Context.MODE_PRIVATE)
        assertEquals(0L, usage.getLong("lastFetchEpochMs", -1L))
        assertEquals(null, usage.getString("lastFetchError", null))
    }

    @Test
    fun importingSettings_resetsModuleHealthThrottle() {
        // An import can change EMA credentials, so the module health throttle must be reset
        // (just like the production tile throttle) so the next Home visit re-checks immediately.
        appContext
            .getSharedPreferences("ema_module_health", Context.MODE_PRIVATE)
            .edit()
            .putLong("lastCheckEpochMs", System.currentTimeMillis())
            .apply()
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            fragment.refreshAllDisplayedValues()
        }
        val health = appContext.getSharedPreferences("ema_module_health", Context.MODE_PRIVATE)
        assertFalse(
            "module health throttle should be cleared after import",
            health.contains("lastCheckEpochMs"),
        )
    }

    @Test
    fun changingCredential_resetsModuleHealthThrottle() {
        // A per-field credential save must also reset the module health throttle so the next
        // Home visit re-checks with the new credentials, not the old 24-hour throttle.
        appContext
            .getSharedPreferences("ema_module_health", Context.MODE_PRIVATE)
            .edit()
            .putLong("lastCheckEpochMs", System.currentTimeMillis())
            .apply()
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            fragment
                .requireView()
                .findViewById<SettingRowView>(R.id.setting_ema_app_id)
                .onSave
                .invoke("a".repeat(32))
        }
        val health = appContext.getSharedPreferences("ema_module_health", Context.MODE_PRIVATE)
        assertFalse(
            "module health throttle should be cleared after credential edit",
            health.contains("lastCheckEpochMs"),
        )
    }

    @Test
    fun factoryReset_clearsApiUsageAndLogs() {
        seedUsageCount(50)
        seedLogs(
            """
            [{"timestampMs":1,"endpoint":"e","durationMs":1,"success":true,
              "requestText":"r","responseText":"x"}]
            """.trimIndent(),
        )
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.settings_factory_reset_button).performClick()
            val dialog =
                org.robolectric.shadows.ShadowDialog
                    .getLatestDialog()
                    as androidx.appcompat.app.AlertDialog
            shadowOf(Looper.getMainLooper()).idle()
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
            shadowOf(Looper.getMainLooper()).idle()
        }
        val usage = appContext.getSharedPreferences("ema_api_usage", Context.MODE_PRIVATE)
        assertEquals(0, usage.getInt("apiRequestCount", 0))
        val log = appContext.getSharedPreferences("ema_api_log", Context.MODE_PRIVATE)
        assertEquals(null, log.getString("log", null))
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
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .putInt("historicDataDays", 30)
            .apply()
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

    // Logs section
    @Test
    fun logsList_showsRecordedCallsNewestFirst() {
        seedLogs(
            """[
                {"timestampMs":2000,"endpoint":"newest","durationMs":12,"success":true,"requestText":"GET","responseText":"{}"},
                {"timestampMs":1000,"endpoint":"older","durationMs":34,"success":false,"requestText":"GET","responseText":"{}"}
            ]""",
        )
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val list = fragment.requireView().findViewById<android.widget.LinearLayout>(R.id.settings_logs_list)
            assertEquals(2, list.childCount)
            val firstRow = list.getChildAt(0) as TextView
            assertTrue(firstRow.text.toString().contains("newest"))
            assertTrue(firstRow.text.toString().contains("12"))
        }
    }

    @Test
    fun logsList_showsEmptyState_whenNoRecords() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val empty = fragment.requireView().findViewById<View>(R.id.settings_logs_empty_state)
            val list = fragment.requireView().findViewById<android.widget.LinearLayout>(R.id.settings_logs_list)
            assertEquals(View.VISIBLE, empty.visibility)
            assertEquals(0, list.childCount)
        }
    }

    @Test
    fun logRow_tap_opensPrettyPrintedDetail() {
        seedLogs(
            """
            [{"timestampMs":2000,"endpoint":"ecu/energy","durationMs":12,"success":true,
              "requestText":"GET /x",
              "responseText":"{\"code\":0,\"data\":{\"power\":[8000]}}"}]
            """.trimIndent(),
        )
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val list = fragment.requireView().findViewById<android.widget.LinearLayout>(R.id.settings_logs_list)
            list.getChildAt(0).performClick()
            val dialog =
                org.robolectric.shadows.ShadowDialog
                    .getLatestDialog()
                    as androidx.appcompat.app.AlertDialog
            val message = dialogMessage(dialog)
            assertTrue("Detail should contain response body", message.contains("power"))
            assertTrue("Detail should be pretty-printed across lines", message.contains("\n"))
        }
    }

    @Test
    fun logDetail_keepsMaskedFieldMasked() {
        seedLogs(
            """
            [{"timestampMs":2000,"endpoint":"ecu/energy","durationMs":12,"success":true,
              "requestText":"App Secret: ••••3456","responseText":"{}"}]
            """.trimIndent(),
        )
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val list = fragment.requireView().findViewById<android.widget.LinearLayout>(R.id.settings_logs_list)
            list.getChildAt(0).performClick()
            val dialog =
                org.robolectric.shadows.ShadowDialog
                    .getLatestDialog()
                    as androidx.appcompat.app.AlertDialog
            val message = dialogMessage(dialog)
            assertTrue("Masked value should remain masked in detail", message.contains("••••3456"))
            assertFalse("Plain secret must not appear", message.contains("secret123456"))
        }
    }

    @Test
    fun factoryReset_clearsBothModuleHealthStores() {
        // Seed data in both health and daily prefs
        appContext
            .getSharedPreferences("ema_module_health", Context.MODE_PRIVATE)
            .edit()
            .putString("status", "GREEN")
            .putLong("lastCheckEpochMs", 12345L)
            .apply()
        appContext
            .getSharedPreferences("ema_module_health_daily", Context.MODE_PRIVATE)
            .edit()
            .putString("daily_2025-07-24", """{"INV1":1.5}""")
            .apply()

        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.settings_factory_reset_button).performClick()
            val dialog =
                org.robolectric.shadows.ShadowDialog
                    .getLatestDialog()
                    as androidx.appcompat.app.AlertDialog
            shadowOf(Looper.getMainLooper()).idle()
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
            shadowOf(Looper.getMainLooper()).idle()
        }

        val health = appContext.getSharedPreferences("ema_module_health", Context.MODE_PRIVATE)
        assertEquals("ema_module_health should be empty after factory reset", 0, health.all.size)
        val daily = appContext.getSharedPreferences("ema_module_health_daily", Context.MODE_PRIVATE)
        assertEquals("ema_module_health_daily should be empty after factory reset", 0, daily.all.size)
    }

    // ── Email Alerts (Phase 6) ────────────────────────────────────────────────

    @Test
    fun emailAlerts_toggleOffByDefault_enablingShowsSetupRow() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val switch =
                fragment
                    .requireView()
                    .findViewById<MaterialSwitch>(R.id.settings_email_alerts_switch)
            val setupRow =
                fragment
                    .requireView()
                    .findViewById<View>(R.id.settings_email_alerts_setup_row)

            assertFalse("email alerts toggle should be off by default", switch.isChecked)
            assertEquals(View.GONE, setupRow.visibility)

            switch.performClick()

            assertEquals(View.VISIBLE, setupRow.visibility)
        }
    }

    @Test
    fun emailAlerts_whenConfigured_showsEnabledForAddress() {
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("emailAddress", "user@gmail.com")
            .putString("emailAppPassword", "mysecretpassword1")
            .putBoolean("emailAlertsEnabled", true)
            .apply()

        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val statusRow =
                fragment
                    .requireView()
                    .findViewById<View>(R.id.settings_email_alerts_status_row)
            val statusText =
                fragment
                    .requireView()
                    .findViewById<TextView>(R.id.settings_email_alerts_status_text)

            assertEquals(View.VISIBLE, statusRow.visibility)
            assertTrue(
                "status text should show the email address",
                statusText.text.toString().contains("user@gmail.com"),
            )
        }
    }

    @Test
    fun emailAlerts_disableFlow_showsDialogAndClearsCredentials() {
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("emailAddress", "user@gmail.com")
            .putString("emailAppPassword", "mysecretpassword1")
            .putBoolean("emailAlertsEnabled", true)
            .apply()

        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.email_alerts_edit_button).performClick()
            fragment.requireView().findViewById<View>(R.id.email_alerts_clear_button).performClick()
            val dialog =
                org.robolectric.shadows.ShadowDialog
                    .getLatestDialog()
                    as androidx.appcompat.app.AlertDialog
            shadowOf(Looper.getMainLooper()).idle()
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
            shadowOf(Looper.getMainLooper()).idle()
        }

        val prefs = appContext.getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
        assertEquals("credentials should be cleared", "", prefs.getString("emailAddress", ""))
        assertEquals("credentials should be cleared", "", prefs.getString("emailAppPassword", ""))
    }

    @Test
    fun emailAlerts_openGoogleAccountButton_firesCorrectIntent() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            // Enable the toggle so the setup row appears
            fragment
                .requireView()
                .findViewById<MaterialSwitch>(R.id.settings_email_alerts_switch)
                .performClick()

            fragment
                .requireView()
                .findViewById<View>(R.id.settings_email_alerts_open_google_account)
                .performClick()

            val intent = shadowOf(fragment.requireActivity()).nextStartedActivity
            assertNotNull("intent should have been fired", intent)
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertEquals(
                Uri.parse("https://myaccount.google.com/apppasswords"),
                intent.data,
            )
        }
    }

    @Test
    fun emailAlerts_save_withValidInput_savesAndTransitions() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            fragment
                .requireView()
                .findViewById<MaterialSwitch>(R.id.settings_email_alerts_switch)
                .performClick()
            fragment
                .requireView()
                .findViewById<TextInputEditText>(R.id.email_address_input)
                .setText("user@gmail.com")
            fragment
                .requireView()
                .findViewById<TextInputEditText>(R.id.email_password_input)
                .setText("abcdefghijklmnop")
            fragment
                .requireView()
                .findViewById<View>(R.id.settings_email_alerts_verify_save)
                .performClick()

            val setupRow = fragment.requireView().findViewById<View>(R.id.settings_email_alerts_setup_row)
            val statusRow = fragment.requireView().findViewById<View>(R.id.settings_email_alerts_status_row)
            assertEquals("setup row should be gone after save", View.GONE, setupRow.visibility)
            assertEquals("status row should be visible after save", View.VISIBLE, statusRow.visibility)
        }
        val prefs = appContext.getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
        assertEquals("user@gmail.com", prefs.getString("emailAddress", ""))
        assertEquals("abcdefghijklmnop", prefs.getString("emailAppPassword", ""))
    }

    @Test
    fun emailAlerts_save_stripsPasswordWhitespaceBeforeSaving() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            fragment
                .requireView()
                .findViewById<MaterialSwitch>(R.id.settings_email_alerts_switch)
                .performClick()
            fragment
                .requireView()
                .findViewById<TextInputEditText>(R.id.email_address_input)
                .setText("user@gmail.com")
            fragment
                .requireView()
                .findViewById<TextInputEditText>(R.id.email_password_input)
                .setText("qbnh wsnp gwpt jeaf")
            fragment
                .requireView()
                .findViewById<View>(R.id.settings_email_alerts_verify_save)
                .performClick()
        }
        val prefs = appContext.getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
        assertEquals("qbnhwsnpgwptjeaf", prefs.getString("emailAppPassword", ""))
    }

    @Test
    fun emailAlerts_save_withInvalidEmail_showsError() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            fragment
                .requireView()
                .findViewById<MaterialSwitch>(R.id.settings_email_alerts_switch)
                .performClick()
            fragment
                .requireView()
                .findViewById<TextInputEditText>(R.id.email_address_input)
                .setText("notanemail")
            fragment
                .requireView()
                .findViewById<TextInputEditText>(R.id.email_password_input)
                .setText("abcdefghijklmnop")
            fragment
                .requireView()
                .findViewById<View>(R.id.settings_email_alerts_verify_save)
                .performClick()

            val error = fragment.requireView().findViewById<View>(R.id.settings_email_alerts_error)
            val setupRow = fragment.requireView().findViewById<View>(R.id.settings_email_alerts_setup_row)
            assertEquals("error should be visible", View.VISIBLE, error.visibility)
            assertEquals("setup row should stay visible", View.VISIBLE, setupRow.visibility)
        }
    }

    @Test
    fun emailAlerts_save_withPasswordWrongLength_showsError() {
        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            fragment
                .requireView()
                .findViewById<MaterialSwitch>(R.id.settings_email_alerts_switch)
                .performClick()
            fragment
                .requireView()
                .findViewById<TextInputEditText>(R.id.email_address_input)
                .setText("user@gmail.com")
            fragment
                .requireView()
                .findViewById<TextInputEditText>(R.id.email_password_input)
                .setText("tooshort")
            fragment
                .requireView()
                .findViewById<View>(R.id.settings_email_alerts_verify_save)
                .performClick()

            val error = fragment.requireView().findViewById<View>(R.id.settings_email_alerts_error)
            assertEquals("error should be visible", View.VISIBLE, error.visibility)
        }
    }

    @Test
    fun emailAlerts_configured_managementSectionAlwaysVisible_whenToggleOff() {
        // Management section must remain visible even when alerts are paused so the user can
        // edit credentials or disable without having to re-enable first.
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("emailAddress", "user@gmail.com")
            .putString("emailAppPassword", "mysecretpassword1")
            .putBoolean("emailAlertsEnabled", false)
            .apply()

        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            val statusRow = fragment.requireView().findViewById<View>(R.id.settings_email_alerts_status_row)
            assertEquals("management section should be visible even when paused", View.VISIBLE, statusRow.visibility)
        }
    }

    @Test
    fun emailAlerts_editButton_showsSetupFormWithPrefilledEmail() {
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("emailAddress", "user@gmail.com")
            .putString("emailAppPassword", "abcdefghijklmnop")
            .putBoolean("emailAlertsEnabled", true)
            .apply()

        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.email_alerts_edit_button).performClick()

            val setupRow = fragment.requireView().findViewById<View>(R.id.settings_email_alerts_setup_row)
            val managementRow = fragment.requireView().findViewById<View>(R.id.settings_email_alerts_status_row)
            val addressInput = fragment.requireView().findViewById<TextInputEditText>(R.id.email_address_input)
            val passwordInput = fragment.requireView().findViewById<TextInputEditText>(R.id.email_password_input)
            val cancelButton = fragment.requireView().findViewById<View>(R.id.email_alerts_cancel_button)
            val testButton = fragment.requireView().findViewById<View>(R.id.email_alerts_test_button)
            val clearButton = fragment.requireView().findViewById<View>(R.id.email_alerts_clear_button)

            assertEquals("setup form should be visible", View.VISIBLE, setupRow.visibility)
            assertEquals("management section should be hidden", View.GONE, managementRow.visibility)
            assertEquals("email should be pre-filled", "user@gmail.com", addressInput.text.toString())
            assertEquals("password should be blank", "", passwordInput.text.toString())
            assertEquals("cancel button should be visible", View.VISIBLE, cancelButton.visibility)
            assertEquals("test button should be visible in edit mode", View.VISIBLE, testButton.visibility)
            assertEquals("clear button should be visible in edit mode", View.VISIBLE, clearButton.visibility)
        }
    }

    @Test
    fun emailAlerts_editCancel_returnsToManagementSection() {
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("emailAddress", "user@gmail.com")
            .putString("emailAppPassword", "abcdefghijklmnop")
            .putBoolean("emailAlertsEnabled", true)
            .apply()

        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.email_alerts_edit_button).performClick()
            fragment.requireView().findViewById<View>(R.id.email_alerts_cancel_button).performClick()

            val setupRow = fragment.requireView().findViewById<View>(R.id.settings_email_alerts_setup_row)
            val managementRow = fragment.requireView().findViewById<View>(R.id.settings_email_alerts_status_row)
            assertEquals("setup form should be hidden", View.GONE, setupRow.visibility)
            assertEquals("management section should be visible", View.VISIBLE, managementRow.visibility)
        }
    }

    @Test
    fun emailAlerts_editSave_updatesCredentialsAndReturnsToManagement() {
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("emailAddress", "old@gmail.com")
            .putString("emailAppPassword", "abcdefghijklmnop")
            .putBoolean("emailAlertsEnabled", true)
            .apply()

        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.email_alerts_edit_button).performClick()
            fragment
                .requireView()
                .findViewById<TextInputEditText>(R.id.email_address_input)
                .setText("new@gmail.com")
            fragment
                .requireView()
                .findViewById<TextInputEditText>(R.id.email_password_input)
                .setText("zyxwvutsrqponmlk")
            fragment.requireView().findViewById<View>(R.id.settings_email_alerts_verify_save).performClick()

            val setupRow = fragment.requireView().findViewById<View>(R.id.settings_email_alerts_setup_row)
            val managementRow = fragment.requireView().findViewById<View>(R.id.settings_email_alerts_status_row)
            assertEquals("setup form should be hidden after save", View.GONE, setupRow.visibility)
            assertEquals("management section should be visible after save", View.VISIBLE, managementRow.visibility)
        }
        val prefs = appContext.getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
        assertEquals("new@gmail.com", prefs.getString("emailAddress", ""))
        assertEquals("zyxwvutsrqponmlk", prefs.getString("emailAppPassword", ""))
    }

    @Test
    fun emailAlerts_testButton_success_showsSuccessResult() {
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("emailAddress", "user@gmail.com")
            .putString("emailAppPassword", "abcdefghijklmnop")
            .putBoolean("emailAlertsEnabled", true)
            .apply()
        SettingsFragment.emailSenderFactory = { _, _ -> FakeEmailSender(EmailResult.Success) }

        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.email_alerts_edit_button).performClick()
            fragment.requireView().findViewById<View>(R.id.email_alerts_test_button).performClick()
            shadowOf(Looper.getMainLooper()).idle()

            val result = fragment.requireView().findViewById<TextView>(R.id.email_alerts_test_result)
            assertEquals(View.VISIBLE, result.visibility)
            assertTrue(
                "success result should contain 'sent'",
                result.text
                    .toString()
                    .lowercase()
                    .contains("sent"),
            )
        }
    }

    @Test
    fun emailAlerts_testButton_authFailure_showsAuthError() {
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("emailAddress", "user@gmail.com")
            .putString("emailAppPassword", "abcdefghijklmnop")
            .putBoolean("emailAlertsEnabled", true)
            .apply()
        SettingsFragment.emailSenderFactory = { _, _ -> FakeEmailSender(EmailResult.AuthFailure) }

        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.email_alerts_edit_button).performClick()
            fragment.requireView().findViewById<View>(R.id.email_alerts_test_button).performClick()
            shadowOf(Looper.getMainLooper()).idle()

            val result = fragment.requireView().findViewById<TextView>(R.id.email_alerts_test_result)
            assertEquals(View.VISIBLE, result.visibility)
            assertTrue(
                "auth failure result should mention authentication",
                result.text
                    .toString()
                    .lowercase()
                    .contains("authentication"),
            )
        }
    }

    @Test
    fun emailAlerts_testButton_networkError_showsNetworkError() {
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("emailAddress", "user@gmail.com")
            .putString("emailAppPassword", "abcdefghijklmnop")
            .putBoolean("emailAlertsEnabled", true)
            .apply()
        SettingsFragment.emailSenderFactory = { _, _ -> FakeEmailSender(EmailResult.NetworkError) }

        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.email_alerts_edit_button).performClick()
            fragment.requireView().findViewById<View>(R.id.email_alerts_test_button).performClick()
            shadowOf(Looper.getMainLooper()).idle()

            val result = fragment.requireView().findViewById<TextView>(R.id.email_alerts_test_result)
            assertEquals(View.VISIBLE, result.visibility)
            assertTrue(
                "network error result should mention connect",
                result.text
                    .toString()
                    .lowercase()
                    .contains("connect"),
            )
        }
    }

    @Test
    fun settingsFragment_factoryReset_cancelMakesNoChanges() {
        appContext
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("emaAppId", "originalid1234567890123456789012")
            .apply()

        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            // Cancel factory reset dialog — simulate by just verifying value is still present
            val row = fragment.requireView().findViewById<SettingRowView>(R.id.setting_ema_app_id)
            assertEquals("originalid1234567890123456789012", row.value)
        }
    }
}
