package com.schlueternetz.emacompanion.feature.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.api.ApiUsageRepository
import com.schlueternetz.emacompanion.core.api.ThrottleResettable
import com.schlueternetz.emacompanion.core.api.log.ApiCallLog
import com.schlueternetz.emacompanion.core.api.log.ApiCallLogRepository
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthRepository
import com.schlueternetz.emacompanion.core.email.EmailResult
import com.schlueternetz.emacompanion.core.email.EmailSender
import com.schlueternetz.emacompanion.core.email.GmailSmtpEmailSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date
import javax.crypto.AEADBadTagException

class SettingsFragment : Fragment() {

    private lateinit var repository: SettingsRepository
    private lateinit var usageRepository: ApiUsageRepository
    private lateinit var moduleHealthRepository: ModuleHealthRepository
    private lateinit var tileRepositories: List<ThrottleResettable>
    private lateinit var logRepository: ApiCallLogRepository
    private lateinit var languageValueView: TextView
    private lateinit var displayModeValueView: TextView
    private lateinit var arrayTimezoneValueView: TextView
    private lateinit var notificationsSwitch: MaterialSwitch
    private lateinit var emailAlertsSwitch: MaterialSwitch
    private lateinit var emailAlertsSetupRow: View
    private lateinit var emailAlertsStatusRow: View
    private lateinit var emailAlertsStatusText: TextView
    private lateinit var emailAddressInput: TextInputEditText
    private lateinit var emailPasswordInput: TextInputEditText
    private lateinit var emailAlertsError: TextView
    private var suppressEmailSwitchListener = false
    private lateinit var settingEmaAppId: SettingRowView
    private lateinit var settingEmaAppSecret: SettingRowView
    private lateinit var settingEmaSystemId: SettingRowView
    private lateinit var settingEmaEcuId: SettingRowView
    private lateinit var settingSystemCapacity: SettingRowView
    private lateinit var settingHistoricDays: SettingRowView
    private lateinit var settingApiRequestLimit: SettingRowView
    private lateinit var apiRequestProgressBar: LinearProgressIndicator
    private lateinit var apiRequestProgressLabel: TextView
    private lateinit var settingBaseUrl: SettingRowView
    private lateinit var logsList: LinearLayout
    private lateinit var logsEmptyState: View

    private var activeEditRow: SettingRowView? = null

    private var pendingExportJson: String? = null

    private val importLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { handleImport(it) }
        }

    private val exportLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let { handleExport(it) }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = SettingsRepository.create(requireContext())
        usageRepository = ApiUsageRepository.create(requireContext())
        moduleHealthRepository = ModuleHealthRepository.create(requireContext())
        tileRepositories = listOf(usageRepository, moduleHealthRepository)
        logRepository = ApiCallLogRepository.create(requireContext())

        settingEmaAppId = view.findViewById(R.id.setting_ema_app_id)
        settingEmaAppSecret = view.findViewById(R.id.setting_ema_app_secret)
        settingEmaSystemId = view.findViewById(R.id.setting_ema_system_id)
        settingEmaEcuId = view.findViewById(R.id.setting_ema_ecu_id)
        settingSystemCapacity = view.findViewById(R.id.setting_system_capacity)
        settingHistoricDays = view.findViewById(R.id.setting_historic_days)
        settingApiRequestLimit = view.findViewById(R.id.setting_api_request_limit)
        apiRequestProgressBar = view.findViewById(R.id.api_request_progress_bar)
        apiRequestProgressLabel = view.findViewById(R.id.api_request_progress_label)
        settingBaseUrl = view.findViewById(R.id.setting_base_url)
        logsList = view.findViewById(R.id.settings_logs_list)
        logsEmptyState = view.findViewById(R.id.settings_logs_empty_state)
        languageValueView = view.findViewById(R.id.settings_language_value)
        displayModeValueView = view.findViewById(R.id.settings_display_mode_value)
        arrayTimezoneValueView = view.findViewById(R.id.settings_array_timezone_value)
        notificationsSwitch = view.findViewById(R.id.settings_notifications_switch)
        emailAlertsSwitch = view.findViewById(R.id.settings_email_alerts_switch)
        emailAlertsSetupRow = view.findViewById(R.id.settings_email_alerts_setup_row)
        emailAlertsStatusRow = view.findViewById(R.id.settings_email_alerts_status_row)
        emailAlertsStatusText = view.findViewById(R.id.settings_email_alerts_status_text)
        emailAddressInput = view.findViewById(R.id.email_address_input)
        emailPasswordInput = view.findViewById(R.id.email_password_input)
        emailAlertsError = view.findViewById(R.id.settings_email_alerts_error)

        wireEmaAppId()
        wireEmaAppSecret()
        wireEmaSystemId()
        wireEmaEcuId()
        wireSystemCapacity()
        wireLanguage(view)
        wireDisplayMode(view)
        wireArrayTimezone(view)
        wireNotifications()
        wireEmailAlerts(view)
        wireHistoricDays()
        wireApiRequestLimit(view)
        wireBaseUrl(view)
        wireImportExportReset(view)
        wireExclusiveEditMode()
        refreshLogs()
    }

    private fun refreshLogs() {
        val logs = logRepository.getAll()
        logsList.removeAllViews()
        if (logs.isEmpty()) {
            logsEmptyState.visibility = View.VISIBLE
            return
        }
        logsEmptyState.visibility = View.GONE
        logs.forEach { logsList.addView(buildLogRow(it)) }
    }

    private fun buildLogRow(log: ApiCallLog): TextView {
        val time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(log.timestampMs))
        val status = getString(
            if (log.success) R.string.settings_logs_success else R.string.settings_logs_failure,
        )
        val summary = getString(
            R.string.settings_logs_row_summary,
            time,
            log.endpoint,
            log.durationMs,
            status,
        )
        val verticalPadding = (12 * resources.displayMetrics.density).toInt()
        val background = TypedValue().also {
            requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true)
        }.resourceId
        return TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            text = summary
            contentDescription = summary
            minHeight = (48 * resources.displayMetrics.density).toInt()
            setPadding(0, verticalPadding, 0, verticalPadding)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            setBackgroundResource(background)
            isClickable = true
            isFocusable = true
            setOnClickListener { showLogDetail(log) }
        }
    }

    private fun showLogDetail(log: ApiCallLog) {
        val message = buildString {
            append(getString(R.string.settings_logs_detail_request))
            append("\n")
            append(prettyPrint(log.requestText))
            append("\n\n")
            append(getString(R.string.settings_logs_detail_response))
            append("\n")
            append(prettyPrint(log.responseText))
        }
        AlertDialog.Builder(requireContext())
            .setTitle(log.endpoint)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun prettyPrint(text: String): String {
        val trimmed = text.trim()
        return try {
            when {
                trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
                trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
                else -> text
            }
        } catch (e: JSONException) {
            text
        }
    }

    private fun wireEmaAppId() {
        settingEmaAppId.label = getString(R.string.settings_ema_app_id_label)
        settingEmaAppId.isRequired = true
        settingEmaAppId.hintText = getString(R.string.settings_ema_app_id_hint)
        settingEmaAppId.value = repository.getEmaAppId()
        settingEmaAppId.errorMessage = getString(R.string.settings_ema_app_id_error)
        settingEmaAppId.validator = { it.matches(Regex("[a-zA-Z0-9]{32}")) }
        settingEmaAppId.onSave = { v ->
            repository.setEmaAppId(v.lowercase())
            settingEmaAppId.value = repository.getEmaAppId()
            invalidateApiThrottle()
            checkConfigurationAndUpdateNav()
        }
    }

    private fun wireEmaAppSecret() {
        settingEmaAppSecret.label = getString(R.string.settings_ema_app_secret_label)
        settingEmaAppSecret.isRequired = true
        settingEmaAppSecret.isMasked = true
        settingEmaAppSecret.hintText = getString(R.string.settings_ema_app_secret_hint)
        settingEmaAppSecret.value = repository.getEmaAppSecret()
        settingEmaAppSecret.errorMessage = getString(R.string.settings_ema_app_secret_error)
        settingEmaAppSecret.validator = { it.matches(Regex("[a-zA-Z0-9]{12}")) }
        settingEmaAppSecret.onSave = { v ->
            repository.setEmaAppSecret(v.lowercase())
            settingEmaAppSecret.value = repository.getEmaAppSecret()
            invalidateApiThrottle()
            checkConfigurationAndUpdateNav()
        }
    }

    private fun wireEmaSystemId() {
        settingEmaSystemId.label = getString(R.string.settings_ema_system_id_label)
        settingEmaSystemId.isRequired = true
        settingEmaSystemId.hintText = getString(R.string.settings_ema_system_id_hint)
        settingEmaSystemId.value = repository.getEmaSystemId()
        settingEmaSystemId.errorMessage = getString(R.string.settings_ema_system_id_error)
        settingEmaSystemId.validator = { it.matches(Regex("[a-zA-Z0-9]{16}")) }
        settingEmaSystemId.onSave = { v ->
            repository.setEmaSystemId(v.uppercase())
            settingEmaSystemId.value = repository.getEmaSystemId()
            invalidateApiThrottle()
            checkConfigurationAndUpdateNav()
        }
    }

    private fun wireEmaEcuId() {
        settingEmaEcuId.label = getString(R.string.settings_ema_ecu_id_label)
        settingEmaEcuId.isRequired = true
        settingEmaEcuId.hintText = getString(R.string.settings_ema_ecu_id_hint)
        settingEmaEcuId.value = repository.getEmaEcuId()
        settingEmaEcuId.keyboardType = InputType.TYPE_CLASS_NUMBER
        settingEmaEcuId.errorMessage = getString(R.string.settings_ema_ecu_id_error)
        settingEmaEcuId.validator = { it.matches(Regex("\\d{12}")) }
        settingEmaEcuId.onSave = { v ->
            repository.setEmaEcuId(v)
            settingEmaEcuId.value = repository.getEmaEcuId()
            invalidateApiThrottle()
            checkConfigurationAndUpdateNav()
        }
    }

    private fun wireSystemCapacity() {
        settingSystemCapacity.label = getString(R.string.settings_system_capacity_label)
        settingSystemCapacity.isRequired = true
        settingSystemCapacity.hintText = getString(R.string.settings_system_capacity_hint)
        settingSystemCapacity.suffix = getString(R.string.settings_system_capacity_suffix)
        val cap = repository.getSystemCapacity()
        settingSystemCapacity.value = if (cap == -1f) "" else "$cap"
        settingSystemCapacity.keyboardType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        settingSystemCapacity.errorMessage = getString(R.string.settings_system_capacity_error)
        settingSystemCapacity.validator = { input ->
            val f = input.toFloatOrNull()
            f != null && f > 0 && f <= SettingsRepository.SYSTEM_CAPACITY_MAX_KW && input.matches(Regex("\\d+(\\.\\d{1,2})?"))
        }
        settingSystemCapacity.onSave = { v ->
            repository.setSystemCapacity(v.toFloat())
            settingSystemCapacity.value = "${repository.getSystemCapacity()}"
            checkConfigurationAndUpdateNav()
        }
    }

    private fun wireHistoricDays() {
        settingHistoricDays.label = getString(R.string.settings_historic_days_label)
        settingHistoricDays.suffix = getString(R.string.settings_historic_days_suffix)
        settingHistoricDays.value = "${repository.getHistoricDataDays()}"
        settingHistoricDays.keyboardType = InputType.TYPE_CLASS_NUMBER
        settingHistoricDays.errorMessage = getString(R.string.settings_historic_days_error)
        settingHistoricDays.validator = { input -> input.toIntOrNull()?.let { it in 1..90 } ?: false }
        settingHistoricDays.onSave = { v ->
            repository.setHistoricDataDays(v.toInt())
            settingHistoricDays.value = "${repository.getHistoricDataDays()}"
        }
    }

    private fun wireApiRequestLimit(view: View) {
        settingApiRequestLimit.label = getString(R.string.settings_api_request_limit_label)
        settingApiRequestLimit.suffix = getString(R.string.settings_api_request_limit_suffix)
        settingApiRequestLimit.keyboardType = android.text.InputType.TYPE_CLASS_NUMBER
        settingApiRequestLimit.errorMessage = getString(R.string.settings_api_request_limit_error)
        settingApiRequestLimit.validator = { input ->
            input.toIntOrNull()?.let { it > 0 && it <= SettingsRepository.API_REQUEST_LIMIT_MAX_PER_MONTH } ?: false
        }
        settingApiRequestLimit.value = "${repository.getApiRequestLimit()}"
        updateApiRequestProgress()
        settingApiRequestLimit.onSave = { v ->
            repository.setApiRequestLimit(v.toInt())
            settingApiRequestLimit.value = "${repository.getApiRequestLimit()}"
            updateApiRequestProgress()
            checkConfigurationAndUpdateNav()
        }
        val apiResetBtn = view.findViewById<View>(R.id.setting_api_request_limit_reset)
        apiResetBtn.setOnClickListener {
            repository.setApiRequestLimit(SettingsRepository.API_REQUEST_LIMIT_DEFAULT)
            settingApiRequestLimit.value = "${repository.getApiRequestLimit()}"
            updateApiRequestProgress()
        }
        settingApiRequestLimit.onEditStateChanged = { editing ->
            apiResetBtn.isEnabled = !editing
            apiResetBtn.alpha = if (editing) 0.38f else 1f
        }
    }

    // A changed connection setting (credentials or base URL) means the next Home fetch should run
    // immediately with the new config, not wait out a throttle started by a prior attempt. Clearing
    // the stale error avoids showing the old failure until that fetch completes.
    private fun invalidateApiThrottle() {
        tileRepositories.forEach { it.resetThrottle() }
    }

    private fun updateApiRequestProgress() {
        val consumedRequests = usageRepository.getRequestCount()
        val limit = repository.getApiRequestLimit()
        val progress = if (limit <= 0) 0f else (consumedRequests.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
        apiRequestProgressBar.progress = (progress * 100).toInt()
        apiRequestProgressLabel.text = getString(
            R.string.settings_api_request_progress_label,
            consumedRequests,
            limit,
        )
    }

    private fun wireExclusiveEditMode() {
        val allRows = listOf(
            settingEmaAppId, settingEmaAppSecret, settingEmaSystemId, settingEmaEcuId,
            settingSystemCapacity, settingHistoricDays, settingApiRequestLimit, settingBaseUrl,
        )
        allRows.forEach { row ->
            val existing = row.onEditStateChanged
            row.onEditStateChanged = { editing ->
                if (editing) {
                    val prev = activeEditRow
                    activeEditRow = row
                    if (prev !== row) prev?.cancelEdit()
                } else {
                    if (activeEditRow === row) activeEditRow = null
                }
                existing?.invoke(editing)
            }
        }
    }

    private fun wireLanguage(view: View) {
        updateLanguageDisplay()
        view.findViewById<View>(R.id.settings_language_row).setOnClickListener { showLanguageDialog() }
    }

    private fun wireDisplayMode(view: View) {
        updateDisplayModeDisplay()
        view.findViewById<View>(R.id.settings_display_mode_row)
            .setOnClickListener { showDisplayModeDialog() }
    }

    private fun wireArrayTimezone(view: View) {
        updateArrayTimezoneDisplay()
        view.findViewById<View>(R.id.settings_array_timezone_row)
            .setOnClickListener { showArrayTimezoneDialog() }
    }

    private fun updateArrayTimezoneDisplay() {
        arrayTimezoneValueView.text = repository.getArrayTimezone()
    }

    private fun showArrayTimezoneDialog() {
        val allZones = java.util.TimeZone.getAvailableIDs().sorted().toTypedArray()
        val current = repository.getArrayTimezone()
        val currentIndex = allZones.indexOfFirst { it == current }.coerceAtLeast(0)
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_array_timezone_dialog_title)
            .setSingleChoiceItems(allZones, currentIndex) { dialog, which ->
                val selected = allZones[which]
                repository.setArrayTimezone(selected)
                updateArrayTimezoneDisplay()
                rescheduleModuleHealthWorker(selected)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun rescheduleModuleHealthWorker(timezoneId: String) {
        com.schlueternetz.emacompanion.feature.home.ModuleHealthWorker.schedule(
            requireContext(),
            timezoneId,
            androidx.work.ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
        )
    }

    private fun wireEmailAlerts(view: View) {
        updateEmailAlertsDisplay()
        emailAlertsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (suppressEmailSwitchListener) return@setOnCheckedChangeListener
            if (isChecked) {
                if (!repository.isEmailConfigured()) {
                    emailAlertsSetupRow.visibility = View.VISIBLE
                }
            } else {
                emailAlertsSetupRow.visibility = View.GONE
                emailAlertsStatusRow.visibility = View.GONE
                repository.setEmailAlertsEnabled(false)
            }
        }
        emailAlertsStatusRow.setOnClickListener { showDisableEmailAlertsDialog() }
        view.findViewById<View>(R.id.settings_email_alerts_open_google_account).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://myaccount.google.com/apppasswords")))
        }
        view.findViewById<View>(R.id.settings_email_alerts_verify_save).setOnClickListener {
            verifyAndSaveEmailCredentials()
        }
    }

    private fun updateEmailAlertsDisplay() {
        suppressEmailSwitchListener = true
        val configured = repository.isEmailConfigured()
        val enabled = repository.getEmailAlertsEnabled()
        emailAlertsSwitch.isChecked = configured && enabled
        if (configured && enabled) {
            emailAlertsStatusRow.visibility = View.VISIBLE
            emailAlertsStatusText.text = getString(R.string.email_alerts_enabled_for, repository.getEmailAddress())
            emailAlertsSetupRow.visibility = View.GONE
        } else {
            emailAlertsStatusRow.visibility = View.GONE
            emailAlertsSetupRow.visibility = View.GONE
        }
        suppressEmailSwitchListener = false
    }

    private fun showDisableEmailAlertsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.email_alerts_disable_title)
            .setMessage(R.string.email_alerts_disable_message)
            .setPositiveButton(R.string.email_alerts_disable_confirm) { _, _ ->
                repository.deleteEmailCredentials()
                repository.setEmailAlertsEnabled(false)
                requireContext().getSharedPreferences(
                    ModuleHealthRepository.PREFS_HEALTH, android.content.Context.MODE_PRIVATE,
                ).edit().remove(ModuleHealthRepository.KEY_LAST_EMAILED_STATUS).apply()
                suppressEmailSwitchListener = true
                emailAlertsSwitch.isChecked = false
                suppressEmailSwitchListener = false
                emailAlertsStatusRow.visibility = View.GONE
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun verifyAndSaveEmailCredentials() {
        val address = emailAddressInput.text?.toString()?.trim() ?: ""
        val password = emailPasswordInput.text?.toString() ?: ""
        if (address.isEmpty() || password.isEmpty()) return
        emailAlertsError.visibility = View.GONE
        val verifyBtn = requireView().findViewById<View>(R.id.settings_email_alerts_verify_save)
        verifyBtn.isEnabled = false
        val sender = emailSenderFactory?.invoke(address, password)
            ?: GmailSmtpEmailSender(from = address, appPassword = password)
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { sender.testConnection() }
            verifyBtn.isEnabled = true
            when (result) {
                EmailResult.Success -> {
                    repository.setEmailAddress(address)
                    repository.setEmailAppPassword(password)
                    repository.setEmailAlertsEnabled(true)
                    emailAlertsSetupRow.visibility = View.GONE
                    emailAlertsStatusRow.visibility = View.VISIBLE
                    emailAlertsStatusText.text = getString(R.string.email_alerts_enabled_for, address)
                }
                else -> {
                    emailAlertsError.visibility = View.VISIBLE
                    emailAlertsError.text = getString(R.string.email_alerts_connection_error)
                }
            }
        }
    }

    private fun wireNotifications() {
        notificationsSwitch.isChecked = repository.getNotificationsEnabled()
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            repository.setNotificationsEnabled(isChecked)
        }
    }

    private fun wireBaseUrl(view: View) {
        settingBaseUrl.label = getString(R.string.settings_base_url_label)
        settingBaseUrl.value = repository.getBaseUrl()
        settingBaseUrl.errorMessage = getString(R.string.settings_base_url_error)
        settingBaseUrl.validator = { isValidUrl(it) }
        settingBaseUrl.onSave = { v ->
            repository.setBaseUrl(v)
            settingBaseUrl.value = repository.getBaseUrl()
            invalidateApiThrottle()
        }
        val baseUrlResetBtn = view.findViewById<View>(R.id.setting_base_url_reset)
        baseUrlResetBtn.setOnClickListener {
            repository.setBaseUrl(SettingsRepository.BASE_URL_DEFAULT)
            settingBaseUrl.value = repository.getBaseUrl()
            invalidateApiThrottle()
        }
        settingBaseUrl.onEditStateChanged = { editing ->
            baseUrlResetBtn.isEnabled = !editing
            baseUrlResetBtn.alpha = if (editing) 0.38f else 1f
        }
    }

    private fun wireImportExportReset(view: View) {
        view.findViewById<View>(R.id.settings_import_button).setOnClickListener {
            importLauncher.launch("application/json")
        }
        view.findViewById<View>(R.id.settings_export_button).setOnClickListener {
            showExportDialog()
        }
        view.findViewById<View>(R.id.settings_factory_reset_button).setOnClickListener {
            showFactoryResetDialog()
        }
    }

    private fun updateLanguageDisplay() {
        val current = repository.getLanguage()
        languageValueView.text = languageDisplayName(current)
    }

    private fun languageDisplayName(code: String): String = when (code) {
        "en" -> getString(R.string.language_option_english)
        "de" -> getString(R.string.language_option_german)
        else -> getString(R.string.language_option_system)
    }

    private fun updateDisplayModeDisplay() {
        displayModeValueView.text = displayModeDisplayName(repository.getDisplayMode())
    }

    private fun displayModeDisplayName(mode: String): String = when (mode) {
        "light" -> getString(R.string.display_mode_option_light)
        "dark" -> getString(R.string.display_mode_option_dark)
        else -> getString(R.string.display_mode_option_system)
    }

    private fun showLanguageDialog() {
        val options = arrayOf(
            getString(R.string.language_option_system),
            getString(R.string.language_option_english),
            getString(R.string.language_option_german),
        )
        val codes = arrayOf("system", "en", "de")
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_language_dialog_title)
            .setItems(options) { _, which ->
                applyLanguage(codes[which], repository)
                updateLanguageDisplay()
            }
            .show()
    }

    private fun showDisplayModeDialog() {
        val options = arrayOf(
            getString(R.string.display_mode_option_system),
            getString(R.string.display_mode_option_light),
            getString(R.string.display_mode_option_dark),
        )
        val modes = arrayOf("system", "light", "dark")
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_display_mode_dialog_title)
            .setItems(options) { _, which ->
                applyDisplayMode(modes[which])
                updateDisplayModeDisplay()
            }
            .show()
    }

    fun applyLanguage(code: String, repo: SettingsRepository) {
        repo.setLanguage(code)
        val localeList = if (code == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(code)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    private fun applyDisplayMode(mode: String) {
        repository.setDisplayMode(mode)
        val nightMode = when (mode) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    private fun showExportDialog() {
        val options = arrayOf(
            getString(R.string.settings_export_no_encryption),
            getString(R.string.settings_export_encrypt_with_pin),
        )
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_export_dialog_title)
            .setItems(options) { _, which ->
                val json = repository.exportToJson()
                if (which == 0) {
                    pendingExportJson = json
                    exportLauncher.launch(getString(R.string.settings_export_filename))
                } else {
                    showPinDialog(getString(R.string.settings_pin_dialog_title_export)) { pin ->
                        pendingExportJson = SettingsCrypto.encrypt(json, pin)
                        exportLauncher.launch(getString(R.string.settings_export_filename))
                    }
                }
            }
            .show()
    }

    private fun showPinDialog(title: String, onPin: (String) -> Unit) {
        val editText = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            hint = getString(R.string.settings_pin_hint)
        }
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(editText)
            .setPositiveButton(android.R.string.ok) { _, _ -> onPin(editText.text.toString()) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        editText.setOnEditorActionListener { _, _, _ ->
            dialog.dismiss()
            onPin(editText.text.toString())
            true
        }
    }

    private fun showFactoryResetDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_factory_reset_dialog_title)
            .setMessage(R.string.settings_factory_reset_dialog_message)
            .setPositiveButton(R.string.settings_factory_reset_confirm) { _, _ ->
                repository.clearAll()
                usageRepository.clear()
                logRepository.clear()
                requireContext().getSharedPreferences(
                    ModuleHealthRepository.PREFS_HEALTH, android.content.Context.MODE_PRIVATE,
                ).edit().clear().apply()
                requireContext().getSharedPreferences(
                    ModuleHealthRepository.PREFS_DAILY, android.content.Context.MODE_PRIVATE,
                ).edit().clear().apply()
                refreshAllDisplayedValues()
            }
            .setNegativeButton(R.string.settings_factory_reset_cancel, null)
            .show()
    }

    internal fun refreshAllDisplayedValues() {
        settingEmaAppId.value = repository.getEmaAppId()
        settingEmaAppSecret.value = repository.getEmaAppSecret()
        settingEmaSystemId.value = repository.getEmaSystemId()
        settingEmaEcuId.value = repository.getEmaEcuId()
        val cap = repository.getSystemCapacity()
        settingSystemCapacity.value = if (cap == -1f) "" else "$cap"
        settingHistoricDays.value = "${repository.getHistoricDataDays()}"
        settingApiRequestLimit.value = "${repository.getApiRequestLimit()}"
        updateApiRequestProgress()
        refreshLogs()
        notificationsSwitch.isChecked = repository.getNotificationsEnabled()
        settingBaseUrl.value = repository.getBaseUrl()
        updateLanguageDisplay()
        updateDisplayModeDisplay()
        updateArrayTimezoneDisplay()
        updateEmailAlertsDisplay()
        // Apply the persisted theme and language, not just their labels. After an import
        // or factory reset the stored value changes but the effect would otherwise be
        // deferred until the next setting edit triggered an Activity recreate.
        applyDisplayMode(repository.getDisplayMode())
        applyLanguage(repository.getLanguage(), repository)
        // An import (or factory reset) can change the connection settings (App ID/Secret,
        // System/ECU ID, Base URL), so reset the throttle and clear the stale error just like a
        // manual edit does — otherwise the next Home visit keeps the old throttle and never
        // fetches with the imported credentials.
        invalidateApiThrottle()
        checkConfigurationAndUpdateNav()
    }

    private fun handleImport(uri: Uri) {
        val content = try {
            requireContext().contentResolver.openInputStream(uri)
                ?.bufferedReader()?.readText() ?: return
        } catch (e: Exception) {
            showSnackbar(getString(R.string.settings_import_error_unreadable))
            return
        }
        try {
            val json = JSONObject(content)
            repository.importFromJson(content)
            logImport(json)
            refreshAllDisplayedValues()
            showSnackbar(getString(R.string.settings_import_success))
            return
        } catch (e: JSONException) {
            // not plain JSON — try as encrypted
        }
        showPinDialog(getString(R.string.settings_pin_dialog_title_import)) { pin ->
            try {
                val decrypted = SettingsCrypto.decrypt(content, pin)
                val json = JSONObject(decrypted)
                repository.importFromJson(decrypted)
                logImport(json)
                refreshAllDisplayedValues()
                showSnackbar(getString(R.string.settings_import_success))
            } catch (e: AEADBadTagException) {
                showSnackbar(getString(R.string.settings_import_error_wrong_pin))
            } catch (e: Exception) {
                showSnackbar(getString(R.string.settings_import_error_unreadable))
            }
        }
    }

    private fun logImport(json: JSONObject) {
        val sensitiveKeys = setOf("emaAppSecret")
        val fields = json.keys().asSequence().joinToString(", ") { key ->
            if (key in sensitiveKeys) "$key=[hidden]" else key
        }
        logRepository.append(
            ApiCallLog(
                timestampMs = System.currentTimeMillis(),
                endpoint = "settings/import",
                durationMs = 0,
                success = true,
                requestText = "Imported fields: $fields",
                responseText = "Settings imported successfully",
            ),
        )
    }

    private fun handleExport(uri: Uri) {
        val json = pendingExportJson ?: return
        try {
            requireContext().contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
            }
            showSnackbar(getString(R.string.settings_export_success))
        } catch (e: Exception) {
            showSnackbar(getString(R.string.settings_export_error))
        } finally {
            pendingExportJson = null
        }
    }

    private fun showSnackbar(message: String) {
        view?.let { Snackbar.make(it, message, Snackbar.LENGTH_LONG).show() }
    }

    internal fun checkConfigurationAndUpdateNav() {
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
            ?: return
        val configured = repository.isConfigured()
        val menu = bottomNav.menu
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            item.isEnabled = configured ||
                item.itemId == R.id.settingsFragment ||
                item.itemId == R.id.userGuideFragment
        }
    }

    private fun isValidUrl(url: String): Boolean {
        if (url.length > SettingsRepository.BASE_URL_MAX_LENGTH) return false
        return try {
            val parsed = java.net.URL(url)
            parsed.protocol.isNotEmpty() && parsed.host.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        /** Test seam: inject a fake EmailSender for Verify & Save without real SMTP. */
        var emailSenderFactory: ((from: String, password: String) -> EmailSender)? = null
    }
}
