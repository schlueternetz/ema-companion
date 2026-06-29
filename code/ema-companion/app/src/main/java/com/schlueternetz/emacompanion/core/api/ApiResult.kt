package com.schlueternetz.emacompanion.core.api

/**
 * Typed outcome of an EMA API call. Expected outcomes are values, not thrown exceptions,
 * so they never cross the client boundary as raw exceptions.
 */
sealed interface ApiResult<out T> {
    data class Success<T>(
        val data: T,
    ) : ApiResult<T>

    /** The request could not reach the server (connection refused, timeout, no network). */
    data object NetworkError : ApiResult<Nothing>

    /** A non-zero EMA `code` or an HTTP error status, or a no-data response. */
    data class ApiError(
        val code: Int? = null,
        val httpStatus: Int? = null,
    ) : ApiResult<Nothing>

    /** The app is not fully configured; no request was issued. */
    data object ConfigurationError : ApiResult<Nothing>
}
