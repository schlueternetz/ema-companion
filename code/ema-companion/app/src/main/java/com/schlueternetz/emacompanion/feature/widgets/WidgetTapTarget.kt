package com.schlueternetz.emacompanion.feature.widgets

import android.content.Context
import android.content.Intent
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity
import com.schlueternetz.emacompanion.MainActivity

/**
 * Decides which screen a widget's tap should open, and builds the action that opens it.
 * `MainActivity` reads [EXTRA_WIDGET_TARGET] once (see its intent-handling) then clears it so a
 * later recreation (e.g. rotation) doesn't re-trigger the navigation.
 */
object WidgetTapTarget {
    const val EXTRA_WIDGET_TARGET = "widget_target"
    const val TARGET_HOME = "home"
    const val TARGET_SETTINGS = "settings"

    /** Not configured, or the widget's own state is currently showing an error → Settings; otherwise Home. */
    fun target(
        configured: Boolean,
        hasError: Boolean,
    ): String = if (!configured || hasError) TARGET_SETTINGS else TARGET_HOME

    fun action(
        context: Context,
        target: String,
    ): Action =
        actionStartActivity(
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_WIDGET_TARGET, target)
            },
        )
}
