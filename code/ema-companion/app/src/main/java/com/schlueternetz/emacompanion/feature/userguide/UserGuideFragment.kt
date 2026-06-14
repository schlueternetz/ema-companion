package com.schlueternetz.emacompanion.feature.userguide

import android.content.Intent
import android.content.res.AssetManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.ConfigurationCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.schlueternetz.emacompanion.R
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.file.FileSchemeHandler

class UserGuideFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_user_guide, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val assets = requireContext().assets
        val requestedPath = arguments?.getString("assetPath") ?: "user-guide/user-guide.md"
        val language = ConfigurationCompat.getLocales(resources.configuration)[0]?.language ?: "en"
        val assetPath = localizeAssetPath(requestedPath, language, assets)
        val folder = assetPath.substringBeforeLast('/', missingDelimiterValue = "")

        val markdown = try {
            assets.open(assetPath).bufferedReader().readText()
        } catch (e: Exception) {
            "# Error\n\nCould not load `$assetPath`."
        }

        // Rewrite relative image paths to file:///android_asset/folder/name so
        // FileSchemeHandler can load them; absolute URLs (http/https/file) are left as-is.
        val processed = rewriteRelativeImages(markdown, folder)

        val markwon = Markwon.builder(requireContext())
            .usePlugin(CorePlugin.create())
            .usePlugin(TablePlugin.create(requireContext()))
            .usePlugin(ImagesPlugin.create { plugin ->
                plugin.addSchemeHandler(FileSchemeHandler.createWithAssets(assets))
            })
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                    builder.linkResolver { _, link ->
                        when {
                            link.endsWith(".md") -> {
                                val resolved = if (folder.isEmpty()) link else "$folder/$link"
                                val args = Bundle().apply { putString("assetPath", resolved) }
                                findNavController().navigate(R.id.userGuideFragment, args)
                            }
                            link.startsWith("http://") || link.startsWith("https://") -> {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                            }
                        }
                    }
                }
            })
            .build()

        val textView = view.findViewById<TextView>(R.id.user_guide_content)
        markwon.setMarkdown(textView, processed)
    }

    // When the locale is German and a "<name>-de.md" sibling exists, load it; otherwise
    // keep the requested English file. Link resolution is unchanged — only the file read
    // at load time is localized, with graceful English fallback.
    private fun localizeAssetPath(path: String, language: String, assets: AssetManager): String {
        if (language != "de" || !path.endsWith(".md")) return path
        val localized = path.removeSuffix(".md") + "-de.md"
        val dir = localized.substringBeforeLast('/', missingDelimiterValue = "")
        val name = localized.substringAfterLast('/')
        val exists = (assets.list(dir) ?: emptyArray()).contains(name)
        return if (exists) localized else path
    }

    private fun rewriteRelativeImages(markdown: String, folder: String): String =
        markdown.replace(
            Regex("!\\[([^]]*)]\\((?![a-zA-Z][a-zA-Z0-9+\\-.]*://)([^)]+)\\)"),
        ) { match ->
            val alt = match.groupValues[1]
            val src = match.groupValues[2]
            val resolved = if (folder.isEmpty()) src else "$folder/$src"
            "![$alt](file:///android_asset/$resolved)"
        }
}
