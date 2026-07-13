package com.schlueternetz.emaapistub

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class ApplicationTest {
    private val goodDataEcuId = "203000001234"
    private val hourlyUrl =
        "/user/api/v2/systems/SID123/devices/ecu/energy/$goodDataEcuId?energy_level=hourly"

    private fun ApplicationTestBuilder.installStub() {
        application { stubModule(MatchingEngine(ScenarioLoader.loadDefault())) }
    }

    @Test
    fun `good data hourly call returns code 0 with hourly data`() =
        testApplication {
            installStub()

            val response = client.get(hourlyUrl)

            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals("0", body["code"]!!.jsonPrimitive.content)
            val data = body["data"]!!.jsonArray
            assertEquals("0.42", data[6].jsonPrimitive.content)
        }

    @Test
    fun `unexpected energy level returns 409 diagnostic`() =
        testApplication {
            installStub()

            val response =
                client.get("/user/api/v2/systems/SID123/devices/ecu/energy/$goodDataEcuId?energy_level=daily")

            assertEquals(HttpStatusCode.Conflict, response.status)
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals("unexpected_call", body["error"]!!.jsonPrimitive.content)
        }

    @Test
    fun `unknown ecu id returns 409 diagnostic`() =
        testApplication {
            installStub()

            val response =
                client.get("/user/api/v2/systems/SID123/devices/ecu/energy/999999999999?energy_level=hourly")

            assertEquals(HttpStatusCode.Conflict, response.status)
        }

    @Test
    fun `unrecognized route returns 404`() =
        testApplication {
            installStub()

            val response = client.get("/no/such/endpoint")

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `reset all rewinds the cursor so the call can be replayed`() =
        testApplication {
            installStub()

            assertEquals(HttpStatusCode.OK, client.get(hourlyUrl).status)
            // cursor consumed; a second call would be unexpected
            assertEquals(HttpStatusCode.Conflict, client.get(hourlyUrl).status)

            assertEquals(HttpStatusCode.OK, client.post("/__stub__/reset").status)
            assertEquals(HttpStatusCode.OK, client.get(hourlyUrl).status)
        }

    @Test
    fun `reset single ecu rewinds only that cursor`() =
        testApplication {
            installStub()

            client.get(hourlyUrl)
            assertEquals(HttpStatusCode.OK, client.post("/__stub__/reset/$goodDataEcuId").status)
            assertEquals(HttpStatusCode.OK, client.get(hourlyUrl).status)
        }
}
