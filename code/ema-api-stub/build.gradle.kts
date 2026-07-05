import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    `java-library`
    application
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}

group = "com.schlueternetz.emaapistub"
version = "1.0"

val ktorVersion = "3.0.3"

dependencies {
    // `api` (not `implementation`): the Companion app's embedded-stub tests consume
    // MatchingEngine/ScenarioLoader/stubModule directly and need these types on their own
    // compile classpath, via this project as a composite-build dependency.
    api("io.ktor:ktor-server-core:$ktorVersion")
    api("io.ktor:ktor-server-cio:$ktorVersion")
    api("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    api("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("com.schlueternetz.emaapistub.ApplicationKt")
}

tasks.test {
    useJUnit()
}
