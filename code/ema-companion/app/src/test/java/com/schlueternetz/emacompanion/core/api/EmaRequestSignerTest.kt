package com.schlueternetz.emacompanion.core.api

import org.junit.Assert.assertEquals
import org.junit.Test

class EmaRequestSignerTest {

    private val timestamp = 1_700_000_000_000L
    private val nonce = "5e36eab8295911ee90751eff13c2920b"
    private val appId = "testappid1234567890123456789012"
    private val secret = "secret123456"

    private fun fixedSigner() = EmaRequestSigner(
        appId = appId,
        appSecret = secret,
        clock = { timestamp },
        nonceProvider = { nonce },
    )

    @Test
    fun buildsExactStringToSign() {
        val expected =
            "1700000000000/5e36eab8295911ee90751eff13c2920b/" +
                "testappid1234567890123456789012/203000001234/GET/HmacSHA256"
        assertEquals(
            expected,
            EmaRequestSigner.stringToSign(
                timestamp = timestamp,
                nonce = nonce,
                appId = appId,
                lastPathSegment = "203000001234",
                method = "GET",
            ),
        )
    }

    @Test
    fun producesExpectedBase64Signature() {
        val headers = fixedSigner().sign(method = "GET", lastPathSegment = "203000001234")
        assertEquals("Akz1RNHBeHxvD0fR+aLaS8cqTGSQnuvhAYAW+PCvrkQ=", headers.signature)
    }

    @Test
    fun producesAllFiveHeaders() {
        val headers = fixedSigner().sign(method = "GET", lastPathSegment = "203000001234")
        assertEquals(appId, headers.appId)
        assertEquals("1700000000000", headers.timestamp)
        assertEquals(nonce, headers.nonce)
        assertEquals("HmacSHA256", headers.signatureMethod)
        assertEquals("Akz1RNHBeHxvD0fR+aLaS8cqTGSQnuvhAYAW+PCvrkQ=", headers.signature)
    }

    @Test
    fun nonceIs32CharsWithoutDashes() {
        val realSigner = EmaRequestSigner(appId = appId, appSecret = secret)
        val headers = realSigner.sign(method = "GET", lastPathSegment = "203000001234")
        assertEquals(32, headers.nonce.length)
        assertEquals(false, headers.nonce.contains("-"))
    }

    @Test
    fun extractsLastPathSegment() {
        assertEquals(
            "203000001234",
            EmaRequestSigner.lastPathSegment("/user/api/v2/systems/sid/devices/ecu/energy/203000001234"),
        )
    }

    @Test
    fun extractsLastPathSegmentIgnoringTrailingSlash() {
        assertEquals(
            "203000001234",
            EmaRequestSigner.lastPathSegment("/user/api/v2/systems/sid/devices/ecu/energy/203000001234/"),
        )
    }
}
