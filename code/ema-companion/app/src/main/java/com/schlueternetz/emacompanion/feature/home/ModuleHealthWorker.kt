package com.schlueternetz.emacompanion.feature.home

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.schlueternetz.emacompanion.core.AlertLevel
import com.schlueternetz.emacompanion.core.AppConfig
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthRepository
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthStatus
import com.schlueternetz.emacompanion.core.email.EmailContentBuilder
import com.schlueternetz.emacompanion.core.email.EmailResult
import com.schlueternetz.emacompanion.core.email.EmailSender
import com.schlueternetz.emacompanion.core.email.GmailSmtpEmailSender
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Whether a module health alert should be dispatched for [newStatus] at the given [level]:
 * - [AlertLevel.OFF] never alerts.
 * - [AlertLevel.ALERTS_ONLY] alerts only when the status differs from [previousStatus] (covers
 *   both degradation and recovery).
 * - [AlertLevel.ALL] alerts on every check regardless of change.
 */
internal fun shouldAlert(
    level: AlertLevel,
    previousStatus: ModuleHealthStatus?,
    newStatus: ModuleHealthStatus,
): Boolean =
    when (level) {
        AlertLevel.OFF -> false
        AlertLevel.ALERTS_ONLY -> newStatus != previousStatus
        AlertLevel.ALL -> true
    }

class ModuleHealthWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repo = repoOverride ?: ModuleHealthRepository.create(applicationContext)
        val settings = SettingsRepository.create(applicationContext)
        val previousNotifiedStatus = repo.getLastNotifiedStatus()
        val previousEmailedStatus = repo.getLastEmailedStatus()
        val state = repo.refresh()
        val newStatus = state.status

        val notificationLevel = settings.getNotificationLevel()
        if (newStatus != ModuleHealthStatus.UNKNOWN &&
            shouldAlert(notificationLevel, previousNotifiedStatus, newStatus)
        ) {
            ModuleHealthNotifier.ensureChannelCreated(applicationContext)
            ModuleHealthNotifier.notify(applicationContext, state, postOnGreen = notificationLevel == AlertLevel.ALL)
            repo.setLastNotifiedStatus(newStatus)
        }

        if (newStatus != ModuleHealthStatus.UNKNOWN &&
            shouldAlert(settings.getEmailAlertLevel(), previousEmailedStatus, newStatus) &&
            settings.isEmailConfigured()
        ) {
            val sender =
                emailSenderOverride ?: GmailSmtpEmailSender(
                    from = settings.getEmailAddress(),
                    appPassword = settings.getEmailAppPassword(),
                )
            val contentBuilder = EmailContentBuilder(applicationContext)
            val result =
                sender.send(
                    to = settings.getEmailAddress(),
                    subject = contentBuilder.buildSubject(newStatus),
                    body = contentBuilder.buildBody(newStatus, state.offlineModules),
                )
            if (result == EmailResult.Success) {
                repo.setLastEmailedStatus(newStatus)
            }
        }

        return Result.success()
    }

    companion object {
        /** Test seam: inject a pre-built repository so doWork() can be tested without real storage. */
        var repoOverride: ModuleHealthRepository? = null

        /** Test seam: inject a fake EmailSender so email logic can be tested without SMTP. */
        var emailSenderOverride: EmailSender? = null

        private const val WORK_NAME = "module_health_check"

        /**
         * Enqueues a 24-hour periodic check aligned to 8pm in [arrayTimezoneId].
         * Uses KEEP policy: an already-enqueued work request is not disturbed on app restart.
         * Call with CANCEL_AND_REENQUEUE on timezone change to realign the initialDelay.
         */
        fun schedule(
            context: Context,
            arrayTimezoneId: String,
            policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP,
        ) {
            val initialDelay = millisUntilNext8pm(arrayTimezoneId)
            val request =
                PeriodicWorkRequestBuilder<ModuleHealthWorker>(
                    AppConfig.MODULE_HEALTH_CHECK_INTERVAL_MS,
                    TimeUnit.MILLISECONDS,
                    // flexInterval: allow system to run anywhere in the last hour of the period
                    AppConfig.MODULE_HEALTH_CHECK_INTERVAL_MS / 24,
                    TimeUnit.MILLISECONDS,
                ).setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .build()

            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, policy, request)
        }

        internal fun millisUntilNext8pm(timezoneId: String): Long {
            val zone =
                try {
                    ZoneId.of(timezoneId)
                } catch (e: Exception) {
                    ZoneId.systemDefault()
                }
            val now = ZonedDateTime.now(zone)
            val target8pm = now.toLocalDate().atTime(20, 0).atZone(zone)
            val next8pm = if (now.isBefore(target8pm)) target8pm else target8pm.plusDays(1)
            return next8pm.toInstant().toEpochMilli() - now.toInstant().toEpochMilli()
        }
    }
}
