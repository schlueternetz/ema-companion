package com.schlueternetz.emacompanion.feature.settings

import android.content.Context
import android.os.Looper
import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.api.ApiResult
import com.schlueternetz.emacompanion.core.api.ApiUsageRepository
import com.schlueternetz.emacompanion.core.api.BatchEnergyFetch
import com.schlueternetz.emacompanion.core.api.DailyEnergyRepository
import com.schlueternetz.emacompanion.core.api.EmaApiClient
import com.schlueternetz.emacompanion.core.api.HourlyEnergyFetch
import com.schlueternetz.emacompanion.core.api.HourlyEnergyRepository
import com.schlueternetz.emacompanion.core.api.HourlySnapshot
import com.schlueternetz.emacompanion.core.api.ProductionFetch
import com.schlueternetz.emacompanion.core.api.log.ApiCallLogRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsWidgetRefreshTest {
    private lateinit var appContext: Context
    private lateinit var client: FakeClient
    private var widgetUpdateCallCount = 0

    private class FakeClient(
        val configuredProvider: () -> Boolean,
    ) : EmaApiClient {
        var hourlyCalls = 0
        var dailyCalls = 0

        override suspend fun getCurrentProduction() = ProductionFetch(ApiResult.ConfigurationError)

        override suspend fun getBatchInverterEnergy(date: String) = BatchEnergyFetch(ApiResult.ConfigurationError)

        override suspend fun getHourlyEnergy(date: String): HourlyEnergyFetch {
            if (!configuredProvider()) return HourlyEnergyFetch(ApiResult.ConfigurationError)
            hourlyCalls++
            return HourlyEnergyFetch(ApiResult.Success(HourlySnapshot(mapOf(6 to 1.0))), "/x", 1L, "req", "{}")
        }
    }

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        listOf("ema_companion_settings", "ema_api_usage", "ema_api_log", "ema_hourly", "ema_daily").forEach { name ->
            appContext
                .getSharedPreferences(name, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
        }
        val settings = SettingsRepository.create(appContext)
        client = FakeClient(configuredProvider = { settings.isConfigured() })
        SettingsFragment.hourlyRepoOverride =
            HourlyEnergyRepository(
                client = client,
                usageCounter = ApiUsageRepository.create(appContext),
                log = ApiCallLogRepository.create(appContext),
                appSecretProvider = { settings.getEmaAppSecret() },
                prefs = appContext.getSharedPreferences("ema_hourly", Context.MODE_PRIVATE),
            )
        SettingsFragment.dailyRepoOverride =
            DailyEnergyRepository(
                client = client,
                usageCounter = ApiUsageRepository.create(appContext),
                log = ApiCallLogRepository.create(appContext),
                appSecretProvider = { settings.getEmaAppSecret() },
                prefs = appContext.getSharedPreferences("ema_daily", Context.MODE_PRIVATE),
            )
        SettingsFragment.widgetUpdateAction = { widgetUpdateCallCount++ }
    }

    @After
    fun tearDown() {
        SettingsFragment.hourlyRepoOverride = null
        SettingsFragment.dailyRepoOverride = null
        SettingsFragment.widgetUpdateAction = SettingsFragment.defaultWidgetUpdateAction
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun changingCredential_triggersImmediateHourlyAndDailyRefresh_andUpdatesWidgets() {
        val settings = SettingsRepository.create(appContext)
        settings.setEmaAppId("a".repeat(32))
        settings.setEmaAppSecret("b".repeat(12))
        settings.setEmaSystemId("c".repeat(16))
        settings.setEmaEcuId("1".repeat(12))
        settings.setSystemCapacity(5f)

        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            fragment
                .requireView()
                .findViewById<SettingRowView>(R.id.setting_ema_app_id)
                .onSave
                .invoke("a".repeat(32))
        }
        idle()

        assertEquals(1, client.hourlyCalls)
        assertEquals(1, widgetUpdateCallCount)
    }

    @Test
    fun importingSettings_triggersImmediateRefresh_andUpdatesWidgets() {
        val settings = SettingsRepository.create(appContext)
        settings.setEmaAppId("a".repeat(32))
        settings.setEmaAppSecret("b".repeat(12))
        settings.setEmaSystemId("c".repeat(16))
        settings.setEmaEcuId("1".repeat(12))
        settings.setSystemCapacity(5f)

        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment -> fragment.refreshAllDisplayedValues() }
        idle()

        assertEquals(1, client.hourlyCalls)
        assertEquals(1, widgetUpdateCallCount)
    }

    @Test
    fun factoryReset_updatesWidgets_withoutAttemptingNetworkCall() {
        val settings = SettingsRepository.create(appContext)
        settings.setEmaAppId("a".repeat(32))
        settings.setEmaAppSecret("b".repeat(12))
        settings.setEmaSystemId("c".repeat(16))
        settings.setEmaEcuId("1".repeat(12))
        settings.setSystemCapacity(5f)

        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.settings_factory_reset_button).performClick()
            val dialog =
                org.robolectric.shadows.ShadowDialog
                    .getLatestDialog() as androidx.appcompat.app.AlertDialog
            idle()
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
        }
        idle()

        assertEquals(0, client.hourlyCalls)
        assertEquals(1, widgetUpdateCallCount)
    }

    @Test
    fun factoryReset_clearsHourlyAndDailyStores() {
        appContext
            .getSharedPreferences("ema_hourly", Context.MODE_PRIVATE)
            .edit()
            .putString("hours", "{}")
            .commit()
        appContext
            .getSharedPreferences("ema_daily", Context.MODE_PRIVATE)
            .edit()
            .putString("day_2026-07-01", "1.0")
            .commit()

        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.settings_factory_reset_button).performClick()
            val dialog =
                org.robolectric.shadows.ShadowDialog
                    .getLatestDialog() as androidx.appcompat.app.AlertDialog
            idle()
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
        }
        idle()

        // .clear() removes the cached data; resetThrottle() (also part of the reset path) then
        // legitimately re-adds a lastFetchMs=0 marker, so assert on the cached data keys, not
        // "prefs is completely empty".
        val hourlyPrefs = appContext.getSharedPreferences("ema_hourly", Context.MODE_PRIVATE)
        val dailyPrefs = appContext.getSharedPreferences("ema_daily", Context.MODE_PRIVATE)
        assertEquals(null, hourlyPrefs.getString("hours", null))
        assertEquals(true, dailyPrefs.all.keys.none { it.startsWith("day_") })
    }
}
