package com.schlueternetz.emacompanion.core.api

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private object FakeLifecycleOwner : LifecycleOwner {
    override val lifecycle: Lifecycle
        get() = throw UnsupportedOperationException("not used by AppForegroundTracker")
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppForegroundTrackerTest {
    private lateinit var prefs: SharedPreferences
    private var now = 10_000_000L

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        prefs = ctx.getSharedPreferences("foreground_t", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    private fun tracker() = AppForegroundTracker(prefs, clock = { now })

    @Test
    fun beforeAnyLifecycleEvent_isStale() {
        assertFalse(tracker().isRecentlyForegrounded())
    }

    @Test
    fun afterOnStart_isRecentlyForegrounded() {
        val t = tracker()
        t.onStart(FakeLifecycleOwner)
        assertTrue(t.isRecentlyForegrounded())
    }

    @Test
    fun remainsForegrounded_evenAfterGraceWindowElapses_ifNeverStopped() {
        val t = tracker()
        t.onStart(FakeLifecycleOwner)
        now += AppForegroundTracker.GRACE_WINDOW_MS + 60_000L
        assertTrue(t.isRecentlyForegrounded())
    }

    @Test
    fun afterOnStop_staysRecentWithinGraceWindow() {
        val t = tracker()
        t.onStart(FakeLifecycleOwner)
        t.onStop(FakeLifecycleOwner)
        now += AppForegroundTracker.GRACE_WINDOW_MS - 1_000L
        assertTrue(t.isRecentlyForegrounded())
    }

    @Test
    fun afterOnStop_isStaleOutsideGraceWindow() {
        val t = tracker()
        t.onStart(FakeLifecycleOwner)
        t.onStop(FakeLifecycleOwner)
        now += AppForegroundTracker.GRACE_WINDOW_MS + 1_000L
        assertFalse(t.isRecentlyForegrounded())
    }

    @Test
    fun survivesNewInstance_readingSamePersistedPrefs() {
        val t = tracker()
        t.onStart(FakeLifecycleOwner)
        t.onStop(FakeLifecycleOwner)
        val t2 = AppForegroundTracker(prefs, clock = { now })
        assertTrue(t2.isRecentlyForegrounded())
    }
}
