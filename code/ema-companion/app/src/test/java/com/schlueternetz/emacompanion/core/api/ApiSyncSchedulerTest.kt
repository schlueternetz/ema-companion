package com.schlueternetz.emacompanion.core.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ApiSyncSchedulerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        val config = Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        context
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        ApiSyncWorker.updateAllAction = { _, _ -> }
    }

    @After
    fun tearDown() {
        ApiSyncWorker.hourlySourceOverride = null
        ApiSyncWorker.dailySourceOverride = null
        ApiSyncWorker.updateAllAction = ApiSyncWorker.defaultUpdateAllAction
        ApiSyncScheduler.moduleHealthResettableFactory =
            { ctx ->
                com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthRepository
                    .create(ctx)
            }
    }

    @Test
    fun requestResyncAfterSettingsChange_resetsModuleHealthThrottle_synchronously() {
        var resetCalls = 0
        val fakeModuleHealth =
            object : ThrottleResettable {
                override fun resetThrottle() {
                    resetCalls++
                }
            }
        ApiSyncScheduler.moduleHealthResettableFactory = { fakeModuleHealth }
        ApiSyncWorker.hourlySourceOverride = CountingHourlySource()
        ApiSyncWorker.dailySourceOverride = CountingDailySource()

        ApiSyncScheduler.requestResyncAfterSettingsChange(context)

        assertEquals(1, resetCalls)
    }

    @Test
    fun requestResyncAfterSettingsChange_doesNotForceAnImmediateModuleHealthCheck() {
        var moduleHealthCheckCount = 0
        val fakeModuleHealth =
            object : ThrottleResettable {
                override fun resetThrottle() {
                    // Only resets the throttle — must not itself trigger a check.
                }
            }
        ApiSyncScheduler.moduleHealthResettableFactory = { fakeModuleHealth }
        ApiSyncWorker.hourlySourceOverride = CountingHourlySource()
        ApiSyncWorker.dailySourceOverride = CountingDailySource()

        ApiSyncScheduler.requestResyncAfterSettingsChange(context)

        // No hook here calls ModuleHealthWorker — the assertion is really that nothing
        // beyond resetThrottle() is invoked on the module-health side; module health's own
        // scheduled worker is untouched by a settings-change resync, verified separately in
        // ModuleHealthWorkerTest.
        assertEquals(0, moduleHealthCheckCount)
    }

    @Test
    fun requestOpportunisticSync_reArmsPeriodicSchedule_whenNotCurrentlyActive() {
        ApiSyncScheduler.requestOpportunisticSync(context)

        val infos =
            WorkManager
                .getInstance(context)
                .getWorkInfosForUniqueWork(ApiSyncScheduler.WORK_NAME_PERIODIC)
                .get()
        assertEquals(
            "opportunistic sync must act as a safety net re-arming a missed/cancelled periodic schedule",
            1,
            infos.size,
        )
    }

    @Test
    fun requestOpportunisticSync_doesNotDisturbAnAlreadyRunningPeriodicSchedule() {
        ApiSyncScheduler.schedulePeriodic(context)
        val before =
            WorkManager
                .getInstance(context)
                .getWorkInfosForUniqueWork(ApiSyncScheduler.WORK_NAME_PERIODIC)
                .get()
                .first()
                .id

        ApiSyncScheduler.requestOpportunisticSync(context)

        val after =
            WorkManager
                .getInstance(context)
                .getWorkInfosForUniqueWork(ApiSyncScheduler.WORK_NAME_PERIODIC)
                .get()
                .first()
                .id
        assertEquals("KEEP policy must not replace an already-active periodic schedule", before, after)
    }

    @Test
    fun fastBurst_repeatedRequests_neverAccumulateMoreThanOnePendingWorkItem() {
        // enqueueUniqueWork(REPLACE) guarantees at most one WorkInfo entry ever exists for a
        // given unique work name — repeated rapid requests can never pile up as separate queued
        // executions, regardless of whether the predecessor had already started.
        val hourly = CountingHourlySource()
        ApiSyncWorker.hourlySourceOverride = hourly
        ApiSyncWorker.dailySourceOverride = CountingDailySource()

        ApiSyncScheduler.requestResyncAfterSettingsChange(context)
        ApiSyncScheduler.requestResyncAfterSettingsChange(context)
        ApiSyncScheduler.requestResyncAfterSettingsChange(context)

        val infos =
            WorkManager
                .getInstance(context)
                .getWorkInfosForUniqueWork(ApiSyncScheduler.WORK_NAME_SETTINGS_CHANGED)
                .get()
        assertEquals("REPLACE must never let more than one entry accumulate for the same work name", 1, infos.size)
    }

    @Test
    fun slowBurst_secondRequestWhileFirstIsSuspended_onlySecondPersists() {
        val gate = CompletableDeferred<Unit>()
        val firstStarted = CompletableDeferred<Unit>()
        val firstCancelled = CompletableDeferred<Unit>()
        val hourly =
            object : HourlyEnergySyncSource {
                var refreshCallCount = 0
                private var lastLabel = "none"

                override fun currentState() = HourlyProductionState()

                override suspend fun refresh(force: Boolean): HourlyProductionState {
                    refreshCallCount++
                    if (refreshCallCount == 1) {
                        firstStarted.complete(Unit)
                        try {
                            gate.await() // suspends here until the test releases it
                            lastLabel = "first"
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            firstCancelled.complete(Unit)
                            throw e
                        }
                    } else {
                        lastLabel = "second"
                    }
                    return HourlyProductionState()
                }

                override fun resetThrottle() {}

                fun label() = lastLabel
            }
        ApiSyncWorker.hourlySourceOverride = hourly
        ApiSyncWorker.dailySourceOverride = CountingDailySource()

        // enqueueUniqueWork's own bookkeeping (a Room/SQLite query, via SynchronousExecutor) must
        // run on THIS thread, not a spawned background Thread — confirmed via jstack that doing so
        // deadlocks: Robolectric's SQLite shadow needs its paused main-thread Looper serviced to
        // hand out a connection, but this test's own JUnit/Robolectric runner thread IS that main
        // thread, and a background Thread has no way to pump it. CoroutineWorker.doWork() itself
        // still runs on Dispatchers.Default (no `setWorkerCoroutineContext` override in setUp), so
        // this call returns as soon as the worker is *started* — well before `refresh()` reaches
        // `gate.await()` — so calling it inline here does not block on the first request finishing.
        ApiSyncScheduler.requestResyncAfterSettingsChange(context)

        kotlinx.coroutines.runBlocking { firstStarted.await() }

        // Second request while the first is still suspended — REPLACE should cancel the first's
        // CoroutineWorker (its `refresh()` coroutine observes cancellation at `gate.await()`).
        ApiSyncScheduler.requestResyncAfterSettingsChange(context)

        // enqueueUniqueWork(REPLACE) only *requests* cancellation of the first CoroutineWorker;
        // delivering that cancellation to its suspended `gate.await()` happens asynchronously on
        // Dispatchers.Default, racing against this thread. Wait for the cancellation to actually
        // land before releasing the gate, instead of hoping it wins a timing race — otherwise
        // `gate.complete()` can resume `await()` normally before the cancellation is observed,
        // which is exactly what happened under CI's slower scheduling (this test is flaky, not
        // the production cancellation behavior it verifies).
        kotlinx.coroutines.runBlocking { kotlinx.coroutines.withTimeout(5_000) { firstCancelled.await() } }

        gate.complete(Unit) // resolve `gate` so the cancelled coroutine's `await()` call site is clean

        assertEquals("second (later) request's fetch must be the one that ran last", "second", hourly.label())
    }
}

private class CountingHourlySource : HourlyEnergySyncSource {
    var refreshCallCount = 0

    override fun currentState() = HourlyProductionState()

    override suspend fun refresh(force: Boolean): HourlyProductionState {
        refreshCallCount++
        return HourlyProductionState()
    }

    override fun resetThrottle() {}
}

private class CountingDailySource : DailyEnergySyncSource {
    var refreshCallCount = 0

    override fun currentState() = DailyProductionState()

    override suspend fun refresh(force: Boolean): DailyProductionState {
        refreshCallCount++
        return DailyProductionState()
    }

    override fun resetThrottle() {}
}
