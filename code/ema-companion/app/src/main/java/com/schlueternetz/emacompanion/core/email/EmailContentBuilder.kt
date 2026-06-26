package com.schlueternetz.emacompanion.core.email

import android.content.Context
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.api.modulehealth.Module
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthStatus

class EmailContentBuilder(private val context: Context) {

    fun buildSubject(status: ModuleHealthStatus): String = when (status) {
        ModuleHealthStatus.YELLOW -> context.getString(R.string.email_subject_yellow)
        ModuleHealthStatus.RED -> context.getString(R.string.email_subject_red)
        ModuleHealthStatus.GREEN -> context.getString(R.string.email_subject_green)
        ModuleHealthStatus.UNKNOWN -> ""
    }

    fun buildBody(status: ModuleHealthStatus, offlineModules: List<Module>): String {
        return when (status) {
            ModuleHealthStatus.YELLOW, ModuleHealthStatus.RED -> {
                val intro = if (status == ModuleHealthStatus.RED) {
                    context.getString(R.string.email_body_red_intro)
                } else {
                    context.getString(R.string.email_body_yellow_intro)
                }
                val moduleLines = offlineModules
                    .sortedByDescending { it.offlineDays }
                    .joinToString("\n") { module ->
                        context.getString(R.string.email_body_module_line, module.uid, module.offlineDays)
                    }
                val cta = context.getString(R.string.email_body_cta)
                "$intro\n\n$moduleLines\n\n$cta"
            }
            ModuleHealthStatus.GREEN -> context.getString(R.string.email_body_green)
            ModuleHealthStatus.UNKNOWN -> ""
        }
    }
}
