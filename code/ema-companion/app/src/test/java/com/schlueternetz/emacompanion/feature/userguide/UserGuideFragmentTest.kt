package com.schlueternetz.emacompanion.feature.userguide

import android.text.Spannable
import android.text.style.ClickableSpan
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.schlueternetz.emacompanion.R
import io.noties.markwon.ext.tables.TableRowSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UserGuideFragmentTest {

    @Test
    fun assetsSmokeTest_userGuideMdExists() {
        val assets = ApplicationProvider.getApplicationContext<android.content.Context>().assets
        val files = assets.list("user-guide") ?: emptyArray()
        assertTrue(
            "user-guide/user-guide.md not found in assets — run ./gradlew preBuild first",
            files.contains("user-guide.md"),
        )
    }

    @Test
    fun userGuideFragment_rendersNonEmptyText() {
        val scenario = launchFragmentInContainer<UserGuideFragment>(
            fragmentArgs = bundleOf("assetPath" to "feature/userguide/index.md"),
            themeResId = R.style.Theme_EMACompanion,
        )
        scenario.onFragment { fragment ->
            val tv = fragment.requireView().findViewById<TextView>(R.id.user_guide_content)
            assertNotNull(tv)
            // Assert real fixture content, not just non-empty — the error fallback is also
            // non-empty, so a bare isNotEmpty() check would pass even if the asset failed to load.
            assertTrue(
                "TextView should render fixture content, got: '${tv.text}'",
                tv.text.contains("index"),
            )
            AccessibilityValidator()
                .setThrowExceptionFor(AccessibilityCheckResult.AccessibilityCheckResultType.ERROR)
                .check(fragment.requireView())
        }
    }

    @Test
    fun userGuideFragment_rendersTableNotRawSource() {
        val scenario = launchFragmentInContainer<UserGuideFragment>(
            fragmentArgs = bundleOf("assetPath" to "feature/userguide/index.md"),
            themeResId = R.style.Theme_EMACompanion,
        )
        scenario.onFragment { fragment ->
            val tv = fragment.requireView().findViewById<TextView>(R.id.user_guide_content)
            val rendered = tv.text.toString()
            // A rendered table consumes the markdown pipe/delimiter syntax. If the raw
            // '|' characters survive, the table was shown as source instead of rendered.
            assertFalse(
                "Table should render, not show markdown source: '$rendered'",
                rendered.contains("|"),
            )
            // Markwon stores table cell text inside TableRowSpan layouts, not in the
            // CharSequence — the presence of these spans is what proves a table rendered.
            val spannable = tv.text as Spannable
            val tableSpans = spannable.getSpans(0, spannable.length, TableRowSpan::class.java)
            assertTrue("Rendered output should contain table row spans", tableSpans.isNotEmpty())
            AccessibilityValidator()
                .setThrowExceptionFor(AccessibilityCheckResult.AccessibilityCheckResultType.ERROR)
                .check(fragment.requireView())
        }
    }

    @Test
    fun userGuideFragment_mdLinkNavigatesToLinkedPage() {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        val scenario = launchFragmentInContainer<UserGuideFragment>(
            fragmentArgs = bundleOf("assetPath" to "feature/userguide/index.md"),
            themeResId = R.style.Theme_EMACompanion,
        )
        scenario.onFragment { fragment ->
            navController.setGraph(R.navigation.nav_graph)
            navController.setCurrentDestination(R.id.userGuideFragment, bundleOf("assetPath" to "feature/userguide/index.md"))
            Navigation.setViewNavController(fragment.requireView(), navController)

            val tv = fragment.requireView().findViewById<TextView>(R.id.user_guide_content)
            val spannable = tv.text as android.text.Spannable
            val spans = spannable.getSpans(0, spannable.length, ClickableSpan::class.java)
            assertTrue("index.md should contain at least one clickable link", spans.isNotEmpty())
            spans.first().onClick(tv)
        }
        assertEquals(R.id.userGuideFragment, navController.currentDestination?.id)
        assertEquals(
            "feature/userguide/linked-page.md",
            navController.currentBackStackEntry?.arguments?.getString("assetPath"),
        )
        scenario.onFragment { fragment ->
            AccessibilityValidator()
                .setThrowExceptionFor(AccessibilityCheckResult.AccessibilityCheckResultType.ERROR)
                .check(fragment.requireView())
        }
    }

    @Test
    fun userGuideFragment_imageInFixtureLoadsWithoutThrowing() {
        val scenario = launchFragmentInContainer<UserGuideFragment>(
            fragmentArgs = bundleOf("assetPath" to "feature/userguide/index.md"),
            themeResId = R.style.Theme_EMACompanion,
        )
        // If the custom AsyncDrawableLoader throws when resolving the image reference, this test fails.
        scenario.onFragment { fragment ->
            val tv = fragment.requireView().findViewById<TextView>(R.id.user_guide_content)
            assertNotNull(tv)
            AccessibilityValidator()
                .setThrowExceptionFor(AccessibilityCheckResult.AccessibilityCheckResultType.ERROR)
                .check(fragment.requireView())
        }
    }
}
