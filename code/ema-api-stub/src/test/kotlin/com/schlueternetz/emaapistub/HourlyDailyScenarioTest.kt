package com.schlueternetz.emaapistub

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Scoped-directory tests (decision 2) for hourly/daily interactions — loaded via
 * `ScenarioLoader.loadFromDirectory` against a per-test [TemporaryFolder], never `loadDefault()`,
 * so these pass regardless of the bundled default set's state.
 */
class HourlyDailyScenarioTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val ecuId = "203000001234"

    private fun engineFrom(
        fileName: String,
        json: String,
    ): MatchingEngine {
        tempFolder.newFile(fileName).writeText(json)
        return MatchingEngine(ScenarioLoader.loadFromDirectory(tempFolder.root))
    }

    private fun request(energyLevel: String) =
        IncomingRequest(
            method = "GET",
            path = "/user/api/v2/systems/{sid}/devices/ecu/energy/{eid}",
            pathParams = mapOf("sid" to "ANYSID", "eid" to ecuId),
            query = mapOf("energy_level" to energyLevel),
        )

    @Test
    fun `hourly interaction returns at most 24 entries indexed by hour`() {
        val engine =
            engineFrom(
                "hourly.json",
                """
                {
                  "ecuId": "$ecuId",
                  "description": "Hourly-only fixture",
                  "interactions": [
                    {
                      "request": {
                        "method": "GET",
                        "path": "/user/api/v2/systems/{sid}/devices/ecu/energy/{eid}",
                        "pathParams": { "eid": "$ecuId" },
                        "query": { "energy_level": "hourly" }
                      },
                      "response": {
                        "body": { "code": 0, "data": [null, null, "1.00", "2.50", null] }
                      }
                    }
                  ]
                }
                """.trimIndent(),
            )

        val result = engine.handle(ecuId, request("hourly")) as MatchResult.Matched

        val body = result.body.jsonObject
        assertEquals("0", body["code"]!!.jsonPrimitive.content)
        val data = body["data"]!!.jsonArray
        assertTrue(data.size <= 24)
        assertEquals(JsonNull, data[0])
        assertEquals("1.00", data[2].jsonPrimitive.content)
    }

    @Test
    fun `daily interaction returns one entry per day of the requested month`() {
        // September has 30 days.
        val days = (1..30).map { "\"$it.10\"" }.joinToString(",")
        val engine =
            engineFrom(
                "daily.json",
                """
                {
                  "ecuId": "$ecuId",
                  "description": "Daily-only fixture (30-day month)",
                  "interactions": [
                    {
                      "request": {
                        "method": "GET",
                        "path": "/user/api/v2/systems/{sid}/devices/ecu/energy/{eid}",
                        "pathParams": { "eid": "$ecuId" },
                        "query": { "energy_level": "daily" }
                      },
                      "response": {
                        "body": { "code": 0, "data": [$days] }
                      }
                    }
                  ]
                }
                """.trimIndent(),
            )

        val result = engine.handle(ecuId, request("daily")) as MatchResult.Matched

        val data = result.body.jsonObject["data"]!!.jsonArray
        assertEquals(30, data.size)
    }

    @Test
    fun `no-data fixture serves code 1001 for both hourly and daily`() {
        val engine =
            engineFrom(
                "no-data.json",
                """
                {
                  "ecuId": "$ecuId",
                  "description": "No-data (code 1001) fixture for hourly and daily",
                  "interactions": [
                    {
                      "request": {
                        "method": "GET",
                        "path": "/user/api/v2/systems/{sid}/devices/ecu/energy/{eid}",
                        "pathParams": { "eid": "$ecuId" },
                        "query": { "energy_level": "hourly" }
                      },
                      "response": { "body": { "code": 1001 } }
                    },
                    {
                      "request": {
                        "method": "GET",
                        "path": "/user/api/v2/systems/{sid}/devices/ecu/energy/{eid}",
                        "pathParams": { "eid": "$ecuId" },
                        "query": { "energy_level": "daily" }
                      },
                      "response": { "body": { "code": 1001 } }
                    }
                  ]
                }
                """.trimIndent(),
            )

        val hourly = engine.handle(ecuId, request("hourly")) as MatchResult.Matched
        val daily = engine.handle(ecuId, request("daily")) as MatchResult.Matched

        assertEquals("1001", hourly.body.jsonObject["code"]!!.jsonPrimitive.content)
        assertEquals("1001", daily.body.jsonObject["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun `multi-month daily fixture returns each month's response in requested order`() {
        val engine =
            engineFrom(
                "multi-month-daily.json",
                """
                {
                  "ecuId": "$ecuId",
                  "description": "Two-calendar-month daily fixture (July then August)",
                  "interactions": [
                    {
                      "request": {
                        "method": "GET",
                        "path": "/user/api/v2/systems/{sid}/devices/ecu/energy/{eid}",
                        "pathParams": { "eid": "$ecuId" },
                        "query": { "energy_level": "daily" }
                      },
                      "response": { "body": { "code": 0, "data": ["1.11"], "month": "july" } }
                    },
                    {
                      "request": {
                        "method": "GET",
                        "path": "/user/api/v2/systems/{sid}/devices/ecu/energy/{eid}",
                        "pathParams": { "eid": "$ecuId" },
                        "query": { "energy_level": "daily" }
                      },
                      "response": { "body": { "code": 0, "data": ["2.22"], "month": "august" } }
                    }
                  ]
                }
                """.trimIndent(),
            )

        val july = engine.handle(ecuId, request("daily")) as MatchResult.Matched
        val august = engine.handle(ecuId, request("daily")) as MatchResult.Matched

        assertEquals("july", july.body.jsonObject["month"]!!.jsonPrimitive.content)
        assertEquals("august", august.body.jsonObject["month"]!!.jsonPrimitive.content)
    }
}
