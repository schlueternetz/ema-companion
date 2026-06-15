package com.schlueternetz.emaapistub

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * A scenario is the ordered script of interactions for a single ECU id.
 * One scenario file per ECU id; [interactions] are replayed in order.
 */
@Serializable
data class Scenario(
    val ecuId: String,
    val description: String = "",
    val interactions: List<Interaction>,
)

/** A single expected call and the response to return when it matches. */
@Serializable
data class Interaction(
    val request: RequestMatcher,
    val response: StubResponse,
)

/**
 * Matches an incoming request. Only the fields present here are asserted;
 * unspecified [pathParams] / [query] entries act as wildcards.
 */
@Serializable
data class RequestMatcher(
    val method: String = "GET",
    val path: String,
    val pathParams: Map<String, String> = emptyMap(),
    val query: Map<String, String> = emptyMap(),
)

/** The response to return on a match. [body] is the verbatim EMA response body. */
@Serializable
data class StubResponse(
    val status: Int = 200,
    val body: JsonElement,
)
