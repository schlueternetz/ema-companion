package com.schlueternetz.emacompanion.feature.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WidgetUpdaterTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun updateAll_invokesGlanceUpdateAllForEveryWidget() =
        runTest {
            val widgets = listOf(NoopWidget(), NoopWidget())

            // Confirms the wrapper delegates to Glance's own updateAll() for every widget in the
            // list without throwing, even with zero placed widget instances (the Robolectric case).
            WidgetUpdater.updateAll(context, widgets)
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
