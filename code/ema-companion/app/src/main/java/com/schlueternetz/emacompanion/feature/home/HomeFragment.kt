package com.schlueternetz.emacompanion.feature.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.api.FetchError
import com.schlueternetz.emacompanion.core.api.ProductionRepository
import com.schlueternetz.emacompanion.core.api.ProductionSnapshot
import com.schlueternetz.emacompanion.core.api.ProductionSource
import com.schlueternetz.emacompanion.core.api.ProductionState
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthRepository
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthSource
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthState
import com.schlueternetz.emacompanion.core.api.modulehealth.ModuleHealthStatus
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class HomeFragment : Fragment() {

    private lateinit var productionView: TextView
    private lateinit var updatedView: TextView
    private lateinit var statusView: TextView
    private lateinit var source: ProductionSource

    private lateinit var moduleHealthTile: MaterialCardView
    private lateinit var moduleHealthIcon: ImageView
    private lateinit var moduleHealthStatusView: TextView
    private lateinit var moduleHealthCheckedView: TextView
    private lateinit var moduleHealthErrorView: TextView
    private lateinit var moduleHealthSource: ModuleHealthSource

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        productionView = view.findViewById(R.id.text_current_production)
        updatedView = view.findViewById(R.id.production_updated)
        statusView = view.findViewById(R.id.production_status)
        source = sourceOverride ?: ProductionRepository.create(requireContext())

        moduleHealthTile = view.findViewById(R.id.tile_module_health)
        moduleHealthIcon = view.findViewById(R.id.module_health_icon)
        moduleHealthStatusView = view.findViewById(R.id.module_health_status)
        moduleHealthCheckedView = view.findViewById(R.id.module_health_checked)
        moduleHealthErrorView = view.findViewById(R.id.module_health_error)
        moduleHealthSource = moduleHealthSourceOverride ?: ModuleHealthRepository.create(requireContext())

        // Render persisted states immediately (no flash) before the fetch in onResume.
        render(source.currentState())
        renderModuleHealth(moduleHealthSource.currentState())
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            render(source.refresh())
        }
        viewLifecycleOwner.lifecycleScope.launch {
            renderModuleHealth(moduleHealthSource.refresh())
        }
    }

    private fun render(state: ProductionState) {
        val value = state.snapshot?.powerWatts?.toString() ?: getString(R.string.home_production_neutral)
        productionView.text = getString(R.string.home_production_value, value, ProductionSnapshot.UNIT)

        val updatedAt = state.updatedAtEpochMs
        if (updatedAt == null) {
            updatedView.visibility = View.GONE
        } else {
            val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(updatedAt))
            updatedView.text = getString(R.string.home_production_updated, time)
            updatedView.visibility = View.VISIBLE
        }

        val statusText = when (state.error) {
            FetchError.NETWORK -> getString(R.string.home_status_network_error)
            FetchError.AUTH -> getString(R.string.home_status_auth_error)
            FetchError.API -> getString(R.string.home_status_api_error)
            null -> null
        }
        if (statusText == null) {
            statusView.visibility = View.GONE
        } else {
            statusView.text = statusText
            statusView.contentDescription = statusText
            statusView.visibility = View.VISIBLE
        }
    }

    private fun renderModuleHealth(state: ModuleHealthState) {
        // On fetch error: question-mark icon + empty label. The error line below provides detail.
        if (state.error != null) {
            moduleHealthStatusView.text = ""
            moduleHealthStatusView.visibility = View.VISIBLE
            moduleHealthIcon.setImageResource(R.drawable.ic_help_circle)
            moduleHealthIcon.setColorFilter(Color.parseColor("#9E9E9E"))
            moduleHealthIcon.contentDescription = getString(R.string.home_module_health_status_unknown)
        } else {
            val statusText = when (state.status) {
                ModuleHealthStatus.GREEN -> getString(R.string.home_module_health_status_green)
                ModuleHealthStatus.YELLOW -> getString(R.string.home_module_health_status_yellow)
                ModuleHealthStatus.RED -> getString(R.string.home_module_health_status_red)
                ModuleHealthStatus.UNKNOWN -> getString(R.string.home_module_health_status_unknown)
            }
            moduleHealthStatusView.text = statusText
            moduleHealthStatusView.visibility = View.VISIBLE
            val iconRes = when (state.status) {
                ModuleHealthStatus.GREEN, ModuleHealthStatus.UNKNOWN -> R.drawable.ic_check_circle
                ModuleHealthStatus.YELLOW, ModuleHealthStatus.RED -> R.drawable.ic_warning
            }
            moduleHealthIcon.setImageResource(iconRes)
            val tintColor = when (state.status) {
                ModuleHealthStatus.GREEN -> Color.parseColor("#4CAF50")
                ModuleHealthStatus.YELLOW -> Color.parseColor("#FFC107")
                ModuleHealthStatus.RED -> Color.parseColor("#F44336")
                ModuleHealthStatus.UNKNOWN -> Color.parseColor("#9E9E9E")
            }
            moduleHealthIcon.setColorFilter(tintColor)
        }

        val checkedAt = state.checkedAtEpochMs
        if (checkedAt == null) {
            moduleHealthCheckedView.visibility = View.GONE
        } else {
            val date = DateFormat.getDateInstance(DateFormat.SHORT).format(Date(checkedAt))
            val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(checkedAt))
            moduleHealthCheckedView.text = getString(R.string.home_module_health_checked, date, time)
            moduleHealthCheckedView.visibility = View.VISIBLE
        }

        val errorText = when (state.error) {
            FetchError.NETWORK -> getString(R.string.home_module_health_error_network)
            FetchError.AUTH -> getString(R.string.home_module_health_error_auth)
            FetchError.API -> getString(R.string.home_module_health_error_api)
            null -> null
        }
        if (errorText == null) {
            moduleHealthErrorView.visibility = View.GONE
        } else {
            moduleHealthErrorView.text = errorText
            moduleHealthErrorView.contentDescription = errorText
            moduleHealthErrorView.visibility = View.VISIBLE
        }

        // GREEN/UNKNOWN: no tap interaction. YELLOW/RED: show detail modal.
        if (state.status == ModuleHealthStatus.GREEN || state.status == ModuleHealthStatus.UNKNOWN) {
            moduleHealthTile.isClickable = false
            moduleHealthTile.isFocusable = false
        } else {
            moduleHealthTile.isClickable = true
            moduleHealthTile.isFocusable = true
            moduleHealthTile.setOnClickListener { showModuleHealthDetail(state) }
        }
    }

    private fun showModuleHealthDetail(state: ModuleHealthState) {
        val message = buildString {
            state.offlineModules.forEach { module ->
                val line = if (module.offlineDays == 1) {
                    getString(R.string.home_module_health_offline_singular)
                } else {
                    getString(R.string.home_module_health_offline_plural, module.offlineDays)
                }
                appendLine("${module.uid}: $line")
            }
        }.trimEnd()
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.home_module_health_detail_title)
            .setMessage(message.ifEmpty { getString(R.string.home_module_health_status_unknown) })
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    companion object {
        /** Test seam: substitutes the production source so Home can be tested without HTTP. */
        var sourceOverride: ProductionSource? = null

        /** Test seam: substitutes the module health source so the tile can be tested without HTTP. */
        var moduleHealthSourceOverride: ModuleHealthSource? = null
    }
}
