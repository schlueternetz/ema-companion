package com.schlueternetz.emacompanion.feature.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.schlueternetz.emacompanion.core.HomeTile
import com.schlueternetz.emacompanion.core.HomeWidget
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        prefs =
            ApplicationProvider
                .getApplicationContext<Context>()
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
        val brokenPrefs =
            object : SharedPreferences by prefs {
                override fun getString(
                    key: String?,
                    defValue: String?,
                ): String? = throw SecurityException("Keystore unavailable")
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
    fun getHistoricDataDays_returnsDefault_whenNothingStored() {
        assertEquals(SettingsRepository.HISTORIC_DATA_DAYS_DEFAULT, repo.getHistoricDataDays())
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
        repo.setHistoricDataDays(30)
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
        repo.setHistoricDataDays(30)
        assertEquals(false, repo.isConfigured())
    }

    // exportToJson
    @Test
    fun exportToJson_containsAllElevenKeys() {
        repo.setLanguage("en")
        repo.setEmaAppId("abc")
        repo.setEmaAppSecret("secret")
        repo.setEmaSystemId("SYSID")
        repo.setEmaEcuId("123456789012")
        repo.setSystemCapacity(4.5f)
        repo.setHistoricDataDays(30)
        repo.setApiRequestLimit(1000)
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
        assertTrue(obj.has("apiRequestLimit"))
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

    @Test
    fun exportToJson_systemCapacityHasNoFloatWideningNoise() {
        repo.setSystemCapacity(9.72f)

        val json = repo.exportToJson()
        val obj = JSONObject(json)

        assertEquals(9.72, obj.getDouble("systemCapacity"), 0.0)
    }

    @Test
    fun setSystemCapacity_roundsToTwoDecimals() {
        repo.setSystemCapacity(9.723f)
        assertEquals(9.72f, repo.getSystemCapacity(), 0.0001f)
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
    fun importFromJson_importsAllElevenFields() {
        repo.importFromJson(
            """{"language":"de","emaAppId":"a","emaAppSecret":"b","emaSystemId":"c",
            "emaEcuId":"d","systemCapacity":5.5,"historicDataDays":45,"apiRequestLimit":800,
            "notificationsEnabled":false,"baseUrl":"http://x.com","displayMode":"light"}""",
        )
        assertEquals("de", repo.getLanguage())
        assertEquals("a", repo.getEmaAppId())
        assertEquals("b", repo.getEmaAppSecret())
        assertEquals("c", repo.getEmaSystemId())
        assertEquals("d", repo.getEmaEcuId())
        assertEquals(5.5f, repo.getSystemCapacity(), 0.01f)
        assertEquals(45, repo.getHistoricDataDays())
        assertEquals(800, repo.getApiRequestLimit())
        assertEquals(false, repo.getNotificationsEnabled())
        assertEquals("http://x.com", repo.getBaseUrl())
        assertEquals("light", repo.getDisplayMode())
    }

    // API Request Limit
    @Test
    fun getApiRequestLimit_returnsDefault_whenNotSet() {
        assertEquals(SettingsRepository.API_REQUEST_LIMIT_DEFAULT, repo.getApiRequestLimit())
    }

    @Test
    fun setApiRequestLimit_persists() {
        repo.setApiRequestLimit(1000)
        assertEquals(1000, repo.getApiRequestLimit())
    }

    @Test
    fun isConfigured_returnsTrue_whenApiRequestLimitNotExplicitlySet() {
        repo.setEmaAppId("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        repo.setEmaAppSecret("bbbbbbbbbbbb")
        repo.setEmaSystemId("CCCCCCCCCCCCCCCC")
        repo.setEmaEcuId("123456789012")
        repo.setSystemCapacity(4.5f)
        repo.setHistoricDataDays(30)
        assertEquals(true, repo.isConfigured())
    }

    @Test
    fun exportToJson_includesApiRequestLimit() {
        repo.setApiRequestLimit(500)
        val obj = JSONObject(repo.exportToJson())
        assertTrue(obj.has("apiRequestLimit"))
        assertEquals(500, obj.getInt("apiRequestLimit"))
    }

    @Test
    fun importFromJson_setsApiRequestLimit() {
        repo.importFromJson("""{"apiRequestLimit":750}""")
        assertEquals(750, repo.getApiRequestLimit())
    }

    // Email alerts enabled
    @Test
    fun getEmailAlertsEnabled_returnsFalse_byDefault() {
        assertFalse(repo.getEmailAlertsEnabled())
    }

    @Test
    fun setEmailAlertsEnabled_persists() {
        repo.setEmailAlertsEnabled(true)
        assertTrue(repo.getEmailAlertsEnabled())
    }

    // Email credentials
    @Test
    fun getEmailAddress_returnsEmpty_whenNothingStored() {
        assertEquals("", repo.getEmailAddress())
    }

    @Test
    fun setEmailAddress_persistsAndReturnsValue() {
        repo.setEmailAddress("user@gmail.com")
        assertEquals("user@gmail.com", repo.getEmailAddress())
    }

    @Test
    fun getEmailAppPassword_returnsEmpty_whenNothingStored() {
        assertEquals("", repo.getEmailAppPassword())
    }

    @Test
    fun setEmailAppPassword_persistsAndReturnsValue() {
        repo.setEmailAppPassword("abcd efgh ijkl mnop")
        assertEquals("abcd efgh ijkl mnop", repo.getEmailAppPassword())
    }

    @Test
    fun deleteEmailCredentials_removesAddressAndPassword() {
        repo.setEmailAddress("user@gmail.com")
        repo.setEmailAppPassword("abcd efgh ijkl mnop")

        repo.deleteEmailCredentials()

        assertEquals("", repo.getEmailAddress())
        assertEquals("", repo.getEmailAppPassword())
    }

    @Test
    fun isEmailConfigured_returnsFalse_whenBothEmpty() {
        assertFalse(repo.isEmailConfigured())
    }

    @Test
    fun isEmailConfigured_returnsFalse_whenAddressEmpty() {
        repo.setEmailAppPassword("abcd efgh ijkl mnop")
        assertFalse(repo.isEmailConfigured())
    }

    @Test
    fun isEmailConfigured_returnsFalse_whenPasswordEmpty() {
        repo.setEmailAddress("user@gmail.com")
        assertFalse(repo.isEmailConfigured())
    }

    @Test
    fun isEmailConfigured_returnsTrue_whenBothPresent() {
        repo.setEmailAddress("user@gmail.com")
        repo.setEmailAppPassword("abcd efgh ijkl mnop")
        assertTrue(repo.isEmailConfigured())
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
        assertEquals(SettingsRepository.HISTORIC_DATA_DAYS_DEFAULT, repo.getHistoricDataDays())
        assertEquals(true, repo.getNotificationsEnabled())
        assertEquals(SettingsRepository.BASE_URL_DEFAULT, repo.getBaseUrl())
        assertEquals(SettingsRepository.DISPLAY_MODE_DEFAULT, repo.getDisplayMode())
    }

    // Tile / widget enabled flags
    @Test
    fun isTileEnabled_defaultsToTrue_whenUnset() {
        HomeTile.entries.forEach { tile ->
            assertTrue("$tile should default to enabled", repo.isTileEnabled(tile))
        }
    }

    @Test
    fun isWidgetEnabled_defaultsToTrue_whenUnset() {
        HomeWidget.entries.forEach { widget ->
            assertTrue("$widget should default to enabled", repo.isWidgetEnabled(widget))
        }
    }

    @Test
    fun setTileEnabled_persistsFalse() {
        repo.setTileEnabled(HomeTile.MODULE_HEALTH, false)
        assertFalse(repo.isTileEnabled(HomeTile.MODULE_HEALTH))
    }

    @Test
    fun setTileEnabled_doesNotAffectOtherTiles() {
        repo.setTileEnabled(HomeTile.MODULE_HEALTH, false)
        assertTrue(repo.isTileEnabled(HomeTile.TODAY_PRODUCTION))
        assertTrue(repo.isTileEnabled(HomeTile.HISTORY_PRODUCTION))
    }

    @Test
    fun setWidgetEnabled_persistsFalse() {
        repo.setWidgetEnabled(HomeWidget.PRODUCTION_SUMMARY, false)
        assertFalse(repo.isWidgetEnabled(HomeWidget.PRODUCTION_SUMMARY))
    }

    @Test
    fun setWidgetEnabled_doesNotAffectOtherWidgets() {
        repo.setWidgetEnabled(HomeWidget.PRODUCTION_SUMMARY, false)
        assertTrue(repo.isWidgetEnabled(HomeWidget.TODAY_PRODUCTION))
        assertTrue(repo.isWidgetEnabled(HomeWidget.PRODUCTION_HISTORY))
    }

    // Derived data-need methods
    @Test
    fun isHourlyDataNeeded_trueByDefault() {
        assertTrue(repo.isHourlyDataNeeded())
    }

    @Test
    fun isHourlyDataNeeded_falseWhenAllThreeConsumersDisabled() {
        repo.setTileEnabled(HomeTile.TODAY_PRODUCTION, false)
        repo.setWidgetEnabled(HomeWidget.TODAY_PRODUCTION, false)
        repo.setWidgetEnabled(HomeWidget.PRODUCTION_SUMMARY, false)
        assertFalse(repo.isHourlyDataNeeded())
    }

    @Test
    fun isHourlyDataNeeded_trueWhenOnlyWidgetRemainsEnabled() {
        repo.setTileEnabled(HomeTile.TODAY_PRODUCTION, false)
        repo.setWidgetEnabled(HomeWidget.PRODUCTION_SUMMARY, false)
        // HomeWidget.TODAY_PRODUCTION still enabled
        assertTrue(repo.isHourlyDataNeeded())
    }

    @Test
    fun isDailyDataNeeded_trueByDefault() {
        assertTrue(repo.isDailyDataNeeded())
    }

    @Test
    fun isDailyDataNeeded_falseWhenAllFourConsumersDisabled() {
        repo.setTileEnabled(HomeTile.TODAY_PRODUCTION, false)
        repo.setTileEnabled(HomeTile.HISTORY_PRODUCTION, false)
        repo.setWidgetEnabled(HomeWidget.PRODUCTION_SUMMARY, false)
        repo.setWidgetEnabled(HomeWidget.PRODUCTION_HISTORY, false)
        assertFalse(repo.isDailyDataNeeded())
    }

    @Test
    fun isDailyDataNeeded_trueWhenOnlyTodayProductionTileRemainsEnabled() {
        // Today Production's best-day cards consume daily data even though History is off.
        repo.setTileEnabled(HomeTile.HISTORY_PRODUCTION, false)
        repo.setWidgetEnabled(HomeWidget.PRODUCTION_SUMMARY, false)
        repo.setWidgetEnabled(HomeWidget.PRODUCTION_HISTORY, false)
        assertTrue(repo.isDailyDataNeeded())
    }

    @Test
    fun isModuleHealthDataNeeded_trueByDefault() {
        assertTrue(repo.isModuleHealthDataNeeded())
    }

    @Test
    fun isModuleHealthDataNeeded_falseWhenTileDisabled() {
        repo.setTileEnabled(HomeTile.MODULE_HEALTH, false)
        assertFalse(repo.isModuleHealthDataNeeded())
    }

    // Tile / widget flags in export/import
    @Test
    fun exportToJson_includesAllSixTileAndWidgetKeys() {
        val obj = JSONObject(repo.exportToJson())
        HomeTile.entries.forEach { assertTrue(obj.has("tileEnabled_${it.name}")) }
        HomeWidget.entries.forEach { assertTrue(obj.has("widgetEnabled_${it.name}")) }
    }

    @Test
    fun exportToJson_reflectsDisabledTileValue() {
        repo.setTileEnabled(HomeTile.MODULE_HEALTH, false)
        val obj = JSONObject(repo.exportToJson())
        assertFalse(obj.getBoolean("tileEnabled_${HomeTile.MODULE_HEALTH.name}"))
    }

    @Test
    fun importFromJson_setsTileAndWidgetFlags() {
        repo.importFromJson(
            """{"tileEnabled_MODULE_HEALTH":false,"widgetEnabled_PRODUCTION_SUMMARY":false}""",
        )
        assertFalse(repo.isTileEnabled(HomeTile.MODULE_HEALTH))
        assertFalse(repo.isWidgetEnabled(HomeWidget.PRODUCTION_SUMMARY))
        // Unmentioned flags stay at their default.
        assertTrue(repo.isTileEnabled(HomeTile.TODAY_PRODUCTION))
        assertTrue(repo.isWidgetEnabled(HomeWidget.TODAY_PRODUCTION))
    }

    @Test
    fun importFromJson_missingTileAndWidgetKeys_leavesExistingValuesUnchanged() {
        repo.setTileEnabled(HomeTile.MODULE_HEALTH, false)
        repo.importFromJson("""{"emaAppId":"abc"}""")
        assertFalse(repo.isTileEnabled(HomeTile.MODULE_HEALTH))
    }

    // clearAll resets tile/widget flags to default-enabled
    @Test
    fun clearAll_resetsTileAndWidgetFlagsToEnabled() {
        repo.setTileEnabled(HomeTile.MODULE_HEALTH, false)
        repo.setWidgetEnabled(HomeWidget.PRODUCTION_SUMMARY, false)
        repo.clearAll()
        HomeTile.entries.forEach { assertTrue(repo.isTileEnabled(it)) }
        HomeWidget.entries.forEach { assertTrue(repo.isWidgetEnabled(it)) }
    }
}
