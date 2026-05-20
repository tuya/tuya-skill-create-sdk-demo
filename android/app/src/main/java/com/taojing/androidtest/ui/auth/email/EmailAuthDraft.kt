package com.taojing.androidtest.ui.auth.email

data class EmailAuthDraft(
    val mode: EmailAuthMode = EmailAuthMode.PASSWORD_LOGIN,
    val countryCode: String = DEFAULT_COUNTRY_CODE,
    val email: String = "",
    val password: String = "",
    val code: String = "",
) {
    companion object {
        const val DEFAULT_COUNTRY_CODE = "86"
    }
}
