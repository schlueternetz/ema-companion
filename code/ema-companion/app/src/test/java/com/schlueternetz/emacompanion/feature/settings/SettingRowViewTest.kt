package com.schlueternetz.emacompanion.feature.settings

import android.content.Context
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.schlueternetz.emacompanion.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingRowViewTest {

    private lateinit var context: Context
    private lateinit var view: SettingRowView

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.setTheme(R.style.Theme_EMACompanion)
        view = SettingRowView(context)
        view.label = "Test Label"
        view.value = "test value"
        view.errorMessage = "Invalid input"
        view.validator = { it.isNotEmpty() }
    }

    @Test
    fun readOnlyMode_showsLabelAndValue() {
        val labelView = view.findViewById<TextView>(R.id.setting_label)
        val valueView = view.findViewById<TextView>(R.id.setting_value)
        assertEquals("Test Label", labelView.text.toString())
        assertEquals("test value", valueView.text.toString())
    }

    @Test
    fun editButton_click_showsInputAndHidesValue() {
        view.findViewById<ImageButton>(R.id.setting_edit_button).performClick()

        val valueView = view.findViewById<TextView>(R.id.setting_value)
        val inputLayout = view.findViewById<View>(R.id.setting_input_layout)
        assertEquals(View.GONE, valueView.visibility)
        assertEquals(View.VISIBLE, inputLayout.visibility)
    }

    @Test
    fun saveButton_withValidInput_callsOnSave() {
        var saved = ""
        view.onSave = { saved = it }

        view.findViewById<ImageButton>(R.id.setting_edit_button).performClick()
        view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.setting_edit_text)
            .setText("valid")
        view.findViewById<ImageButton>(R.id.setting_save_button).performClick()

        assertEquals("valid", saved)
    }

    @Test
    fun saveButton_withInvalidInput_showsError() {
        view.validator = { false }

        view.findViewById<ImageButton>(R.id.setting_edit_button).performClick()
        view.findViewById<ImageButton>(R.id.setting_save_button).performClick()

        val errorView = view.findViewById<TextView>(R.id.setting_error)
        assertEquals(View.VISIBLE, errorView.visibility)
        assertEquals("Invalid input", errorView.text.toString())
    }

    @Test
    fun cancelButton_restoresPreviousValue() {
        view.value = "original"
        view.findViewById<ImageButton>(R.id.setting_edit_button).performClick()
        view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.setting_edit_text)
            .setText("changed")
        view.findViewById<ImageButton>(R.id.setting_cancel_button).performClick()

        val valueView = view.findViewById<TextView>(R.id.setting_value)
        assertEquals("original", valueView.text.toString())
    }

    @Test
    fun masking_displaysOnlyLastFourChars() {
        view.isMasked = true
        view.value = "mysecretkey"

        val valueView = view.findViewById<TextView>(R.id.setting_value)
        val displayed = valueView.text.toString()
        assertTrue(displayed.endsWith("tkey"))
        assertTrue(displayed.startsWith("•••••••"))
    }

    @Test
    fun clearOnEdit_whenMasked_clearsTextField() {
        view.isMasked = true
        view.value = "mysecret"
        view.findViewById<ImageButton>(R.id.setting_edit_button).performClick()

        val editText = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.setting_edit_text)
        assertEquals("", editText.text.toString())
    }

    @Test
    fun isRequired_emptyValue_showsRequiredHint() {
        view.isRequired = true
        view.value = ""

        val valueView = view.findViewById<TextView>(R.id.setting_value)
        assertEquals("Required", valueView.hint?.toString())
    }

    @Test
    fun isRequired_nonEmptyValue_hintIsEmpty() {
        view.isRequired = true
        view.value = "some value"

        val valueView = view.findViewById<TextView>(R.id.setting_value)
        assertTrue(valueView.hint.isNullOrEmpty())
    }

    @Test
    fun notRequired_emptyValue_noHint() {
        view.isRequired = false
        view.value = ""

        val valueView = view.findViewById<TextView>(R.id.setting_value)
        assertTrue(valueView.hint.isNullOrEmpty())
    }

    @Test
    fun isRequired_valueSetAfterRequired_hintClears() {
        view.isRequired = true
        view.value = ""
        view.value = "saved value"

        val valueView = view.findViewById<TextView>(R.id.setting_value)
        assertTrue(valueView.hint.isNullOrEmpty())
    }

    @Test
    fun suffix_strippedFromEditTextWhenEditing() {
        view.suffix = " kW"
        view.value = "9.72 kW"

        view.findViewById<ImageButton>(R.id.setting_edit_button).performClick()

        val editText = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.setting_edit_text)
        assertEquals("9.72", editText.text.toString())
    }

    @Test
    fun suffix_shownOnInputLayoutWhenEditing() {
        view.suffix = " kW"
        view.value = "9.72 kW"

        view.findViewById<ImageButton>(R.id.setting_edit_button).performClick()

        val inputLayout = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.setting_input_layout)
        assertEquals(" kW", inputLayout.suffixText)
    }

    @Test
    fun hasNoAccessibilityErrors() {
        // Measure and layout so ATF can inspect dimensions
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        AccessibilityValidator()
            .setThrowExceptionFor(AccessibilityCheckResult.AccessibilityCheckResultType.ERROR)
            .check(view)
    }
}
