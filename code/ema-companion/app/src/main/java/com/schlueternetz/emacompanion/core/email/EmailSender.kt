package com.schlueternetz.emacompanion.core.email

interface EmailSender {
    suspend fun send(
        to: String,
        subject: String,
        body: String,
    ): EmailResult

    suspend fun testConnection(): EmailResult
}

sealed class EmailResult {
    object Success : EmailResult()

    object AuthFailure : EmailResult()

    object NetworkError : EmailResult()
}
