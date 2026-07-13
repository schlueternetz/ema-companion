package com.schlueternetz.emacompanion.core.api

/** Why the last fetch failed, shown as a status line inside the Home tile. */
enum class FetchError {
    /** Never reached EMA (no connection, timeout). */
    NETWORK,

    /** Reached EMA but the credentials/authorization were rejected. */
    AUTH,

    /** Reached EMA but it returned some other error (bad parameters, server error, …). */
    API,
}
