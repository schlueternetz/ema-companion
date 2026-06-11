package com.schlueternetz.emacompanion.feature.settings

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.schlueternetz.emacompanion.R

class SettingRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val labelView: TextView
    private val valueView: TextView
    private val editButton: ImageButton
    private val saveButton: ImageButton
    private val cancelButton: ImageButton
    private val inputLayout: TextInputLayout
    private val editText: TextInputEditText
    private val errorView: TextView

    var label: String = ""
        set(value) {
            field = value
            labelView.text = value
        }

    var value: String = ""
        set(value) {
            field = value
            valueView.text = maskedDisplay(value)
            updateRequiredHint()
        }

    var isMasked: Boolean = false
        set(value) {
            field = value
            valueView.text = maskedDisplay(this.value)
        }

    var errorMessage: String = ""

    var validator: (String) -> Boolean = { true }

    var onSave: (String) -> Unit = {}

    var isRequired: Boolean = false
        set(value) {
            field = value
            updateRequiredHint()
        }

    var suffix: String = ""
        set(value) {
            field = value
            inputLayout.suffixText = value
        }

    var keyboardType: Int = InputType.TYPE_CLASS_TEXT
        set(value) {
            field = value
            editText.inputType = value
        }

    private var savedDisplayValue: String = ""

    init {
        LayoutInflater.from(context).inflate(R.layout.view_setting_row, this, true)
        labelView = findViewById(R.id.setting_label)
        valueView = findViewById(R.id.setting_value)
        editButton = findViewById(R.id.setting_edit_button)
        saveButton = findViewById(R.id.setting_save_button)
        cancelButton = findViewById(R.id.setting_cancel_button)
        inputLayout = findViewById(R.id.setting_input_layout)
        editText = findViewById(R.id.setting_edit_text)
        errorView = findViewById(R.id.setting_error)

        editButton.setOnClickListener { enterEditMode() }
        saveButton.setOnClickListener { attemptSave() }
        cancelButton.setOnClickListener { cancelEdit() }
    }

    private fun enterEditMode() {
        savedDisplayValue = value
        if (isMasked) editText.setText("") else editText.setText(value.removeSuffix(suffix))
        editText.setSelection(editText.text?.length ?: 0)
        valueView.visibility = View.GONE
        editButton.visibility = View.GONE
        inputLayout.visibility = View.VISIBLE
        saveButton.visibility = View.VISIBLE
        cancelButton.visibility = View.VISIBLE
        errorView.visibility = View.GONE
    }

    private fun attemptSave() {
        val input = editText.text?.toString() ?: ""
        if (!validator(input)) {
            errorView.text = errorMessage
            errorView.visibility = View.VISIBLE
            return
        }
        errorView.visibility = View.GONE
        value = input
        onSave(input)
        exitEditMode()
    }

    private fun cancelEdit() {
        value = savedDisplayValue
        exitEditMode()
    }

    private fun exitEditMode() {
        inputLayout.visibility = View.GONE
        saveButton.visibility = View.GONE
        cancelButton.visibility = View.GONE
        valueView.visibility = View.VISIBLE
        editButton.visibility = View.VISIBLE
        errorView.visibility = View.GONE
    }

    private fun updateRequiredHint() {
        valueView.hint = if (isRequired && value.isEmpty()) {
            context.getString(R.string.setting_row_required_hint)
        } else {
            null
        }
    }

    private fun maskedDisplay(raw: String): String {
        if (!isMasked || raw.isEmpty()) return raw
        val visibleCount = minOf(4, raw.length)
        val masked = "•".repeat(raw.length - visibleCount)
        return masked + raw.takeLast(visibleCount)
    }
}
