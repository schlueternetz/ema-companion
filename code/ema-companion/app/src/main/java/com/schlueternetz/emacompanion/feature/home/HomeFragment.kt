package com.schlueternetz.emacompanion.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.api.FetchError
import com.schlueternetz.emacompanion.core.api.ProductionRepository
import com.schlueternetz.emacompanion.core.api.ProductionSnapshot
import com.schlueternetz.emacompanion.core.api.ProductionSource
import com.schlueternetz.emacompanion.core.api.ProductionState
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class HomeFragment : Fragment() {

    private lateinit var productionView: TextView
    private lateinit var updatedView: TextView
    private lateinit var statusView: TextView
    private lateinit var source: ProductionSource

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
        // Render the persisted state immediately (no flash) before the fetch in onResume.
        render(source.currentState())
    }

    // Triggered on app open (Home is the start destination) and whenever Home is highlighted again.
    // The repository internally no-ops within the 10-minute throttle window.
    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            render(source.refresh())
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

    companion object {
        /** Test seam: substitutes the production source so Home can be tested without HTTP. */
        var sourceOverride: ProductionSource? = null
    }
}
