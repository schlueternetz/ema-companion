package com.schlueternetz.emacompanion.feature.widgets

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.schlueternetz.emacompanion.core.api.ApiSyncScheduler
import com.schlueternetz.emacompanion.core.api.ApiSyncWorker
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
class WidgetPlacementHooksTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        val config = Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @After
    fun tearDown() {
        ApiSyncWorker.hasConsumingWidgetPlacedAction = ApiSyncWorker.defaultHasConsumingWidgetPlacedAction
    }

    private fun periodicWorkInfos(): List<WorkInfo> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork(ApiSyncScheduler.WORK_NAME_PERIODIC).get()

    @Test
    fun onEnabled_armsThePeriodicSchedule_forEachWidgetType() {
        TodayProductionWidgetReceiver().onEnabled(context)
        assertEquals(1, periodicWorkInfos().size)

        ProductionSummaryWidgetReceiver().onEnabled(context)
        assertEquals(1, periodicWorkInfos().size) // KEEP policy: still just one entry

        ProductionHistoryWidgetReceiver().onEnabled(context)
        assertEquals(1, periodicWorkInfos().size)
    }

    @Test
    fun onDisabled_cancelsSchedule_whenNoWidgetOfAnyTypeRemainsPlaced() {
        ApiSyncWorker.hasConsumingWidgetPlacedAction = { false }
        ApiSyncScheduler.schedulePeriodic(context)

        TodayProductionWidgetReceiver().onDisabled(context)

        val infos = periodicWorkInfos()
        assertTrue(infos.isEmpty() || infos.first().state == WorkInfo.State.CANCELLED)
    }

    @Test
    fun onDisabled_keepsScheduleActive_whenAnotherWidgetTypeStillPlaced() {
        ApiSyncWorker.hasConsumingWidgetPlacedAction = { true } // simulates another widget type still placed
        ApiSyncScheduler.schedulePeriodic(context)

        ProductionSummaryWidgetReceiver().onDisabled(context)

        val infos = periodicWorkInfos()
        assertEquals(1, infos.size)
        assertTrue("schedule must remain active, not cancelled", infos.first().state != WorkInfo.State.CANCELLED)
    }
}
