package com.schlueternetz.emacompanion.core.api

import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Produces the five `X-CA-*` headers required to sign an EMA API request (manual §2.2).
 *
 * Pure and deterministic given an injected [clock] and [nonceProvider], so the exact
 * `stringToSign` and Base64 HMAC-SHA256 output can be asserted in unit tests.
 */
class EmaRequestSigner(
    private val appId: String,
    private val appSecret: String,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val nonceProvider: () -> String = { UUID.randomUUID().toString().replace("-", "") },
) {
    data class SignedHeaders(
        val appId: String,
        val timestamp: String,
        val nonce: String,
        val signatureMethod: String,
        val signature: String,
    )

    fun sign(
        method: String,
        lastPathSegment: String,
    ): SignedHeaders {
        val timestamp = clock()
        val nonce = nonceProvider()
        val toSign = stringToSign(timestamp, nonce, appId, lastPathSegment, method)
        return SignedHeaders(
            appId = appId,
            timestamp = timestamp.toString(),
            nonce = nonce,
            signatureMethod = SIGNATURE_METHOD,
            signature = hmacSha256Base64(toSign, appSecret),
        )
    }

    companion object {
        const val SIGNATURE_METHOD = "HmacSHA256"

        fun stringToSign(
            timestamp: Long,
            nonce: String,
            appId: String,
            lastPathSegment: String,
            method: String,
        ): String = "$timestamp/$nonce/$appId/$lastPathSegment/$method/$SIGNATURE_METHOD"

        fun lastPathSegment(path: String): String = path.trimEnd('/').substringAfterLast('/')

        private fun hmacSha256Base64(
            data: String,
            secret: String,
        ): String {
            val mac = Mac.getInstance(SIGNATURE_METHOD)
            val keyBytes = secret.toByteArray(Charsets.UTF_8)
            mac.init(SecretKeySpec(keyBytes, SIGNATURE_METHOD))
            val result = mac.doFinal(data.toByteArray(Charsets.UTF_8))
            return Base64.getEncoder().encodeToString(result)
        }
    }
}
