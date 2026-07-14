package com.schlueternetz.emacompanion.feature.support

import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import com.schlueternetz.emacompanion.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SupportFragmentTest {
    @Test
    fun buyMeACoffeeButton_firesCorrectIntent() {
        val scenario = launchFragmentInContainer<SupportFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.support_bmac_button).performClick()

            val intent = shadowOf(fragment.requireActivity()).nextStartedActivity
            assertNotNull("intent should have been fired", intent)
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertEquals(Uri.parse("https://buymeacoffee.com/schlueternetz"), intent.data)
        }
    }

    @Test
    fun visitWebsiteButton_firesCorrectIntent() {
        val scenario = launchFragmentInContainer<SupportFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.support_website_button).performClick()

            val intent = shadowOf(fragment.requireActivity()).nextStartedActivity
            assertNotNull("intent should have been fired", intent)
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertEquals(Uri.parse("https://www.schlueternetz.com"), intent.data)
        }
    }
}
