package com.taojing.androidtest.ui.auth.phone

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.thingclips.smart.android.user.api.ILoginCallback
import com.thingclips.smart.android.user.api.IRegisterCallback
import com.thingclips.smart.android.user.api.IResetPasswordCallback
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.sdk.api.IResultCallback
import com.taojing.androidtest.R
import com.taojing.androidtest.ui.common.UiNotice
import com.taojing.androidtest.ui.common.formatTuyaError

data class PhoneAuthUiState(
    val draft: PhoneAuthDraft = PhoneAuthDraft(),
    val isLoading: Boolean = false,
    val notice: UiNotice? = null,
    val navigateToProfile: Boolean = false,
)

class PhoneAuthViewModel : ViewModel() {

    private val _state = MutableLiveData(PhoneAuthUiState())
    val state: LiveData<PhoneAuthUiState> = _state

    fun initializeCountryCode(countryCode: String) {
        if (countryCode.isNotBlank()) {
            updateDraft { it.copy(countryCode = countryCode) }
        }
    }

    fun updateMode(mode: PhoneAuthMode) {
        updateDraft { it.copy(mode = mode, password = "", code = "") }
    }

    fun updateCountryCode(countryCode: String) = updateDraft { it.copy(countryCode = countryCode) }
    fun updatePhone(phone: String) = updateDraft { it.copy(phone = phone) }
    fun updatePassword(password: String) = updateDraft { it.copy(password = password) }
    fun updateCode(code: String) = updateDraft { it.copy(code = code) }

    fun sendCode() {
        val draft = currentState().draft
        val verifyType = draft.mode.verifyType() ?: run {
            updateState { it.copy(notice = UiNotice(resId = R.string.message_auth_code_not_needed)) }
            return
        }

        when {
            draft.countryCode.isBlank() -> postNotice(R.string.message_country_code_required)
            draft.phone.isBlank() -> postNotice(R.string.message_phone_required)
            else -> {
                updateState { it.copy(isLoading = true, notice = null) }
                ThingHomeSdk.getUserInstance().sendVerifyCodeWithUserName(
                    draft.phone, "", draft.countryCode, verifyType,
                    object : IResultCallback {
                        override fun onSuccess() {
                            updateState { it.copy(isLoading = false, notice = UiNotice(resId = R.string.message_code_sent)) }
                        }
                        override fun onError(code: String, error: String) {
                            updateState { it.copy(isLoading = false, notice = UiNotice(text = formatTuyaError(code, error))) }
                        }
                    },
                )
            }
        }
    }

    fun submit() {
        val draft = currentState().draft
        val validation = validate(draft)
        if (validation != null) { postNotice(validation); return }
        updateState { it.copy(isLoading = true, notice = null) }
        when (draft.mode) {
            PhoneAuthMode.REGISTER -> register(draft)
            PhoneAuthMode.PASSWORD_LOGIN -> loginWithPassword(draft)
            PhoneAuthMode.CODE_LOGIN -> loginWithCode(draft)
            PhoneAuthMode.RESET_PASSWORD -> resetPassword(draft)
        }
    }

    fun clearNotice() = updateState { it.copy(notice = null) }

    fun consumeProfileNavigation() = updateState { it.copy(navigateToProfile = false) }

    private fun register(draft: PhoneAuthDraft) {
        ThingHomeSdk.getUserInstance().registerAccountWithPhone(
            draft.countryCode, draft.phone, draft.password, draft.code,
            object : IRegisterCallback {
                override fun onSuccess(user: com.thingclips.smart.android.user.bean.User) {
                    updateState { it.copy(isLoading = false, notice = UiNotice(resId = R.string.message_register_success), navigateToProfile = true) }
                }
                override fun onError(code: String, error: String) = fail(code, error)
            },
        )
    }

    private fun loginWithPassword(draft: PhoneAuthDraft) {
        ThingHomeSdk.getUserInstance().loginWithPhonePassword(
            draft.countryCode, draft.phone, draft.password,
            object : ILoginCallback {
                override fun onSuccess(user: com.thingclips.smart.android.user.bean.User) {
                    updateState { it.copy(isLoading = false, notice = UiNotice(resId = R.string.message_login_success), navigateToProfile = true) }
                }
                override fun onError(code: String, error: String) = fail(code, error)
            },
        )
    }

    private fun loginWithCode(draft: PhoneAuthDraft) {
        ThingHomeSdk.getUserInstance().loginWithPhone(
            draft.countryCode, draft.phone, draft.code,
            object : ILoginCallback {
                override fun onSuccess(user: com.thingclips.smart.android.user.bean.User) {
                    updateState { it.copy(isLoading = false, notice = UiNotice(resId = R.string.message_login_success), navigateToProfile = true) }
                }
                override fun onError(code: String, error: String) = fail(code, error)
            },
        )
    }

    private fun resetPassword(draft: PhoneAuthDraft) {
        ThingHomeSdk.getUserInstance().resetPhonePassword(
            draft.countryCode, draft.phone, draft.code, draft.password,
            object : IResetPasswordCallback {
                override fun onSuccess() {
                    updateState {
                        it.copy(
                            draft = PhoneAuthDraft(mode = PhoneAuthMode.PASSWORD_LOGIN, countryCode = draft.countryCode, phone = draft.phone),
                            isLoading = false,
                            notice = UiNotice(resId = R.string.message_password_reset_success),
                        )
                    }
                }
                override fun onError(code: String, error: String) = fail(code, error)
            },
        )
    }

    private fun validate(draft: PhoneAuthDraft): Int? {
        return when {
            draft.countryCode.isBlank() -> R.string.message_country_code_required
            draft.phone.isBlank() -> R.string.message_phone_required
            draft.mode.requiresPassword() && draft.password.isBlank() -> R.string.message_password_required
            draft.mode.requiresCode() && draft.code.isBlank() -> R.string.message_code_required
            else -> null
        }
    }

    private fun fail(code: String, error: String) {
        updateState { it.copy(isLoading = false, notice = UiNotice(text = formatTuyaError(code, error))) }
    }

    private fun postNotice(resId: Int) = updateState { it.copy(notice = UiNotice(resId = resId)) }

    private fun updateDraft(transform: (PhoneAuthDraft) -> PhoneAuthDraft) {
        updateState { state -> state.copy(draft = transform(state.draft)) }
    }

    private fun updateState(transform: (PhoneAuthUiState) -> PhoneAuthUiState) {
        _state.value = transform(currentState())
    }

    private fun currentState(): PhoneAuthUiState = _state.value ?: PhoneAuthUiState()

    private fun PhoneAuthMode.requiresPassword() =
        this == PhoneAuthMode.REGISTER || this == PhoneAuthMode.PASSWORD_LOGIN || this == PhoneAuthMode.RESET_PASSWORD

    private fun PhoneAuthMode.requiresCode() =
        this == PhoneAuthMode.REGISTER || this == PhoneAuthMode.CODE_LOGIN || this == PhoneAuthMode.RESET_PASSWORD

    private fun PhoneAuthMode.verifyType(): Int? = when (this) {
        PhoneAuthMode.REGISTER -> 1
        PhoneAuthMode.CODE_LOGIN -> 2
        PhoneAuthMode.RESET_PASSWORD -> 3
        PhoneAuthMode.PASSWORD_LOGIN -> null
    }
}
