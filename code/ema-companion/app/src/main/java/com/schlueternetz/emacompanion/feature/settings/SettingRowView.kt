package com.schlueternetz.emacompanion.feature.settings

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.Masking

class SettingRowView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : FrameLayout(context, attrs, defStyleAttr) {
        private val labelView: TextView
        private val valueView: TextView
        private val infoButton: ImageButton
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
                valueView.text = buildDisplayText(value)
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

        var onEditStateChanged: ((Boolean) -> Unit)? = null

        var isRequired: Boolean = false
            set(value) {
                field = value
                updateRequiredHint()
            }

        var suffix: String = ""
            set(value) {
                field = value
                inputLayout.suffixText = if (value.isNotEmpty()) " $value" else ""
                valueView.text = buildDisplayText(this.value)
            }

        var keyboardType: Int = InputType.TYPE_CLASS_TEXT
            set(value) {
                field = value
                editText.inputType = value
            }

        var hintText: String? = null
            set(value) {
                field = value
                infoButton.visibility = if (value != null) View.VISIBLE else View.GONE
                infoButton.setOnClickListener {
                    val hint = field
                    if (hint != null) {
                        AlertDialog
                            .Builder(context)
                            .setTitle(label)
                            .setMessage(hint)
                            .setPositiveButton(android.R.string.ok) { d, _ -> d.dismiss() }
                            .show()
                    }
                }
            }

        private var savedDisplayValue: String = ""

        init {
            LayoutInflater.from(context).inflate(R.layout.view_setting_row, this, true)
            labelView = findViewById(R.id.setting_label)
            valueView = findViewById(R.id.setting_value)
            infoButton = findViewById(R.id.setting_info_button)
            editButton = findViewById(R.id.setting_edit_button)
            saveButton = findViewById(R.id.setting_save_button)
            cancelButton = findViewById(R.id.setting_cancel_button)
            inputLayout = findViewById(R.id.setting_input_layout)
            editText = findViewById(R.id.setting_edit_text)
            errorView = findViewById(R.id.setting_error)

            editButton.setOnClickListener { enterEditMode() }
            saveButton.setOnClickListener { attemptSave() }
            cancelButton.setOnClickListener { cancelEdit() }
            editText.setOnEditorActionListener { _, _, _ ->
                attemptSave()
                true
            }
        }

        private fun enterEditMode() {
            savedDisplayValue = value
            editText.setText(if (isMasked) "" else value)
            editText.setSelection(editText.text?.length ?: 0)
            valueView.visibility = View.GONE
            infoButton.visibility = View.GONE
            editButton.visibility = View.GONE
            inputLayout.visibility = View.VISIBLE
            saveButton.visibility = View.VISIBLE
            cancelButton.visibility = View.VISIBLE
            errorView.visibility = View.GONE
            onEditStateChanged?.invoke(true)
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

        fun cancelEdit() {
            value = savedDisplayValue
            exitEditMode()
        }

        private fun exitEditMode() {
            inputLayout.visibility = View.GONE
            saveButton.visibility = View.GONE
            cancelButton.visibility = View.GONE
            valueView.visibility = View.VISIBLE
            infoButton.visibility = if (hintText != null) View.VISIBLE else View.GONE
            editButton.visibility = View.VISIBLE
            errorView.visibility = View.GONE
            onEditStateChanged?.invoke(false)
        }

        private fun updateRequiredHint() {
            valueView.hint =
                if (isRequired && value.isEmpty()) {
                    context.getString(R.string.setting_row_required_hint)
                } else {
                    null
                }
        }

        private fun buildDisplayText(raw: String): String {
            val displayed = maskedDisplay(raw)
            return if (suffix.isNotEmpty() && displayed.isNotEmpty()) "$displayed $suffix" else displayed
        }

        private fun maskedDisplay(raw: String): String {
            if (!isMasked || raw.isEmpty()) return raw
            return Masking.mask(raw)
        }
    }
