package com.schlueternetz.emacompanion.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.api.ProductionRepository
import com.schlueternetz.emacompanion.core.api.ProductionSnapshot
import com.schlueternetz.emacompanion.core.api.ProductionSource
import com.schlueternetz.emacompanion.core.api.ProductionState
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var productionView: TextView
    private lateinit var banner: View
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
        banner = view.findViewById(R.id.network_error_banner)
        source = sourceOverride ?: ProductionRepository.create(requireContext())
        render(ProductionState())
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
        productionView.text = getString(R.string.home_current_production, value, ProductionSnapshot.UNIT)
        banner.visibility = if (state.networkError) View.VISIBLE else View.GONE
    }

    companion object {
        /** Test seam: substitutes the production source so Home can be tested without HTTP. */
        var sourceOverride: ProductionSource? = null
    }
}
