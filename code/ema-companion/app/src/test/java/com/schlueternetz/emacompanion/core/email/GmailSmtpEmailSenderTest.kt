package com.schlueternetz.emacompanion.core.email

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetup
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GmailSmtpEmailSenderTest {
    private lateinit var greenMail: GreenMail
    private var smtpPort = 0

    @Before
    fun setUp() {
        // Port 0 → OS assigns a free ephemeral port; avoids "address in use" on CI
        greenMail = GreenMail(ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP))
        greenMail.setUser("sender@test.com", "testpass")
        greenMail.start()
        smtpPort = greenMail.smtp.port
    }

    @After
    fun tearDown() {
        greenMail.stop()
    }

    @Test
    fun send_deliversMessageWithCorrectToSubjectAndBody() {
        val sender =
            GmailSmtpEmailSender(
                from = "sender@test.com",
                appPassword = "testpass",
                smtpHost = "127.0.0.1",
                smtpPort = smtpPort,
                useTls = false,
            )

        val result =
            runBlocking {
                sender.send(
                    to = "recipient@test.com",
                    subject = "EMA Companion: Solar module offline",
                    body = "One or more modules are offline.",
                )
            }

        assertEquals(EmailResult.Success, result)
        greenMail.waitForIncomingEmail(1000, 1)
        val messages = greenMail.receivedMessages
        assertEquals(1, messages.size)
        assertEquals("EMA Companion: Solar module offline", messages[0].subject)
        assertTrue(messages[0].content.toString().contains("One or more modules are offline."))
    }

    @Test
    fun send_returnsAuthFailure_onWrongPassword() {
        val sender =
            GmailSmtpEmailSender(
                from = "sender@test.com",
                appPassword = "wrongpassword",
                smtpHost = "127.0.0.1",
                smtpPort = smtpPort,
                useTls = false,
            )

        val result =
            runBlocking {
                sender.send(to = "recipient@test.com", subject = "test", body = "body")
            }

        assertEquals(EmailResult.AuthFailure, result)
    }

    @Test
    fun send_returnsNetworkError_whenServerUnreachable() {
        val sender =
            GmailSmtpEmailSender(
                from = "sender@test.com",
                appPassword = "testpass",
                smtpHost = "127.0.0.1",
                smtpPort = 9999,
                useTls = false,
            )

        val result =
            runBlocking {
                sender.send(to = "recipient@test.com", subject = "test", body = "body")
            }

        assertEquals(EmailResult.NetworkError, result)
    }

    @Test
    fun testConnection_returnsSuccess_onValidCredentials() {
        val sender =
            GmailSmtpEmailSender(
                from = "sender@test.com",
                appPassword = "testpass",
                smtpHost = "127.0.0.1",
                smtpPort = smtpPort,
                useTls = false,
            )

        val result = runBlocking { sender.testConnection() }

        assertEquals(EmailResult.Success, result)
        assertEquals("testConnection must not send a message", 0, greenMail.receivedMessages.size)
    }

    @Test
    fun testConnection_returnsAuthFailure_onWrongPassword() {
        val sender =
            GmailSmtpEmailSender(
                from = "sender@test.com",
                appPassword = "wrongpassword",
                smtpHost = "127.0.0.1",
                smtpPort = smtpPort,
                useTls = false,
            )

        val result = runBlocking { sender.testConnection() }

        assertEquals(EmailResult.AuthFailure, result)
    }
}
