package com.schlueternetz.emacompanion.core.api.log

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ApiCallLogRepositoryTest {

    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("ema_api_log_test", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    private fun repo() = ApiCallLogRepository(prefs)

    private fun log(endpoint: String, success: Boolean = true) = ApiCallLog(
        timestampMs = 1_000L,
        endpoint = endpoint,
        durationMs = 42L,
        success = success,
        requestText = "GET /$endpoint",
        responseText = """{"code":0}""",
    )

    @Test
    fun append_storesRecord() {
        repo().append(log("ecu/energy"))
        val all = repo().getAll()
        assertEquals(1, all.size)
        assertEquals("ecu/energy", all[0].endpoint)
        assertEquals(42L, all[0].durationMs)
        assertTrue(all[0].success)
    }

    @Test
    fun append_ordersNewestFirst() {
        val repo = repo()
        repo.append(log("first"))
        repo.append(log("second"))
        val all = repo.getAll()
        assertEquals("second", all[0].endpoint)
        assertEquals("first", all[1].endpoint)
    }

    @Test
    fun append_capsAt100DroppingOldest() {
        val repo = repo()
        for (i in 1..105) repo.append(log(i.toString()))
        val all = repo.getAll()
        assertEquals(100, all.size)
        assertEquals("105", all[0].endpoint)
        assertEquals("6", all[99].endpoint)
    }

    @Test
    fun append_masksSecretInStoredText() {
        val secret = "secret123456"
        val entry = ApiCallLog(
            timestampMs = 1L,
            endpoint = "ecu/energy",
            durationMs = 1L,
            success = true,
            requestText = "App Secret: $secret in plain text",
            responseText = "no secret here",
        )
        repo().append(entry, secret = secret)
        val stored = repo().getAll()[0]
        assertFalse("Secret must not appear in plain text", stored.requestText.contains(secret))
        assertTrue(stored.requestText.contains("3456"))
    }

    @Test
    fun clear_removesAllRecords() {
        val repo = repo()
        repo.append(log("ecu/energy"))
        repo.clear()
        assertTrue(repo.getAll().isEmpty())
    }
}
