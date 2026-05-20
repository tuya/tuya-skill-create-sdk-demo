package com.taojing.androidtest.ui.common

import android.widget.EditText

fun EditText.updateTextIfChanged(value: String) {
    if (text?.toString() != value) {
        setText(value)
        setSelection(value.length)
    }
}

fun formatTuyaError(code: String, error: String): String {
    return "$code: $error"
}
