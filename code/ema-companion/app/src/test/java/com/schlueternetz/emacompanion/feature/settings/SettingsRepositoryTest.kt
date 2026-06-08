package com.schlueternetz.emacompanion.feature.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsRepositoryTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var repo: SettingsRepository

    @Before
    fun setUp() {
        prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("test_settings", Context.MODE_PRIVATE)
        repo = SettingsRepository(prefs)
    }

    @After
    fun tearDown() {
        prefs.edit().clear().apply()
    }

    @Test
    fun getLanguage_returnsSystem_whenNothingStored() {
        assertEquals(SettingsRepository.LANGUAGE_DEFAULT, repo.getLanguage())
    }

    @Test
    fun setLanguage_persistsAndReturnsValue() {
        repo.setLanguage("en")
        assertEquals("en", repo.getLanguage())
    }

    @Test
    fun setLanguage_overwritesPreviousValue() {
        repo.setLanguage("en")
        repo.setLanguage("de")
        assertEquals("de", repo.getLanguage())
    }

    @Test
    fun getLanguage_returnsDefault_onSecurityException() {
        val brokenPrefs = object : SharedPreferences by prefs {
            override fun getString(key: String?, defValue: String?): String? =
                throw SecurityException("Keystore unavailable")
        }
        val repoWithBrokenPrefs = SettingsRepository(brokenPrefs)
        assertEquals(SettingsRepository.LANGUAGE_DEFAULT, repoWithBrokenPrefs.getLanguage())
    }
}
