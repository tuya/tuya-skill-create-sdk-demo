package com.taojing.androidtest.ui.auth.email

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

data class EmailAuthUiState(
    val draft: EmailAuthDraft = EmailAuthDraft(),
    val isLoading: Boolean = false,
    val notice: UiNotice? = null,
    val navigateToProfile: Boolean = false,
)

class EmailAuthViewModel : ViewModel() {

    private val _state = MutableLiveData(EmailAuthUiState())
    val state: LiveData<EmailAuthUiState> = _state

    fun initializeCountryCode(countryCode: String) {
        if (countryCode.isNotBlank()) {
            updateDraft { it.copy(countryCode = countryCode) }
        }
    }

    fun updateMode(mode: EmailAuthMode) {
        updateDraft { it.copy(mode = mode, password = "", code = "") }
    }

    fun updateCountryCode(countryCode: String) = updateDraft { it.copy(countryCode = countryCode) }
    fun updateEmail(email: String) = updateDraft { it.copy(email = email) }
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
            draft.email.isBlank() -> postNotice(R.string.message_email_required)
            else -> {
                updateState { it.copy(isLoading = true, notice = null) }
                ThingHomeSdk.getUserInstance().sendVerifyCodeWithUserName(
                    draft.email, "", draft.countryCode, verifyType,
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
            EmailAuthMode.REGISTER -> register(draft)
            EmailAuthMode.PASSWORD_LOGIN -> loginWithPassword(draft)
            EmailAuthMode.CODE_LOGIN -> loginWithCode(draft)
            EmailAuthMode.RESET_PASSWORD -> resetPassword(draft)
        }
    }

    fun clearNotice() = updateState { it.copy(notice = null) }

    fun consumeProfileNavigation() = updateState { it.copy(navigateToProfile = false) }

    private fun register(draft: EmailAuthDraft) {
        ThingHomeSdk.getUserInstance().registerAccountWithEmail(
            draft.countryCode, draft.email, draft.password, draft.code,
            object : IRegisterCallback {
                override fun onSuccess(user: com.thingclips.smart.android.user.bean.User) {
                    updateState { it.copy(isLoading = false, notice = UiNotice(resId = R.string.message_register_success), navigateToProfile = true) }
                }
                override fun onError(code: String, error: String) = fail(code, error)
            },
        )
    }

    private fun loginWithPassword(draft: EmailAuthDraft) {
        ThingHomeSdk.getUserInstance().loginWithEmail(
            draft.countryCode, draft.email, draft.password,
            object : ILoginCallback {
                override fun onSuccess(user: com.thingclips.smart.android.user.bean.User) {
                    updateState { it.copy(isLoading = false, notice = UiNotice(resId = R.string.message_login_success), navigateToProfile = true) }
                }
                override fun onError(code: String, error: String) = fail(code, error)
            },
        )
    }

    private fun loginWithCode(draft: EmailAuthDraft) {
        ThingHomeSdk.getUserInstance().loginWithEmailCode(
            draft.countryCode, draft.email, draft.code,
            object : ILoginCallback {
                override fun onSuccess(user: com.thingclips.smart.android.user.bean.User) {
                    updateState { it.copy(isLoading = false, notice = UiNotice(resId = R.string.message_login_success), navigateToProfile = true) }
                }
                override fun onError(code: String, error: String) = fail(code, error)
            },
        )
    }

    private fun resetPassword(draft: EmailAuthDraft) {
        ThingHomeSdk.getUserInstance().resetEmailPassword(
            draft.countryCode, draft.email, draft.code, draft.password,
            object : IResetPasswordCallback {
                override fun onSuccess() {
                    updateState {
                        it.copy(
                            draft = EmailAuthDraft(mode = EmailAuthMode.PASSWORD_LOGIN, countryCode = draft.countryCode, email = draft.email),
                            isLoading = false,
                            notice = UiNotice(resId = R.string.message_password_reset_success),
                        )
                    }
                }
                override fun onError(code: String, error: String) = fail(code, error)
            },
        )
    }

    private fun validate(draft: EmailAuthDraft): Int? {
        return when {
            draft.countryCode.isBlank() -> R.string.message_country_code_required
            draft.email.isBlank() -> R.string.message_email_required
            draft.mode.requiresPassword() && draft.password.isBlank() -> R.string.message_password_required
            draft.mode.requiresCode() && draft.code.isBlank() -> R.string.message_code_required
            else -> null
        }
    }

    private fun fail(code: String, error: String) {
        updateState { it.copy(isLoading = false, notice = UiNotice(text = formatTuyaError(code, error))) }
    }

    private fun postNotice(resId: Int) = updateState { it.copy(notice = UiNotice(resId = resId)) }

    private fun updateDraft(transform: (EmailAuthDraft) -> EmailAuthDraft) {
        updateState { state -> state.copy(draft = transform(state.draft)) }
    }

    private fun updateState(transform: (EmailAuthUiState) -> EmailAuthUiState) {
        _state.value = transform(currentState())
    }

    private fun currentState(): EmailAuthUiState = _state.value ?: EmailAuthUiState()

    private fun EmailAuthMode.requiresPassword() =
        this == EmailAuthMode.REGISTER || this == EmailAuthMode.PASSWORD_LOGIN || this == EmailAuthMode.RESET_PASSWORD

    private fun EmailAuthMode.requiresCode() =
        this == EmailAuthMode.REGISTER || this == EmailAuthMode.CODE_LOGIN || this == EmailAuthMode.RESET_PASSWORD

    private fun EmailAuthMode.verifyType(): Int? = when (this) {
        EmailAuthMode.REGISTER -> 1
        EmailAuthMode.CODE_LOGIN -> 2
        EmailAuthMode.RESET_PASSWORD -> 3
        EmailAuthMode.PASSWORD_LOGIN -> null
    }
}
