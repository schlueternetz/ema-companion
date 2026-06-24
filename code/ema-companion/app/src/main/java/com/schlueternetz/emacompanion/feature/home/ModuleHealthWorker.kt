package com.schlueternetz.emacompanion.feature.home

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.schlueternetz.emacompanion.core.AppConfig
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthRepository
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class ModuleHealthWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = ModuleHealthRepository.create(applicationContext)
        val state = repo.refresh()
        ModuleHealthNotifier.ensureChannelCreated(applicationContext)
        ModuleHealthNotifier.notify(applicationContext, state)
        return Result.success()
    }

    companion object {
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
            val request = PeriodicWorkRequestBuilder<ModuleHealthWorker>(
                AppConfig.MODULE_HEALTH_CHECK_INTERVAL_MS,
                TimeUnit.MILLISECONDS,
                // flexInterval: allow system to run anywhere in the last hour of the period
                AppConfig.MODULE_HEALTH_CHECK_INTERVAL_MS / 24,
                TimeUnit.MILLISECONDS,
            )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, policy, request)
        }

        internal fun millisUntilNext8pm(timezoneId: String): Long {
            val zone = try {
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
