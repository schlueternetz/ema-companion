package com.schlueternetz.emacompanion.feature.settings

import android.content.Context
import android.os.Looper
import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.api.ApiResult
import com.schlueternetz.emacompanion.core.api.ApiSyncWorker
import com.schlueternetz.emacompanion.core.api.ApiUsageRepository
import com.schlueternetz.emacompanion.core.api.BatchEnergyFetch
import com.schlueternetz.emacompanion.core.api.DailyEnergyRepository
import com.schlueternetz.emacompanion.core.api.EmaApiClient
import com.schlueternetz.emacompanion.core.api.HourlyEnergyFetch
import com.schlueternetz.emacompanion.core.api.HourlyEnergyRepository
import com.schlueternetz.emacompanion.core.api.HourlySnapshot
import com.schlueternetz.emacompanion.core.api.log.ApiCallLogRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

        override suspend fun getBatchInverterEnergy(date: String) = BatchEnergyFetch(ApiResult.ConfigurationError)

        override suspend fun getHourlyEnergy(date: String): HourlyEnergyFetch {
            if (!configuredProvider()) return HourlyEnergyFetch(ApiResult.ConfigurationError)
            hourlyCalls++
            return HourlyEnergyFetch(ApiResult.Success(HourlySnapshot(mapOf(6 to 1.0))), "/x", 1L, "req", "{}")
        }
    }

    private fun realHourlyRepo(
        client: EmaApiClient,
        settings: SettingsRepository,
    ) = HourlyEnergyRepository(
        client = client,
        usageCounter = ApiUsageRepository.create(appContext),
        log = ApiCallLogRepository.create(appContext),
        appSecretProvider = { settings.getEmaAppSecret() },
        prefs = appContext.getSharedPreferences("ema_hourly", Context.MODE_PRIVATE),
    )

    private fun realDailyRepo(
        client: EmaApiClient,
        settings: SettingsRepository,
    ) = DailyEnergyRepository(
        client = client,
        usageCounter = ApiUsageRepository.create(appContext),
        log = ApiCallLogRepository.create(appContext),
        appSecretProvider = { settings.getEmaAppSecret() },
        prefs = appContext.getSharedPreferences("ema_daily", Context.MODE_PRIVATE),
    )

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            appContext,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        listOf("ema_companion_settings", "ema_api_usage", "ema_api_log", "ema_hourly", "ema_daily").forEach { name ->
            appContext
                .getSharedPreferences(name, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
        }
        val settings = SettingsRepository.create(appContext)
        client = FakeClient(configuredProvider = { settings.isConfigured() })
        ApiSyncWorker.hourlySourceOverride = realHourlyRepo(client, settings)
        ApiSyncWorker.dailySourceOverride = realDailyRepo(client, settings)
        ApiSyncWorker.updateAllAction = { _, _ -> widgetUpdateCallCount++ }
    }

    @After
    fun tearDown() {
        ApiSyncWorker.hourlySourceOverride = null
        ApiSyncWorker.dailySourceOverride = null
        ApiSyncWorker.updateAllAction = ApiSyncWorker.defaultUpdateAllAction
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    /**
     * `enqueueUniqueWork()` doesn't block for completion, so a bare `idle()` right after
     * triggering a resync is not a reliable wait for the resulting `ApiSyncWorker.doWork()` to
     * have actually finished — poll briefly instead.
     */
    private fun awaitUntil(
        maxAttempts: Int = 100,
        condition: () -> Boolean,
    ) {
        var attempts = 0
        while (!condition() && attempts < maxAttempts) {
            idle()
            Thread.sleep(20)
            attempts++
        }
    }

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
        awaitUntil { widgetUpdateCallCount >= 2 }

        assertEquals(1, client.hourlyCalls)
        // ApiSyncWorker (unlike the old inline invalidateApiThrottle()) updates widgets once per
        // successfully-completed branch, not once for the whole resync — hourly succeeds for
        // real; daily's backfill call hits FakeClient's default getDailyEnergy() (unimplemented
        // here, returns ConfigurationError), which the repository treats as a no-op success
        // (error stays null), so it also counts as a completed branch.
        assertEquals(2, widgetUpdateCallCount)
    }

    @Test
    fun changingCredential_skipsHourlyAndDailyRefresh_whenNoEnabledConsumer() {
        val settings = SettingsRepository.create(appContext)
        settings.setEmaAppId("a".repeat(32))
        settings.setEmaAppSecret("b".repeat(12))
        settings.setEmaSystemId("c".repeat(16))
        settings.setEmaEcuId("1".repeat(12))
        settings.setSystemCapacity(5f)
        settings.setTileEnabled(com.schlueternetz.emacompanion.core.HomeTile.TODAY_PRODUCTION, false)
        settings.setTileEnabled(com.schlueternetz.emacompanion.core.HomeTile.HISTORY_PRODUCTION, false)
        settings.setWidgetEnabled(com.schlueternetz.emacompanion.core.HomeWidget.TODAY_PRODUCTION, false)
        settings.setWidgetEnabled(com.schlueternetz.emacompanion.core.HomeWidget.PRODUCTION_SUMMARY, false)
        settings.setWidgetEnabled(com.schlueternetz.emacompanion.core.HomeWidget.PRODUCTION_HISTORY, false)

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

        assertEquals(0, client.hourlyCalls)
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
        awaitUntil { widgetUpdateCallCount >= 2 }

        assertEquals(1, client.hourlyCalls)
        // See changingCredential_..._andUpdatesWidgets: one call per successfully-completed branch.
        assertEquals(2, widgetUpdateCallCount)
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
        awaitUntil { widgetUpdateCallCount >= 2 }

        assertEquals(0, client.hourlyCalls)
        // See changingCredential_..._andUpdatesWidgets: one call per successfully-completed branch.
        assertEquals(2, widgetUpdateCallCount)
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

        val hourlyPrefs = appContext.getSharedPreferences("ema_hourly", Context.MODE_PRIVATE)
        val dailyPrefs = appContext.getSharedPreferences("ema_daily", Context.MODE_PRIVATE)
        assertEquals(null, hourlyPrefs.getString("hours", null))
        assertEquals(true, dailyPrefs.all.keys.none { it.startsWith("day_") })
    }

    // ── The regression this whole change exists to fix ──────────────────────
    //
    // Mirrors the exact CI failure: saving one connection-affecting field then immediately
    // saving another used to fire two independent, uncoordinated forced refreshes. If the
    // earlier one finished LAST with a failure (e.g. the stub's single-use interaction had
    // already been consumed by the redundant earlier attempt), it silently overwrote the
    // later save's success with a stale error. `ExistingWorkPolicy.REPLACE` must make this
    // structurally impossible: the earlier request is cancelled, never persists, regardless
    // of which one would otherwise have finished last.

    @Test
    fun rapidSequentialCredentialSaves_onlyLatestSavePersists_noStaleErrorOverwrite() {
        val settings = SettingsRepository.create(appContext)
        settings.setEmaAppId("a".repeat(32))
        settings.setEmaAppSecret("b".repeat(12))
        settings.setEmaSystemId("c".repeat(16))
        settings.setEmaEcuId("1".repeat(12))
        settings.setSystemCapacity(5f)

        val gate = CompletableDeferred<Unit>()
        val firstFetchStarted = CompletableDeferred<Unit>()
        var callCount = 0
        val gatedClient =
            object : EmaApiClient {
                override suspend fun getBatchInverterEnergy(date: String) = BatchEnergyFetch(ApiResult.ConfigurationError)

                override suspend fun getHourlyEnergy(date: String): HourlyEnergyFetch {
                    callCount++
                    if (callCount == 1) {
                        firstFetchStarted.complete(Unit)
                        gate.await() // suspends until released below
                        // The earlier, now-superseded attempt "fails" here — it must never be
                        // allowed to persist over the later save's result.
                        return HourlyEnergyFetch(ApiResult.NetworkError)
                    }
                    return HourlyEnergyFetch(ApiResult.Success(HourlySnapshot(mapOf(6 to 9.0))), "/x", 1L, "req", "{}")
                }
            }
        ApiSyncWorker.hourlySourceOverride = realHourlyRepo(gatedClient, settings)
        ApiSyncWorker.dailySourceOverride = realDailyRepo(gatedClient, settings)

        val scenario = launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_EMACompanion)
        idle()

        // The first save's resulting resync is simulated by calling ApiSyncScheduler directly on
        // a background thread (exactly what SettingRowView.onSave -> invalidateApiThrottle() does
        // today) — invoking Fragment/View code itself from a raw background JVM thread is not
        // safe in Robolectric, so the *second*, real save below is driven through the actual
        // SettingsFragment UI on the main thread, proving the production wiring end-to-end while
        // the concurrency needed to exercise REPLACE's cancellation comes from the first call.
        val firstThread =
            Thread {
                com.schlueternetz.emacompanion.core.api.ApiSyncScheduler
                    .requestResyncAfterSettingsChange(appContext)
            }
        firstThread.isDaemon = true
        firstThread.start()
        runBlocking { firstFetchStarted.await() }

        // Second save (ECU ID), via the real Fragment UI, while the first fetch is still
        // suspended — REPLACE must cancel it.
        scenario.onFragment { fragment ->
            fragment
                .requireView()
                .findViewById<SettingRowView>(R.id.setting_ema_ecu_id)
                .onSave
                .invoke("9".repeat(12))
        }

        gate.complete(Unit)
        firstThread.join(5_000)

        // Poll briefly: enqueueUniqueWork() doesn't block for completion, so the second (real)
        // fetch may still be finishing its own dispatch/execution at this point.
        var finalState = ApiSyncWorker.hourlySourceOverride!!.currentState()
        var attempts = 0
        while (finalState.snapshot?.hours?.get(6) == null && attempts < 100) {
            idle()
            Thread.sleep(20)
            finalState = ApiSyncWorker.hourlySourceOverride!!.currentState()
            attempts++
        }

        assertEquals(
            "callCount=$callCount, error=${finalState.error}, snapshot=${finalState.snapshot}, attempts=$attempts",
            2,
            callCount,
        )
        assertNull("the earlier, superseded attempt's failure must not have persisted", finalState.error)
        assertEquals(9.0, finalState.snapshot?.hours?.get(6)!!, 0.001)
    }
}
