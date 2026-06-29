package com.schlueternetz.emaapistub

import kotlinx.serialization.json.JsonElement

/** A request as resolved by the routing layer, ready to match against a scenario. */
data class IncomingRequest(
    val method: String,
    val path: String,
    val pathParams: Map<String, String> = emptyMap(),
    val query: Map<String, String> = emptyMap(),
)

/** Outcome of dispatching a request through the engine. */
sealed interface MatchResult {
    /** The request matched the current interaction; serve [body] with [status]. */
    data class Matched(val status: Int, val body: JsonElement) : MatchResult

    /** The request did not match; the caller should fail loudly with a diagnostic. */
    data class Unexpected(val reason: String, val expected: RequestMatcher?, val actual: IncomingRequest) : MatchResult
}

/**
 * Record/replay engine. Holds a per-ECU cursor and matches each incoming request
 * against that ECU's next interaction in strict order, advancing only on a match.
 */
class MatchingEngine(private val scenarios: Map<String, Scenario>) {
    private val cursors = mutableMapOf<String, Int>()
    private val lock = Any()

    fun handle(
        ecuId: String,
        request: IncomingRequest,
    ): MatchResult =
        synchronized(lock) {
            val scenario =
                scenarios[ecuId]
                    ?: return MatchResult.Unexpected("No scenario configured for ECU id '$ecuId'", null, request)

            val cursor = cursors.getOrDefault(ecuId, 0)
            if (cursor >= scenario.interactions.size) {
                return MatchResult.Unexpected(
                    "No more interactions for ECU id '$ecuId' (all ${scenario.interactions.size} consumed)",
                    null,
                    request,
                )
            }

            val interaction = scenario.interactions[cursor]
            if (!matches(interaction.request, request)) {
                return MatchResult.Unexpected(
                    "Request does not match interaction #$cursor for ECU id '$ecuId'",
                    interaction.request,
                    request,
                )
            }

            cursors[ecuId] = cursor + 1
            MatchResult.Matched(interaction.response.status, interaction.response.body)
        }

    fun reset(): Unit = synchronized(lock) { cursors.clear() }

    fun reset(ecuId: String): Unit = synchronized(lock) { cursors.remove(ecuId) }

    private fun matches(
        matcher: RequestMatcher,
        request: IncomingRequest,
    ): Boolean {
        if (!matcher.method.equals(request.method, ignoreCase = true)) return false
        if (matcher.path != request.path) return false
        if (matcher.pathParams.any { (k, v) -> request.pathParams[k] != v }) return false
        if (matcher.query.any { (k, v) -> request.query[k] != v }) return false
        return true
    }
}
