package com.schlueternetz.emacompanion.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import javax.crypto.AEADBadTagException

class SettingsCryptoTest {

    private val json = """{"emaAppId":"test","displayMode":"dark"}"""
    private val pin = "1234"

    @Test
    fun encryptThenDecrypt_returnsOriginalJson() {
        val encrypted = SettingsCrypto.encrypt(json, pin)
        val decrypted = SettingsCrypto.decrypt(encrypted, pin)
        assertEquals(json, decrypted)
    }

    @Test(expected = AEADBadTagException::class)
    fun decrypt_wrongPin_throwsAEADBadTagException() {
        val encrypted = SettingsCrypto.encrypt(json, pin)
        SettingsCrypto.decrypt(encrypted, "9999")
    }

    @Test
    fun encrypt_sameInput_producesDifferentCiphertext() {
        val first = SettingsCrypto.encrypt(json, pin)
        val second = SettingsCrypto.encrypt(json, pin)
        assertNotEquals(first, second)
    }
}
