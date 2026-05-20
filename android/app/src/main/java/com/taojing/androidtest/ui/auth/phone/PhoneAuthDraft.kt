package com.taojing.androidtest.ui.auth.phone

data class PhoneAuthDraft(
    val mode: PhoneAuthMode = PhoneAuthMode.PASSWORD_LOGIN,
    val countryCode: String = DEFAULT_COUNTRY_CODE,
    val phone: String = "",
    val password: String = "",
    val code: String = "",
) {
    companion object {
        const val DEFAULT_COUNTRY_CODE = "86"
    }
}
