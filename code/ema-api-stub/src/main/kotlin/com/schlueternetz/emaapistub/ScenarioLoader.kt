package com.schlueternetz.emaapistub

import kotlinx.serialization.json.Json
import java.io.File

/** Thrown when scenario configuration cannot be loaded; fails the stub fast. */
class ScenarioLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Loads per-ECU scenario files and indexes them by ECU id. */
object ScenarioLoader {
    private val json = Json { ignoreUnknownKeys = true }

    /** Loads every `*.json` file in [dir], indexed by [Scenario.ecuId]. Fails fast on any error. */
    fun loadFromDirectory(dir: File): Map<String, Scenario> {
        if (!dir.isDirectory) {
            throw ScenarioLoadException("Scenario directory not found: ${dir.absolutePath}")
        }
        val files = dir.listFiles { f -> f.isFile && f.extension == "json" }?.sorted().orEmpty()
        val scenarios = mutableMapOf<String, Scenario>()
        for (file in files) {
            val scenario =
                try {
                    json.decodeFromString<Scenario>(file.readText())
                } catch (e: Exception) {
                    throw ScenarioLoadException("Failed to parse scenario file: ${file.absolutePath}", e)
                }
            val existing = scenarios.put(scenario.ecuId, scenario)
            if (existing != null) {
                throw ScenarioLoadException("Duplicate scenario for ECU id ${scenario.ecuId} in ${file.absolutePath}")
            }
        }
        return scenarios
    }

    /** Loads the scenario set bundled on the classpath under `scenarios/`. */
    fun loadDefault(): Map<String, Scenario> {
        val url =
            javaClass.classLoader.getResource("scenarios")
                ?: throw ScenarioLoadException("Bundled scenarios directory not found on classpath")
        return loadFromDirectory(File(url.toURI()))
    }
}
