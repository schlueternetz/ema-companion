package com.schlueternetz.emacompanion.feature.home

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.TestListenableWorkerBuilder
import com.schlueternetz.emacompanion.core.AlertLevel
import com.schlueternetz.emacompanion.core.HomeTile
import com.schlueternetz.emacompanion.core.api.ApiResult
import com.schlueternetz.emacompanion.core.api.BatchEnergyFetch
import com.schlueternetz.emacompanion.core.api.EmaApiClient
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthRepository
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthStatus
import com.schlueternetz.emacompanion.core.email.EmailResult
import com.schlueternetz.emacompanion.core.email.EmailSender
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ModuleHealthWorkerTest {
    private lateinit var context: Context
    private lateinit var fakeClient: FakeClient
    private lateinit var repo: ModuleHealthRepository
    private lateinit var fakeEmailSender: FakeEmailSender
    private lateinit var settingsRepo: SettingsRepository

    private val today = LocalDate.of(2025, 7, 24)
    private val yesterday = today.minusDays(1)
    private val dayBefore = today.minusDays(2)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context
            .getSharedPreferences(ModuleHealthRepository.PREFS_HEALTH, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context
            .getSharedPreferences(ModuleHealthRepository.PREFS_DAILY, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        fakeClient = FakeClient()
        fakeEmailSender = FakeEmailSender()
        settingsRepo = SettingsRepository.create(context)
        repo = ModuleHealthRepository.forTest(context, fakeClient, today = { today })
        ModuleHealthWorker.repoOverride = repo
        ModuleHealthWorker.emailSenderOverride = fakeEmailSender

        Shadows
            .shadowOf(context as Application)
            .grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @After
    fun tearDown() {
        ModuleHealthWorker.repoOverride = null
        ModuleHealthWorker.emailSenderOverride = null
    }

    @Test
    fun doWork_statusChangesFromUnknown_postsNotification() {
        seedYellow()

        runWorker()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertEquals("notification expected on first YELLOW", 1, Shadows.shadowOf(nm).size())
        assertEquals(ModuleHealthStatus.YELLOW, repo.getLastNotifiedStatus())
    }

    @Test
    fun doWork_sameStatus_noNotification() {
        repo.setLastNotifiedStatus(ModuleHealthStatus.YELLOW)
        seedYellow()

        runWorker()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertEquals("no notification when status unchanged", 0, Shadows.shadowOf(nm).size())
        assertEquals(ModuleHealthStatus.YELLOW, repo.getLastNotifiedStatus())
    }

    @Test
    fun doWork_recoveryGreen_updatesLastNotifiedStatusToGreen() {
        repo.setLastNotifiedStatus(ModuleHealthStatus.YELLOW)
        seedGreen()

        runWorker()

        assertEquals(ModuleHealthStatus.GREEN, repo.getLastNotifiedStatus())
    }

    @Test
    fun doWork_unknownResult_noNotificationAndStatusNotUpdated() {
        // Empty responses → UNKNOWN
        runWorker()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertEquals("UNKNOWN status never notifies", 0, Shadows.shadowOf(nm).size())
        assertNull(repo.getLastNotifiedStatus())
    }

    // ── alerting is never gated (ADR-010) ───────────────────────────────────

    @Test
    fun doWork_tileDisabled_stillChecksAndNotifies() {
        settingsRepo.setTileEnabled(HomeTile.MODULE_HEALTH, false)
        seedYellow()

        runWorker()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertEquals(
            "Module Health tile being disabled must not stop the background check or its alert",
            1,
            Shadows.shadowOf(nm).size(),
        )
        assertEquals(ModuleHealthStatus.YELLOW, repo.getLastNotifiedStatus())
    }

    // ── notification level gating ───────────────────────────────────────────

    @Test
    fun doWork_notificationLevelOff_noNotificationEvenOnChange() {
        settingsRepo.setNotificationLevel(AlertLevel.OFF)
        seedYellow()

        runWorker()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertEquals("Off level must never notify", 0, Shadows.shadowOf(nm).size())
    }

    @Test
    fun doWork_notificationLevelAll_notifiesEvenWhenStatusUnchanged() {
        settingsRepo.setNotificationLevel(AlertLevel.ALL)
        repo.setLastNotifiedStatus(ModuleHealthStatus.YELLOW)
        seedYellow()

        runWorker()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertEquals("All level notifies every check regardless of change", 1, Shadows.shadowOf(nm).size())
    }

    // ── email integration ─────────────────────────────────────────────────────

    @Test
    fun doWork_statusChanges_emailConfigured_sendsEmail() {
        settingsRepo.setEmailAlertLevel(AlertLevel.ALERTS_ONLY)
        settingsRepo.setEmailAddress("user@example.com")
        settingsRepo.setEmailAppPassword("secret")
        seedYellow()

        runWorker()

        assertEquals("one email expected on YELLOW", 1, fakeEmailSender.sentCount)
        assertTrue(fakeEmailSender.lastSubject!!.contains("Solar module offline"))
        assertEquals(ModuleHealthStatus.YELLOW, repo.getLastEmailedStatus())
    }

    @Test
    fun doWork_sameStatus_noEmail() {
        settingsRepo.setEmailAlertLevel(AlertLevel.ALERTS_ONLY)
        settingsRepo.setEmailAddress("user@example.com")
        settingsRepo.setEmailAppPassword("secret")
        repo.setLastEmailedStatus(ModuleHealthStatus.YELLOW)
        seedYellow()

        runWorker()

        assertEquals("no email when status unchanged", 0, fakeEmailSender.sentCount)
        assertEquals(ModuleHealthStatus.YELLOW, repo.getLastEmailedStatus())
    }

    @Test
    fun doWork_emailLevelOff_noEmail() {
        settingsRepo.setEmailAlertLevel(AlertLevel.OFF)
        settingsRepo.setEmailAddress("user@example.com")
        settingsRepo.setEmailAppPassword("secret")
        seedYellow()

        runWorker()

        assertEquals("no email when level is Off", 0, fakeEmailSender.sentCount)
    }

    @Test
    fun doWork_emailLevelAll_sendsEvenWhenStatusUnchanged() {
        settingsRepo.setEmailAlertLevel(AlertLevel.ALL)
        settingsRepo.setEmailAddress("user@example.com")
        settingsRepo.setEmailAppPassword("secret")
        repo.setLastEmailedStatus(ModuleHealthStatus.YELLOW)
        seedYellow()

        runWorker()

        assertEquals("All level sends every check regardless of change", 1, fakeEmailSender.sentCount)
    }

    @Test
    fun doWork_emailUnconfigured_neverSendsRegardlessOfLevel() {
        settingsRepo.setEmailAlertLevel(AlertLevel.ALL)
        seedYellow()

        runWorker()

        assertEquals("unconfigured email must never send", 0, fakeEmailSender.sentCount)
    }

    @Test
    fun doWork_emailAuthFailure_lastEmailedStatusNotUpdated() {
        settingsRepo.setEmailAlertLevel(AlertLevel.ALERTS_ONLY)
        settingsRepo.setEmailAddress("user@example.com")
        settingsRepo.setEmailAppPassword("secret")
        fakeEmailSender.nextResult = EmailResult.AuthFailure
        seedYellow()

        runWorker()

        assertNull(
            "lastEmailedStatus must not update on AuthFailure so next change retries",
            repo.getLastEmailedStatus(),
        )
    }

    @Test
    fun doWork_recoveryGreen_emailConfigured_sendsRecoveryEmail() {
        settingsRepo.setEmailAlertLevel(AlertLevel.ALERTS_ONLY)
        settingsRepo.setEmailAddress("user@example.com")
        settingsRepo.setEmailAppPassword("secret")
        repo.setLastEmailedStatus(ModuleHealthStatus.YELLOW)
        seedGreen()

        runWorker()

        assertEquals("recovery email expected", 1, fakeEmailSender.sentCount)
        assertTrue(fakeEmailSender.lastSubject!!.contains("All modules producing"))
        assertEquals(ModuleHealthStatus.GREEN, repo.getLastEmailedStatus())
    }

    private fun seedYellow() {
        fakeClient.responses[today.toString()] = mapOf("INV1" to 0.0, "INV2" to 1.5)
        fakeClient.responses[yesterday.toString()] = mapOf("INV1" to 1.2, "INV2" to 1.2)
        fakeClient.responses[dayBefore.toString()] = mapOf("INV1" to 1.1, "INV2" to 1.1)
    }

    private fun seedGreen() {
        fakeClient.responses[today.toString()] = mapOf("INV1" to 1.5, "INV2" to 1.5)
        fakeClient.responses[yesterday.toString()] = mapOf("INV1" to 1.2, "INV2" to 1.2)
        fakeClient.responses[dayBefore.toString()] = mapOf("INV1" to 1.1, "INV2" to 1.1)
    }

    private fun runWorker() {
        val worker = TestListenableWorkerBuilder<ModuleHealthWorker>(context).build()
        runBlocking { worker.doWork() }
    }

    class FakeEmailSender(
        var nextResult: EmailResult = EmailResult.Success,
    ) : EmailSender {
        var sentCount = 0
        var lastSubject: String? = null

        override suspend fun send(
            to: String,
            subject: String,
            body: String,
        ): EmailResult {
            sentCount++
            lastSubject = subject
            return nextResult
        }

        override suspend fun testConnection(): EmailResult = nextResult
    }

    class FakeClient : EmaApiClient {
        val responses = mutableMapOf<String, Map<String, Double>>()

        override suspend fun getBatchInverterEnergy(date: String): BatchEnergyFetch {
            val data = responses[date] ?: return BatchEnergyFetch(ApiResult.NetworkError)
            return BatchEnergyFetch(ApiResult.Success(data))
        }
    }
}
