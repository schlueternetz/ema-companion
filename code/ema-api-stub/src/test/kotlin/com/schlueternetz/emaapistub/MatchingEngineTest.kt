package com.schlueternetz.emaapistub

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchingEngineTest {
    private val ecuId = "203000001234"

    private fun body(json: String): JsonElement = Json.parseToJsonElement(json)

    private fun matcher(
        energyLevel: String = "minutely",
        eid: String = ecuId,
    ) = RequestMatcher(
        method = "GET",
        path = "/user/api/v2/systems/{sid}/devices/ecu/energy/{eid}",
        pathParams = mapOf("eid" to eid),
        query = mapOf("energy_level" to energyLevel),
    )

    private fun request(
        energyLevel: String = "minutely",
        sid: String = "ANYSID",
        eid: String = ecuId,
    ) = IncomingRequest(
        method = "GET",
        path = "/user/api/v2/systems/{sid}/devices/ecu/energy/{eid}",
        pathParams = mapOf("sid" to sid, "eid" to eid),
        query = mapOf("energy_level" to energyLevel),
    )

    private fun engineWith(vararg interactions: Interaction) =
        MatchingEngine(mapOf(ecuId to Scenario(ecuId = ecuId, interactions = interactions.toList())))

    @Test
    fun `matching request returns response and advances cursor`() {
        val engine = engineWith(Interaction(matcher(), StubResponse(body = body("""{"code":0}"""))))

        val result = engine.handle(ecuId, request())

        assertTrue(result is MatchResult.Matched)
        result as MatchResult.Matched
        assertEquals(200, result.status)
        assertEquals("0", result.body.jsonObject["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun `unspecified params act as wildcards`() {
        // matcher pins eid + energy_level but not sid; request uses an arbitrary sid
        val engine = engineWith(Interaction(matcher(), StubResponse(body = body("""{"code":0}"""))))

        val result = engine.handle(ecuId, request(sid = "WHATEVER-SID"))

        assertTrue(result is MatchResult.Matched)
    }

    @Test
    fun `same endpoint listed twice returns responses in order`() {
        val engine =
            engineWith(
                Interaction(matcher(), StubResponse(body = body("""{"step":1}"""))),
                Interaction(matcher(), StubResponse(body = body("""{"step":2}"""))),
            )

        val first = engine.handle(ecuId, request()) as MatchResult.Matched
        val second = engine.handle(ecuId, request()) as MatchResult.Matched

        assertEquals("1", first.body.jsonObject["step"]!!.jsonPrimitive.content)
        assertEquals("2", second.body.jsonObject["step"]!!.jsonPrimitive.content)
    }

    @Test
    fun `scripted ema error body is served as a normal match`() {
        val engine = engineWith(Interaction(matcher(), StubResponse(body = body("""{"code":1001}"""))))

        val result = engine.handle(ecuId, request()) as MatchResult.Matched

        assertEquals("1001", result.body.jsonObject["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun `request not matching current interaction is unexpected and does not advance`() {
        val engine =
            engineWith(
                Interaction(matcher(energyLevel = "minutely"), StubResponse(body = body("""{"step":1}"""))),
            )

        val mismatch = engine.handle(ecuId, request(energyLevel = "hourly"))
        assertTrue(mismatch is MatchResult.Unexpected)

        // cursor did not advance: the correct call still matches interaction #0
        val correct = engine.handle(ecuId, request(energyLevel = "minutely"))
        assertTrue(correct is MatchResult.Matched)
    }

    @Test
    fun `unknown ecu id is unexpected`() {
        val engine = engineWith(Interaction(matcher(), StubResponse(body = body("""{"code":0}"""))))

        val result = engine.handle("999999999999", request(eid = "999999999999"))

        assertTrue(result is MatchResult.Unexpected)
    }

    @Test
    fun `call after last interaction is unexpected`() {
        val engine = engineWith(Interaction(matcher(), StubResponse(body = body("""{"code":0}"""))))

        engine.handle(ecuId, request()) // consume the only interaction
        val result = engine.handle(ecuId, request())

        assertTrue(result is MatchResult.Unexpected)
    }

    @Test
    fun `reset returns cursor to first interaction`() {
        val engine = engineWith(Interaction(matcher(), StubResponse(body = body("""{"code":0}"""))))

        engine.handle(ecuId, request()) // consume
        engine.reset()
        val result = engine.handle(ecuId, request())

        assertTrue(result is MatchResult.Matched)
    }
}
