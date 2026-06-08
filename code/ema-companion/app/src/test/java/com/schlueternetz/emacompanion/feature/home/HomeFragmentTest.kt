package com.schlueternetz.emacompanion.feature.home

import androidx.fragment.app.testing.launchFragmentInContainer
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.schlueternetz.emacompanion.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HomeFragmentTest {

    @Test
    fun homeFragment_launchesSuccessfully() {
        launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
    }

    @Test
    fun homeFragment_hasNoAccessibilityErrors() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EMACompanion)
        scenario.onFragment { fragment ->
            AccessibilityValidator()
                .setThrowExceptionFor(AccessibilityCheckResult.AccessibilityCheckResultType.ERROR)
                .check(fragment.requireView())
        }
    }
}
