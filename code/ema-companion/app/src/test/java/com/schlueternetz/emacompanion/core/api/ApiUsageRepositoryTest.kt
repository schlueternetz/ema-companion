package com.schlueternetz.emacompanion.core.api

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ApiUsageRepositoryTest {
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        prefs =
            ApplicationProvider
                .getApplicationContext<Context>()
                .getSharedPreferences("ema_api_usage_test", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    private fun repo(month: String = "2026-06") = ApiUsageRepository(prefs, monthProvider = { month })

    @Test
    fun recordRequest_incrementsWithinMonth() {
        val repo = repo()
        repo.recordRequest()
        repo.recordRequest()
        repo.recordRequest()
        assertEquals(3, repo.getRequestCount())
    }

    @Test
    fun count_persistsAcrossInstances() {
        repo().recordRequest()
        repo().recordRequest()
        // Fresh instance over the same backing file.
        assertEquals(2, repo().getRequestCount())
    }

    @Test
    fun recordRequest_resetsCountOnMonthRollover() {
        repo(month = "2026-05").recordRequest()
        repo(month = "2026-05").recordRequest()
        // New month: the first counted request yields 1.
        val june = repo(month = "2026-06")
        june.recordRequest()
        assertEquals(1, june.getRequestCount())
    }

    @Test
    fun count_isZeroWhenStoredMonthDiffersAndNoNewRequest() {
        repo(month = "2026-05").recordRequest()
        assertEquals(0, repo(month = "2026-06").getRequestCount())
    }

    @Test
    fun lastFetch_isPersisted() {
        val repo = repo()
        repo.setLastFetchEpochMs(123_456L)
        assertEquals(123_456L, repo().getLastFetchEpochMs())
    }

    @Test
    fun clear_resetsCountAndLastFetch() {
        val repo = repo()
        repo.recordRequest()
        repo.setLastFetchEpochMs(999L)
        repo.clear()
        assertEquals(0, repo.getRequestCount())
        assertEquals(0L, repo.getLastFetchEpochMs())
    }
}
