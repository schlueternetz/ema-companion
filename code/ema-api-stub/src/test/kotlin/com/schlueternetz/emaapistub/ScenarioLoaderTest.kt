package com.schlueternetz.emaapistub

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ScenarioLoaderTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `parses a scenario file into an ordered model indexed by ecu id`() {
        tempFolder.newFile("203000001234.json").writeText(
            """
            {
              "ecuId": "203000001234",
              "description": "Good Data",
              "interactions": [
                {
                  "request": {
                    "method": "GET",
                    "path": "/user/api/v2/systems/{sid}/devices/ecu/energy/{eid}",
                    "pathParams": { "eid": "203000001234" },
                    "query": { "energy_level": "minutely" }
                  },
                  "response": {
                    "body": { "code": 0, "data": { "power": [0, 8000] } }
                  }
                }
              ]
            }
            """.trimIndent(),
        )

        val scenarios = ScenarioLoader.loadFromDirectory(tempFolder.root)

        val scenario = scenarios.getValue("203000001234")
        assertEquals("Good Data", scenario.description)
        assertEquals(1, scenario.interactions.size)
        val interaction = scenario.interactions.first()
        assertEquals("GET", interaction.request.method)
        assertEquals("minutely", interaction.request.query["energy_level"])
        assertEquals(200, interaction.response.status)
        val code = interaction.response.body.jsonObject["code"]!!.jsonPrimitive.content
        assertEquals("0", code)
    }

    @Test
    fun `fails fast when the directory is missing`() {
        val missing = tempFolder.root.resolve("does-not-exist")
        assertThrows(ScenarioLoadException::class.java) {
            ScenarioLoader.loadFromDirectory(missing)
        }
    }

    @Test
    fun `fails fast on malformed json`() {
        tempFolder.newFile("broken.json").writeText("{ not valid json ")
        val ex =
            assertThrows(ScenarioLoadException::class.java) {
                ScenarioLoader.loadFromDirectory(tempFolder.root)
            }
        assertTrue(ex.message!!.contains("broken.json"))
    }
}
