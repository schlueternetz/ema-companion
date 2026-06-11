package com.schlueternetz.emacompanion.feature.settings

object SettingsTestLoader {
    fun loadFixture(repository: SettingsRepository, resourcePath: String) {
        val json = SettingsTestLoader::class.java.classLoader!!
            .getResourceAsStream(resourcePath)!!
            .bufferedReader()
            .readText()
        repository.importFromJson(json)
    }
}
