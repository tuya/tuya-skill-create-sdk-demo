package com.taojing.androidtest.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.thingclips.smart.android.user.api.IRegisterCallback
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.taojing.androidtest.R
import com.taojing.androidtest.ui.common.UiNotice
import com.taojing.androidtest.ui.common.formatTuyaError

data class AuthMethodUiState(
    val countryCode: String = DEFAULT_COUNTRY_CODE,
    val isLoading: Boolean = false,
    val notice: UiNotice? = null,
    val navigateToProfile: Boolean = false,
) {
    companion object {
        const val DEFAULT_COUNTRY_CODE = "86"
    }
}

class AuthMethodViewModel : ViewModel() {

    private val _state = MutableLiveData(AuthMethodUiState())
    val state: LiveData<AuthMethodUiState> = _state

    fun updateCountryCode(countryCode: String) {
        updateState { it.copy(countryCode = countryCode) }
    }

    fun touristLogin() {
        val currentState = currentState()
        if (currentState.countryCode.isBlank()) {
            updateState { it.copy(notice = UiNotice(resId = R.string.message_country_code_required)) }
            return
        }

        updateState { it.copy(isLoading = true, notice = null) }
        ThingHomeSdk.getUserInstance().touristRegisterAndLogin(
            currentState.countryCode,
            object : IRegisterCallback {
                override fun onSuccess(user: com.thingclips.smart.android.user.bean.User) {
                    updateState { it.copy(isLoading = false, notice = UiNotice(resId = R.string.message_tourist_login_success), navigateToProfile = true) }
                }
                override fun onError(code: String, error: String) {
                    updateState { it.copy(isLoading = false, notice = UiNotice(text = formatTuyaError(code, error))) }
                }
            },
        )
    }

    fun clearNotice() = updateState { it.copy(notice = null) }

    fun consumeProfileNavigation() = updateState { it.copy(navigateToProfile = false) }

    private fun updateState(transform: (AuthMethodUiState) -> AuthMethodUiState) {
        _state.value = transform(currentState())
    }

    private fun currentState(): AuthMethodUiState = _state.value ?: AuthMethodUiState()
}
