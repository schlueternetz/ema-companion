package com.schlueternetz.emaapistub

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pins the shipped Good Data deliverable, since the engine serves scenario files verbatim. */
class GoodDataScenarioTest {
    private val goodDataEcuId = "203000001234"

    @Test
    fun `bundled good data ecu has hourly interaction first`() {
        val scenarios = ScenarioLoader.loadDefault()
        val scenario = scenarios.getValue(goodDataEcuId)
        val interaction = scenario.interactions.first()

        assertEquals(ECU_ENERGY_PATH, interaction.request.path)
        assertEquals("hourly", interaction.request.query["energy_level"])
        val hourlyData = interaction.response.body.jsonObject["data"]!!.jsonArray
        assertTrue(hourlyData.size <= 24)
    }

    @Test
    fun `bundled good data ecu has daily interaction after hourly`() {
        val scenarios = ScenarioLoader.loadDefault()
        val scenario = scenarios.getValue(goodDataEcuId)

        val daily = scenario.interactions[1]
        assertEquals("daily", daily.request.query["energy_level"])
        val dailyData = daily.response.body.jsonObject["data"]!!.jsonArray
        assertEquals(31, dailyData.size)
    }
}
