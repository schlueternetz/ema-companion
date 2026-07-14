package com.schlueternetz.emacompanion.feature.support

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.schlueternetz.emacompanion.R

class SupportFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_support, container, false)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.support_bmac_button).setOnClickListener {
            openUrl(getString(R.string.support_bmac_url))
        }
        view.findViewById<View>(R.id.support_website_button).setOnClickListener {
            openUrl(getString(R.string.support_website_url))
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
