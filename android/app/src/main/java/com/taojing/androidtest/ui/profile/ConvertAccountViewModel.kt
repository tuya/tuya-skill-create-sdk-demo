package com.taojing.androidtest.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.thingclips.smart.android.user.api.IBooleanCallback
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.sdk.api.IResultCallback
import com.taojing.androidtest.R
import com.taojing.androidtest.ui.common.UiNotice
import com.taojing.androidtest.ui.common.formatTuyaError

data class ConvertAccountUiState(
    val draft: ConvertAccountDraft = ConvertAccountDraft(),
    val isLoading: Boolean = false,
    val notice: UiNotice? = null,
    val navigateBack: Boolean = false,
)

class ConvertAccountViewModel : ViewModel() {

    private val _state = MutableLiveData(ConvertAccountUiState())
    val state: LiveData<ConvertAccountUiState> = _state

    fun updateMode(mode: ConvertAccountMode) = updateDraft { it.copy(mode = mode) }
    fun updateCountryCode(countryCode: String) = updateDraft { it.copy(countryCode = countryCode) }
    fun updateUserName(userName: String) = updateDraft { it.copy(userName = userName) }
    fun updateCode(code: String) = updateDraft { it.copy(code = code) }
    fun updatePassword(password: String) = updateDraft { it.copy(password = password) }

    fun sendCode() {
        val draft = currentState().draft
        when {
            draft.countryCode.isBlank() -> postNotice(R.string.message_country_code_required)
            draft.userName.isBlank() -> postNotice(
                if (draft.mode == ConvertAccountMode.PHONE) R.string.message_phone_required else R.string.message_email_required
            )
            else -> {
                updateState { it.copy(isLoading = true, notice = null) }
                ThingHomeSdk.getUserInstance().sendVerifyCodeWithUserName(
                    draft.userName, "", draft.countryCode, 1,
                    object : IResultCallback {
                        override fun onSuccess() {
                            updateState { it.copy(isLoading = false, notice = UiNotice(resId = R.string.message_code_sent)) }
                        }
                        override fun onError(code: String, error: String) = fail(code, error)
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
        ThingHomeSdk.getUserInstance().touristBindWithUserName(
            draft.countryCode, draft.userName, draft.code, draft.password,
            object : IBooleanCallback {
                override fun onSuccess() {
                    updateState { it.copy(isLoading = false, notice = UiNotice(resId = R.string.message_convert_account_success), navigateBack = true) }
                }
                override fun onError(code: String, error: String) = fail(code, error)
            },
        )
    }

    fun clearNotice() = updateState { it.copy(notice = null) }

    fun consumeNavigateBack() = updateState { it.copy(navigateBack = false) }

    private fun validate(draft: ConvertAccountDraft): Int? {
        return when {
            draft.countryCode.isBlank() -> R.string.message_country_code_required
            draft.userName.isBlank() -> if (draft.mode == ConvertAccountMode.PHONE) R.string.message_phone_required else R.string.message_email_required
            draft.code.isBlank() -> R.string.message_code_required
            draft.password.isBlank() -> R.string.message_password_required
            else -> null
        }
    }

    private fun fail(code: String, error: String) {
        updateState { it.copy(isLoading = false, notice = UiNotice(text = formatTuyaError(code, error))) }
    }

    private fun postNotice(resId: Int) = updateState { it.copy(notice = UiNotice(resId = resId)) }

    private fun updateDraft(transform: (ConvertAccountDraft) -> ConvertAccountDraft) {
        updateState { it.copy(draft = transform(it.draft)) }
    }

    private fun updateState(transform: (ConvertAccountUiState) -> ConvertAccountUiState) {
        _state.value = transform(currentState())
    }

    private fun currentState(): ConvertAccountUiState = _state.value ?: ConvertAccountUiState()
}
