package com.schlueternetz.emacompanion.core.email

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class GmailSmtpEmailSender(
    private val from: String,
    private val appPassword: String,
    private val smtpHost: String = "smtp.gmail.com",
    private val smtpPort: Int = 587,
    private val useTls: Boolean = true,
) : EmailSender {

    override suspend fun send(to: String, subject: String, body: String): EmailResult =
        withContext(Dispatchers.IO) {
            try {
                val session = buildSession()
                val fromAddress = from
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(fromAddress))
                    setRecipient(Message.RecipientType.TO, InternetAddress(to))
                    setSubject(subject)
                    setText(body)
                }
                val recipients = arrayOf<javax.mail.Address>(InternetAddress(to))
                val transport = session.getTransport("smtp")
                transport.connect(smtpHost, smtpPort, from, appPassword)
                transport.sendMessage(message, recipients)
                transport.close()
                EmailResult.Success
            } catch (e: javax.mail.AuthenticationFailedException) {
                EmailResult.AuthFailure
            } catch (e: MessagingException) {
                EmailResult.NetworkError
            }
        }

    override suspend fun testConnection(): EmailResult =
        withContext(Dispatchers.IO) {
            try {
                val session = buildSession()
                val transport = session.getTransport("smtp")
                transport.connect(smtpHost, smtpPort, from, appPassword)
                transport.close()
                EmailResult.Success
            } catch (e: javax.mail.AuthenticationFailedException) {
                EmailResult.AuthFailure
            } catch (e: MessagingException) {
                EmailResult.NetworkError
            }
        }

    private fun buildSession(): Session {
        val props = Properties().apply {
            put("mail.smtp.host", smtpHost)
            put("mail.smtp.port", smtpPort.toString())
            put("mail.smtp.auth", "true")
            if (useTls) {
                put("mail.smtp.starttls.enable", "true")
            }
        }
        val auth = object : Authenticator() {
            override fun getPasswordAuthentication() = PasswordAuthentication(from, appPassword)
        }
        return Session.getInstance(props, auth)
    }
}
