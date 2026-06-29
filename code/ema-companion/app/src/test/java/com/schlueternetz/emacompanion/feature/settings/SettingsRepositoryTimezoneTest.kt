package com.schlueternetz.emacompanion.feature.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class SettingsRepositoryTimezoneTest {
    private lateinit var repo: SettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("ema_companion_settings_tz_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        repo = SettingsRepository(prefs)
    }

    @Test
    fun arrayTimezone_defaultIsSystemTimezone() {
        assertEquals(TimeZone.getDefault().id, repo.getArrayTimezone())
    }

    @Test
    fun arrayTimezone_savedValueIsReadBack() {
        repo.setArrayTimezone("America/New_York")
        assertEquals("America/New_York", repo.getArrayTimezone())
    }

    @Test
    fun arrayTimezone_exportIncludesTimezone() {
        repo.setArrayTimezone("Europe/Berlin")
        val obj = JSONObject(repo.exportToJson())
        assertEquals("Europe/Berlin", obj.getString("arrayTimezone"))
    }

    @Test
    fun arrayTimezone_importRestoresTimezone() {
        val json = """{"arrayTimezone":"Australia/Sydney"}"""
        repo.importFromJson(json)
        assertEquals("Australia/Sydney", repo.getArrayTimezone())
    }
}
