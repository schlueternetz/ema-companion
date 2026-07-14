package com.schlueternetz.emacompanion.feature.home

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.schlueternetz.emacompanion.core.api.modulehealth.Module
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthState
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthStatus
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ModuleHealthNotifierTest {
    private lateinit var context: Context
    private lateinit var manager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Shadows.shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        manager = context.getSystemService(NotificationManager::class.java)
        ModuleHealthNotifier.ensureChannelCreated(context)
    }

    @Test
    fun yellowStatus_postsNotification() {
        val state =
            ModuleHealthState(
                status = ModuleHealthStatus.YELLOW,
                offlineModules = listOf(Module("INV1", 1)),
            )
        ModuleHealthNotifier.notify(context, state)

        val shadow = Shadows.shadowOf(manager)
        assertEquals(1, shadow.allNotifications.size)
    }

    @Test
    fun yellowOnTwoConsecutiveChecks_postsNotificationBothTimes() {
        val state =
            ModuleHealthState(
                status = ModuleHealthStatus.YELLOW,
                offlineModules = listOf(Module("INV1", 1)),
            )
        ModuleHealthNotifier.notify(context, state)
        ModuleHealthNotifier.notify(context, state)

        val shadow = Shadows.shadowOf(manager)
        // Both checks post (replace) a notification — still only 1 active (replaced, not stacked)
        assertEquals(1, shadow.allNotifications.size)
    }

    @Test
    fun redStatus_postsNotification() {
        val state =
            ModuleHealthState(
                status = ModuleHealthStatus.RED,
                offlineModules = listOf(Module("INV1", 3)),
            )
        ModuleHealthNotifier.notify(context, state)

        val shadow = Shadows.shadowOf(manager)
        assertEquals(1, shadow.allNotifications.size)
    }

    @Test
    fun greenStatus_cancelsExistingNotification() {
        // First post a YELLOW notification
        ModuleHealthNotifier.notify(
            context,
            ModuleHealthState(ModuleHealthStatus.YELLOW, listOf(Module("INV1", 1))),
        )
        val shadow = Shadows.shadowOf(manager)
        assertEquals(1, shadow.allNotifications.size)

        // Then GREEN should cancel it
        ModuleHealthNotifier.notify(context, ModuleHealthState(ModuleHealthStatus.GREEN))
        assertEquals(0, shadow.allNotifications.size)
    }

    @Test
    fun unknownStatus_doesNotPostNotification() {
        ModuleHealthNotifier.notify(context, ModuleHealthState(ModuleHealthStatus.UNKNOWN))
        val shadow = Shadows.shadowOf(manager)
        assertEquals(0, shadow.allNotifications.size)
    }

    @Test
    fun greenStatus_withPostOnGreenTrue_postsConfirmationNotification() {
        ModuleHealthNotifier.notify(context, ModuleHealthState(ModuleHealthStatus.GREEN), postOnGreen = true)
        val shadow = Shadows.shadowOf(manager)
        assertEquals(1, shadow.allNotifications.size)
    }

    @Test
    fun greenStatus_withPostOnGreenTrue_consecutiveChecksReplaceNotStack() {
        val state = ModuleHealthState(ModuleHealthStatus.GREEN)
        ModuleHealthNotifier.notify(context, state, postOnGreen = true)
        ModuleHealthNotifier.notify(context, state, postOnGreen = true)

        val shadow = Shadows.shadowOf(manager)
        assertEquals(1, shadow.allNotifications.size)
    }

    @Test
    fun unknownStatus_withPostOnGreenTrue_stillDoesNotPostNotification() {
        ModuleHealthNotifier.notify(context, ModuleHealthState(ModuleHealthStatus.UNKNOWN), postOnGreen = true)
        val shadow = Shadows.shadowOf(manager)
        assertEquals(0, shadow.allNotifications.size)
    }
}
