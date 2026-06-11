package com.schlueternetz.emacompanion.feature.settings

import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.schlueternetz.emacompanion.R
import org.json.JSONException
import org.json.JSONObject
import javax.crypto.AEADBadTagException

class SettingsFragment : Fragment() {

    private lateinit var repository: SettingsRepository
    private lateinit var languageValueView: TextView
    private lateinit var displayModeValueView: TextView
    private lateinit var notificationsSwitch: MaterialSwitch
    private lateinit var settingEmaAppId: SettingRowView
    private lateinit var settingEmaAppSecret: SettingRowView
    private lateinit var settingEmaSystemId: SettingRowView
    private lateinit var settingEmaEcuId: SettingRowView
    private lateinit var settingSystemCapacity: SettingRowView
    private lateinit var settingHistoricDays: SettingRowView
    private lateinit var settingBaseUrl: SettingRowView

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

        settingEmaAppId = view.findViewById(R.id.setting_ema_app_id)
        settingEmaAppSecret = view.findViewById(R.id.setting_ema_app_secret)
        settingEmaSystemId = view.findViewById(R.id.setting_ema_system_id)
        settingEmaEcuId = view.findViewById(R.id.setting_ema_ecu_id)
        settingSystemCapacity = view.findViewById(R.id.setting_system_capacity)
        settingHistoricDays = view.findViewById(R.id.setting_historic_days)
        settingBaseUrl = view.findViewById(R.id.setting_base_url)
        languageValueView = view.findViewById(R.id.settings_language_value)
        displayModeValueView = view.findViewById(R.id.settings_display_mode_value)
        notificationsSwitch = view.findViewById(R.id.settings_notifications_switch)

        wireEmaAppId()
        wireEmaAppSecret()
        wireEmaSystemId()
        wireEmaEcuId()
        wireSystemCapacity()
        wireLanguage(view)
        wireDisplayMode(view)
        wireNotifications()
        wireHistoricDays()
        wireBaseUrl(view)
        wireImportExportReset(view)
    }

    private fun wireEmaAppId() {
        settingEmaAppId.label = getString(R.string.settings_ema_app_id_label)
        settingEmaAppId.value = repository.getEmaAppId()
        settingEmaAppId.errorMessage = getString(R.string.settings_ema_app_id_error)
        settingEmaAppId.validator = { it.matches(Regex("[a-zA-Z0-9]{32}")) }
        settingEmaAppId.onSave = { v ->
            repository.setEmaAppId(v.lowercase())
            settingEmaAppId.value = repository.getEmaAppId()
            checkConfigurationAndUpdateNav()
        }
    }

    private fun wireEmaAppSecret() {
        settingEmaAppSecret.label = getString(R.string.settings_ema_app_secret_label)
        settingEmaAppSecret.isMasked = true
        settingEmaAppSecret.value = repository.getEmaAppSecret()
        settingEmaAppSecret.errorMessage = getString(R.string.settings_ema_app_secret_error)
        settingEmaAppSecret.validator = { it.matches(Regex("[a-zA-Z0-9]{12}")) }
        settingEmaAppSecret.onSave = { v ->
            repository.setEmaAppSecret(v.lowercase())
            settingEmaAppSecret.value = repository.getEmaAppSecret()
            checkConfigurationAndUpdateNav()
        }
    }

    private fun wireEmaSystemId() {
        settingEmaSystemId.label = getString(R.string.settings_ema_system_id_label)
        settingEmaSystemId.value = repository.getEmaSystemId()
        settingEmaSystemId.errorMessage = getString(R.string.settings_ema_system_id_error)
        settingEmaSystemId.validator = { it.matches(Regex("[a-zA-Z0-9]{16}")) }
        settingEmaSystemId.onSave = { v ->
            repository.setEmaSystemId(v.uppercase())
            settingEmaSystemId.value = repository.getEmaSystemId()
            checkConfigurationAndUpdateNav()
        }
    }

    private fun wireEmaEcuId() {
        settingEmaEcuId.label = getString(R.string.settings_ema_ecu_id_label)
        settingEmaEcuId.value = repository.getEmaEcuId()
        settingEmaEcuId.keyboardType = InputType.TYPE_CLASS_NUMBER
        settingEmaEcuId.errorMessage = getString(R.string.settings_ema_ecu_id_error)
        settingEmaEcuId.validator = { it.matches(Regex("\\d{12}")) }
        settingEmaEcuId.onSave = { v ->
            repository.setEmaEcuId(v)
            settingEmaEcuId.value = repository.getEmaEcuId()
            checkConfigurationAndUpdateNav()
        }
    }

    private fun wireSystemCapacity() {
        val suffix = getString(R.string.settings_system_capacity_suffix)
        settingSystemCapacity.label = getString(R.string.settings_system_capacity_label)
        val cap = repository.getSystemCapacity()
        settingSystemCapacity.value = if (cap == -1f) "" else "$cap$suffix"
        settingSystemCapacity.keyboardType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        settingSystemCapacity.errorMessage = getString(R.string.settings_system_capacity_error)
        settingSystemCapacity.validator = { input ->
            val f = input.toFloatOrNull()
            f != null && f > 0 && f <= 999.99f && input.matches(Regex("\\d+(\\.\\d{1,2})?"))
        }
        settingSystemCapacity.onSave = { v ->
            repository.setSystemCapacity(v.toFloat())
            settingSystemCapacity.value = "${repository.getSystemCapacity()}$suffix"
            checkConfigurationAndUpdateNav()
        }
    }

    private fun wireHistoricDays() {
        val suffix = getString(R.string.settings_historic_days_suffix)
        settingHistoricDays.label = getString(R.string.settings_historic_days_label)
        val days = repository.getHistoricDataDays()
        settingHistoricDays.value = if (days == -1) "" else "$days$suffix"
        settingHistoricDays.keyboardType = InputType.TYPE_CLASS_NUMBER
        settingHistoricDays.errorMessage = getString(R.string.settings_historic_days_error)
        settingHistoricDays.validator = { input -> input.toIntOrNull()?.let { it in 1..90 } ?: false }
        settingHistoricDays.onSave = { v ->
            repository.setHistoricDataDays(v.toInt())
            val stored = repository.getHistoricDataDays()
            settingHistoricDays.value = "$stored$suffix"
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
        }
        view.findViewById<View>(R.id.setting_base_url_reset).setOnClickListener {
            repository.setBaseUrl(SettingsRepository.BASE_URL_DEFAULT)
            settingBaseUrl.value = repository.getBaseUrl()
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
            hint = getString(R.string.settings_pin_hint)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(editText)
            .setPositiveButton(android.R.string.ok) { _, _ -> onPin(editText.text.toString()) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showFactoryResetDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_factory_reset_dialog_title)
            .setMessage(R.string.settings_factory_reset_dialog_message)
            .setPositiveButton(R.string.settings_factory_reset_confirm) { _, _ ->
                repository.clearAll()
                refreshAllDisplayedValues()
            }
            .setNegativeButton(R.string.settings_factory_reset_cancel, null)
            .show()
    }

    private fun refreshAllDisplayedValues() {
        settingEmaAppId.value = repository.getEmaAppId()
        settingEmaAppSecret.value = repository.getEmaAppSecret()
        settingEmaSystemId.value = repository.getEmaSystemId()
        settingEmaEcuId.value = repository.getEmaEcuId()
        val cap = repository.getSystemCapacity()
        settingSystemCapacity.value =
            if (cap == -1f) "" else "$cap${getString(R.string.settings_system_capacity_suffix)}"
        val days = repository.getHistoricDataDays()
        settingHistoricDays.value =
            if (days == -1) "" else "$days${getString(R.string.settings_historic_days_suffix)}"
        notificationsSwitch.isChecked = repository.getNotificationsEnabled()
        settingBaseUrl.value = repository.getBaseUrl()
        updateLanguageDisplay()
        updateDisplayModeDisplay()
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
            JSONObject(content)
            repository.importFromJson(content)
            refreshAllDisplayedValues()
            showSnackbar(getString(R.string.settings_import_success))
            return
        } catch (e: JSONException) {
            // not plain JSON — try as encrypted
        }
        showPinDialog(getString(R.string.settings_pin_dialog_title_import)) { pin ->
            try {
                val decrypted = SettingsCrypto.decrypt(content, pin)
                repository.importFromJson(decrypted)
                refreshAllDisplayedValues()
                showSnackbar(getString(R.string.settings_import_success))
            } catch (e: AEADBadTagException) {
                showSnackbar(getString(R.string.settings_import_error_wrong_pin))
            } catch (e: Exception) {
                showSnackbar(getString(R.string.settings_import_error_unreadable))
            }
        }
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

    private fun checkConfigurationAndUpdateNav() {
        if (!repository.isConfigured()) return
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
            ?: return
        val menu = bottomNav.menu
        for (i in 0 until menu.size()) {
            menu.getItem(i).isEnabled = true
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            val parsed = java.net.URL(url)
            parsed.protocol.isNotEmpty() && parsed.host.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
