package com.schlueternetz.emacompanion.feature.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    // EMA App ID
    @Test
    fun getEmaAppId_returnsEmpty_whenNothingStored() {
        assertEquals("", repo.getEmaAppId())
    }

    @Test
    fun setEmaAppId_persistsAndReturnsValue() {
        repo.setEmaAppId("abc123")
        assertEquals("abc123", repo.getEmaAppId())
    }

    // EMA App Secret
    @Test
    fun getEmaAppSecret_returnsEmpty_whenNothingStored() {
        assertEquals("", repo.getEmaAppSecret())
    }

    @Test
    fun setEmaAppSecret_persistsAndReturnsValue() {
        repo.setEmaAppSecret("secret")
        assertEquals("secret", repo.getEmaAppSecret())
    }

    // EMA System ID
    @Test
    fun getEmaSystemId_returnsEmpty_whenNothingStored() {
        assertEquals("", repo.getEmaSystemId())
    }

    @Test
    fun setEmaSystemId_persistsAndReturnsValue() {
        repo.setEmaSystemId("SYS1234567890ABCD")
        assertEquals("SYS1234567890ABCD", repo.getEmaSystemId())
    }

    // EMA ECU ID
    @Test
    fun getEmaEcuId_returnsEmpty_whenNothingStored() {
        assertEquals("", repo.getEmaEcuId())
    }

    @Test
    fun setEmaEcuId_persistsAndReturnsValue() {
        repo.setEmaEcuId("123456789012")
        assertEquals("123456789012", repo.getEmaEcuId())
    }

    // System Capacity
    @Test
    fun getSystemCapacity_returnsSentinel_whenNothingStored() {
        assertEquals(-1f, repo.getSystemCapacity(), 0.001f)
    }

    @Test
    fun setSystemCapacity_persistsAndReturnsValue() {
        repo.setSystemCapacity(4.56f)
        assertEquals(4.56f, repo.getSystemCapacity(), 0.001f)
    }

    // Historic Data Days
    @Test
    fun getHistoricDataDays_returnsSentinel_whenNothingStored() {
        assertEquals(-1, repo.getHistoricDataDays())
    }

    @Test
    fun setHistoricDataDays_persistsAndReturnsValue() {
        repo.setHistoricDataDays(30)
        assertEquals(30, repo.getHistoricDataDays())
    }

    // Notifications Enabled
    @Test
    fun getNotificationsEnabled_returnsTrue_whenNothingStored() {
        assertEquals(true, repo.getNotificationsEnabled())
    }

    @Test
    fun setNotificationsEnabled_persistsAndReturnsValue() {
        repo.setNotificationsEnabled(false)
        assertEquals(false, repo.getNotificationsEnabled())
    }

    // Base URL
    @Test
    fun getBaseUrl_returnsDefault_whenNothingStored() {
        assertEquals(SettingsRepository.BASE_URL_DEFAULT, repo.getBaseUrl())
    }

    @Test
    fun setBaseUrl_persistsAndReturnsValue() {
        repo.setBaseUrl("http://localhost:8080")
        assertEquals("http://localhost:8080", repo.getBaseUrl())
    }

    // Display Mode
    @Test
    fun getDisplayMode_returnsSystem_whenNothingStored() {
        assertEquals(SettingsRepository.DISPLAY_MODE_DEFAULT, repo.getDisplayMode())
    }

    @Test
    fun setDisplayMode_persistsAndReturnsValue() {
        repo.setDisplayMode("dark")
        assertEquals("dark", repo.getDisplayMode())
    }

    // isConfigured
    @Test
    fun isConfigured_returnsTrue_whenAllRequiredFieldsPresent() {
        repo.setEmaAppId("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        repo.setEmaAppSecret("bbbbbbbbbbbb")
        repo.setEmaSystemId("CCCCCCCCCCCCCCCC")
        repo.setEmaEcuId("123456789012")
        repo.setSystemCapacity(4.5f)
        assertEquals(true, repo.isConfigured())
    }

    @Test
    fun isConfigured_returnsFalse_whenEmaAppIdEmpty() {
        repo.setEmaAppSecret("bbbbbbbbbbbb")
        repo.setEmaSystemId("CCCCCCCCCCCCCCCC")
        repo.setEmaEcuId("123456789012")
        repo.setSystemCapacity(4.5f)
        assertEquals(false, repo.isConfigured())
    }

    @Test
    fun isConfigured_returnsFalse_whenEmaAppSecretEmpty() {
        repo.setEmaAppId("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        repo.setEmaSystemId("CCCCCCCCCCCCCCCC")
        repo.setEmaEcuId("123456789012")
        repo.setSystemCapacity(4.5f)
        assertEquals(false, repo.isConfigured())
    }

    @Test
    fun isConfigured_returnsFalse_whenEmaSystemIdEmpty() {
        repo.setEmaAppId("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        repo.setEmaAppSecret("bbbbbbbbbbbb")
        repo.setEmaEcuId("123456789012")
        repo.setSystemCapacity(4.5f)
        assertEquals(false, repo.isConfigured())
    }

    @Test
    fun isConfigured_returnsFalse_whenEmaEcuIdEmpty() {
        repo.setEmaAppId("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        repo.setEmaAppSecret("bbbbbbbbbbbb")
        repo.setEmaSystemId("CCCCCCCCCCCCCCCC")
        repo.setSystemCapacity(4.5f)
        assertEquals(false, repo.isConfigured())
    }

    @Test
    fun isConfigured_returnsFalse_whenSystemCapacityIsSentinel() {
        repo.setEmaAppId("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        repo.setEmaAppSecret("bbbbbbbbbbbb")
        repo.setEmaSystemId("CCCCCCCCCCCCCCCC")
        repo.setEmaEcuId("123456789012")
        assertEquals(false, repo.isConfigured())
    }

    // exportToJson
    @Test
    fun exportToJson_containsAllTenKeys() {
        repo.setLanguage("en")
        repo.setEmaAppId("abc")
        repo.setEmaAppSecret("secret")
        repo.setEmaSystemId("SYSID")
        repo.setEmaEcuId("123456789012")
        repo.setSystemCapacity(4.5f)
        repo.setHistoricDataDays(30)
        repo.setNotificationsEnabled(false)
        repo.setBaseUrl("http://test.com")
        repo.setDisplayMode("dark")

        val json = repo.exportToJson()
        val obj = JSONObject(json)

        assertTrue(obj.has("language"))
        assertTrue(obj.has("emaAppId"))
        assertTrue(obj.has("emaAppSecret"))
        assertTrue(obj.has("emaSystemId"))
        assertTrue(obj.has("emaEcuId"))
        assertTrue(obj.has("systemCapacity"))
        assertTrue(obj.has("historicDataDays"))
        assertTrue(obj.has("notificationsEnabled"))
        assertTrue(obj.has("baseUrl"))
        assertTrue(obj.has("displayMode"))
    }

    @Test
    fun exportToJson_valuesMatchStored() {
        repo.setEmaAppId("myid")
        repo.setDisplayMode("dark")

        val json = repo.exportToJson()
        val obj = JSONObject(json)

        assertEquals("myid", obj.getString("emaAppId"))
        assertEquals("dark", obj.getString("displayMode"))
    }

    // importFromJson
    @Test
    fun importFromJson_mergesRecognizedKeys() {
        repo.setEmaAppId("oldid")
        repo.importFromJson("""{"emaAppId":"newid"}""")
        assertEquals("newid", repo.getEmaAppId())
    }

    @Test
    fun importFromJson_leavesAbsentKeysUnchanged() {
        repo.setDisplayMode("dark")
        repo.importFromJson("""{"emaAppId":"abc"}""")
        assertEquals("dark", repo.getDisplayMode())
    }

    @Test
    fun importFromJson_ignoresUnrecognizedKeys() {
        repo.importFromJson("""{"unknownKey":"value"}""")
        // Should not throw; known fields retain defaults
        assertEquals("", repo.getEmaAppId())
    }

    @Test(expected = IllegalArgumentException::class)
    fun importFromJson_throwsOnMalformedJson() {
        repo.importFromJson("not valid json {{{")
    }

    @Test
    fun importFromJson_importsAllTenFields() {
        repo.importFromJson(
            """{"language":"de","emaAppId":"a","emaAppSecret":"b","emaSystemId":"c",
            "emaEcuId":"d","systemCapacity":5.5,"historicDataDays":45,
            "notificationsEnabled":false,"baseUrl":"http://x.com","displayMode":"light"}""",
        )
        assertEquals("de", repo.getLanguage())
        assertEquals("a", repo.getEmaAppId())
        assertEquals("b", repo.getEmaAppSecret())
        assertEquals("c", repo.getEmaSystemId())
        assertEquals("d", repo.getEmaEcuId())
        assertEquals(5.5f, repo.getSystemCapacity(), 0.01f)
        assertEquals(45, repo.getHistoricDataDays())
        assertEquals(false, repo.getNotificationsEnabled())
        assertEquals("http://x.com", repo.getBaseUrl())
        assertEquals("light", repo.getDisplayMode())
    }

    // clearAll
    @Test
    fun clearAll_removesAllKeys() {
        repo.setLanguage("en")
        repo.setEmaAppId("abc")
        repo.setEmaAppSecret("secret")
        repo.setEmaSystemId("SYSID")
        repo.setEmaEcuId("123456")
        repo.setSystemCapacity(5.0f)
        repo.setHistoricDataDays(30)
        repo.setNotificationsEnabled(false)
        repo.setBaseUrl("http://test.com")
        repo.setDisplayMode("dark")

        repo.clearAll()

        assertEquals(SettingsRepository.LANGUAGE_DEFAULT, repo.getLanguage())
        assertEquals("", repo.getEmaAppId())
        assertEquals("", repo.getEmaAppSecret())
        assertEquals("", repo.getEmaSystemId())
        assertEquals("", repo.getEmaEcuId())
        assertEquals(-1f, repo.getSystemCapacity(), 0.001f)
        assertEquals(-1, repo.getHistoricDataDays())
        assertEquals(true, repo.getNotificationsEnabled())
        assertEquals(SettingsRepository.BASE_URL_DEFAULT, repo.getBaseUrl())
        assertEquals(SettingsRepository.DISPLAY_MODE_DEFAULT, repo.getDisplayMode())
    }
}
