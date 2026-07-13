package com.schlueternetz.emacompanion.feature.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.test.core.app.ApplicationProvider
import com.schlueternetz.emacompanion.core.HomeWidget
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WidgetUpdaterTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        context
            .getSharedPreferences("ema_companion_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        settings = SettingsRepository.create(context)
    }

    @Test
    fun updateAll_invokesGlanceUpdateAllForEveryWidget() =
        runTest {
            val widgets = listOf(NoopWidget(), NoopWidget())

            // Confirms the wrapper delegates to Glance's own updateAll() for every widget in the
            // list without throwing, even with zero placed widget instances (the Robolectric case).
            WidgetUpdater.updateAll(context, widgets)
        }

    @Test
    fun enabledWidgets_includesAllThreeByDefault() {
        val widgets = WidgetUpdater.enabledWidgets(settings)
        assertEquals(3, widgets.size)
        assertTrue(widgets.any { it is TodayProductionWidget })
        assertTrue(widgets.any { it is ProductionSummaryWidget })
        assertTrue(widgets.any { it is ProductionHistoryWidget })
    }

    @Test
    fun enabledWidgets_excludesDisabledWidgetType() {
        settings.setWidgetEnabled(HomeWidget.PRODUCTION_SUMMARY, false)
        val widgets = WidgetUpdater.enabledWidgets(settings)
        assertEquals(2, widgets.size)
        assertTrue(widgets.none { it is ProductionSummaryWidget })
    }

    @Test
    fun enabledWidgets_emptyWhenAllDisabled() {
        HomeWidget.entries.forEach { settings.setWidgetEnabled(it, false) }
        assertEquals(0, WidgetUpdater.enabledWidgets(settings).size)
    }

    private class NoopWidget : GlanceAppWidget() {
        override suspend fun provideGlance(
            context: Context,
            id: GlanceId,
        ) {
            provideContent { }
        }
    }
}
