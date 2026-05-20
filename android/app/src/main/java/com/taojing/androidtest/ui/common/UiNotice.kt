package com.taojing.androidtest.ui.common

import android.content.Context

data class UiNotice(
    val resId: Int? = null,
    val text: String? = null,
) {
    fun resolve(context: Context): String {
        return text ?: context.getString(checkNotNull(resId))
    }
}
