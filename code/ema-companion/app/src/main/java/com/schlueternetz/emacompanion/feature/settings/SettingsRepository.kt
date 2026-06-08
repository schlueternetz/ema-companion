package com.schlueternetz.emacompanion.feature.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SettingsRepository(private val prefs: SharedPreferences) {

    companion object {
        const val LANGUAGE_KEY = "language"
        const val LANGUAGE_DEFAULT = "system"

        fun create(context: Context): SettingsRepository {
            return try {
                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                SettingsRepository(
                    EncryptedSharedPreferences.create(
                        "ema_companion_settings",
                        masterKeyAlias,
                        context,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                    ),
                )
            } catch (e: Exception) {
                // Keystore unavailable (e.g., corrupted store or test environment);
                // fall back to plain SharedPreferences so the app remains functional.
                SettingsRepository(
                    context.getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE),
                )
            }
        }
    }

    fun getLanguage(): String = try {
        prefs.getString(LANGUAGE_KEY, LANGUAGE_DEFAULT) ?: LANGUAGE_DEFAULT
    } catch (e: SecurityException) {
        LANGUAGE_DEFAULT
    }

    fun setLanguage(language: String) {
        prefs.edit().putString(LANGUAGE_KEY, language).apply()
    }
}
