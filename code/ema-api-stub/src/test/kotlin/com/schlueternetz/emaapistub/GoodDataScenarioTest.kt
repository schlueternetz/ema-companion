package com.schlueternetz.emaapistub

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pins the shipped Good Data deliverable, since the engine serves scenario files verbatim. */
class GoodDataScenarioTest {
    private val goodDataEcuId = "203000001234"

    @Test
    fun `bundled good data ecu reports current production of 8000 W via minutely`() {
        val scenarios = ScenarioLoader.loadDefault()
        val scenario = scenarios.getValue(goodDataEcuId)
        val interaction = scenario.interactions.first()

        // First interaction is the ECU minutely energy endpoint.
        assertEquals(ECU_ENERGY_PATH, interaction.request.path)
        assertEquals("minutely", interaction.request.query["energy_level"])

        val data = interaction.response.body.jsonObject["data"]!!.jsonObject
        val time = data["time"]!!.jsonArray
        val power = data["power"]!!.jsonArray
        val energy = data["energy"]!!.jsonArray

        assertEquals(time.size, power.size)
        assertEquals(time.size, energy.size)
        assertEquals(8000, power.last().jsonPrimitive.content.toInt())
    }
}
