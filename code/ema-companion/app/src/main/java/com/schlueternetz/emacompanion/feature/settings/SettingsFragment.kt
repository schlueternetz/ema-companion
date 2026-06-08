package com.schlueternetz.emacompanion.feature.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import com.schlueternetz.emacompanion.R

class SettingsFragment : Fragment() {

    private lateinit var repository: SettingsRepository
    private lateinit var languageValueView: TextView

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
        languageValueView = view.findViewById(R.id.settings_language_value)

        updateLanguageDisplay()

        view.findViewById<View>(R.id.settings_language_row).setOnClickListener {
            showLanguageDialog()
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

    fun applyLanguage(code: String, repo: SettingsRepository) {
        repo.setLanguage(code)
        val localeList = if (code == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(code)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
