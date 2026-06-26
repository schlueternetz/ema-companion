package com.schlueternetz.emacompanion.core.email

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.schlueternetz.emacompanion.core.api.modulehealth.Module
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "en")
class EmailContentBuilderEnTest {

    private lateinit var builder: EmailContentBuilder

    @Before
    fun setUp() {
        builder = EmailContentBuilder(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun buildSubject_yellow_returnsEnglishSubject() {
        assertEquals(
            "EMA Companion: Solar module offline",
            builder.buildSubject(ModuleHealthStatus.YELLOW),
        )
    }

    @Test
    fun buildSubject_red_returnsEnglishSubject() {
        assertEquals(
            "EMA Companion: Solar module offline — action needed",
            builder.buildSubject(ModuleHealthStatus.RED),
        )
    }

    @Test
    fun buildSubject_green_returnsEnglishSubject() {
        assertEquals(
            "EMA Companion: All modules producing",
            builder.buildSubject(ModuleHealthStatus.GREEN),
        )
    }

    @Test
    fun buildBody_yellow_containsIntroAndModuleListAndCta() {
        val modules = listOf(Module("INV1", 1), Module("INV2", 2))
        val body = builder.buildBody(ModuleHealthStatus.YELLOW, modules)

        assertTrue(body.contains("One or more solar modules in your array have not produced for 1"))
        assertTrue(body.contains("Module INV2: No production for 2 day(s)"))
        assertTrue(body.contains("Module INV1: No production for 1 day(s)"))
        assertTrue(body.contains("Open EMA Companion to view details."))
    }

    @Test
    fun buildBody_yellow_modulesOrderedByOfflineDaysDescending() {
        val modules = listOf(Module("INV1", 1), Module("INV2", 2))
        val body = builder.buildBody(ModuleHealthStatus.YELLOW, modules)

        val inv2Pos = body.indexOf("INV2")
        val inv1Pos = body.indexOf("INV1")
        assertTrue("INV2 (2 days) must appear before INV1 (1 day)", inv2Pos < inv1Pos)
    }

    @Test
    fun buildBody_red_containsRedIntroAndModuleList() {
        val modules = listOf(Module("INV1", 3))
        val body = builder.buildBody(ModuleHealthStatus.RED, modules)

        assertTrue(body.contains("3 or more days"))
        assertTrue(body.contains("Module INV1: No production for 3 day(s)"))
    }

    @Test
    fun buildBody_green_containsGreenBodyWithNoModuleList() {
        val body = builder.buildBody(ModuleHealthStatus.GREEN, emptyList())

        assertTrue(body.contains("All solar modules in your array are producing again."))
        assertFalse("GREEN body must not contain 'Module '", body.contains("Module "))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "de")
class EmailContentBuilderDeTest {

    private lateinit var builder: EmailContentBuilder

    @Before
    fun setUp() {
        builder = EmailContentBuilder(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun buildSubject_yellow_returnsGermanSubject() {
        assertEquals(
            "EMA Companion: Solarmodul offline",
            builder.buildSubject(ModuleHealthStatus.YELLOW),
        )
    }

    @Test
    fun buildSubject_red_returnsGermanSubject() {
        assertEquals(
            "EMA Companion: Solarmodul offline – Handlungsbedarf",
            builder.buildSubject(ModuleHealthStatus.RED),
        )
    }

    @Test
    fun buildSubject_green_returnsGermanSubject() {
        assertEquals(
            "EMA Companion: Alle Module produzieren",
            builder.buildSubject(ModuleHealthStatus.GREEN),
        )
    }

    @Test
    fun buildBody_yellow_containsGermanIntro() {
        val modules = listOf(Module("INV1", 1))
        val body = builder.buildBody(ModuleHealthStatus.YELLOW, modules)

        assertTrue(body.contains("Ein oder mehrere Solarmodule"))
        assertTrue(body.contains("Modul INV1: Keine Produktion seit 1 Tag(en)"))
    }

    @Test
    fun buildBody_green_containsGermanRecoveryText() {
        val body = builder.buildBody(ModuleHealthStatus.GREEN, emptyList())

        assertTrue(body.contains("Alle Solarmodule deiner Anlage produzieren wieder."))
    }
}
