package com.taojing.androidtest.ui.profile

data class ConvertAccountDraft(
    val mode: ConvertAccountMode = ConvertAccountMode.PHONE,
    val countryCode: String = DEFAULT_COUNTRY_CODE,
    val userName: String = "",
    val code: String = "",
    val password: String = "",
) {
    companion object {
        const val DEFAULT_COUNTRY_CODE = "86"
    }
}
