package com.taojing.androidtest.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.thingclips.smart.android.user.api.ILogoutCallback
import com.thingclips.smart.android.user.api.IReNickNameCallback
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.sdk.api.IResultCallback
import com.taojing.androidtest.R
import com.taojing.androidtest.ui.common.UiNotice
import com.taojing.androidtest.ui.common.UserSummary
import com.taojing.androidtest.ui.common.formatTuyaError

data class ProfileUiState(
    val summary: UserSummary = UserSummary.fromUser(null),
    val nicknameInput: String = "",
    val isLoading: Boolean = false,
    val notice: UiNotice? = null,
    val navigateToAuth: Boolean = false,
)

class ProfileViewModel : ViewModel() {

    private val _state = MutableLiveData(ProfileUiState())
    val state: LiveData<ProfileUiState> = _state

    init {
        loadCurrentUser()
    }

    fun loadCurrentUser() {
        val user = ThingHomeSdk.getUserInstance().user
        updateState { it.copy(summary = UserSummary.fromUser(user), nicknameInput = user?.nickName.orEmpty()) }
    }

    fun onNicknameChanged(nickname: String) = updateState { it.copy(nicknameInput = nickname) }

    fun refreshUserInfo() {
        updateState { it.copy(isLoading = true, notice = null) }
        ThingHomeSdk.getUserInstance().updateUserInfo(object : IResultCallback {
            override fun onSuccess() {
                val user = ThingHomeSdk.getUserInstance().user
                updateState { it.copy(summary = UserSummary.fromUser(user), nicknameInput = user?.nickName.orEmpty(), isLoading = false, notice = UiNotice(resId = R.string.message_refresh_success)) }
            }
            override fun onError(code: String, error: String) = fail(code, error)
        })
    }

    fun updateNickname() {
        val nickname = currentState().nicknameInput.trim()
        if (nickname.isBlank()) {
            updateState { it.copy(notice = UiNotice(resId = R.string.message_nickname_required)) }
            return
        }
        updateState { it.copy(isLoading = true, notice = null) }
        ThingHomeSdk.getUserInstance().updateNickName(nickname, object : IReNickNameCallback {
            override fun onSuccess() {
                val user = ThingHomeSdk.getUserInstance().user?.apply { nickName = nickname }
                updateState { it.copy(summary = UserSummary.fromUser(user), nicknameInput = nickname, isLoading = false, notice = UiNotice(resId = R.string.message_nickname_updated)) }
            }
            override fun onError(code: String, error: String) = fail(code, error)
        })
    }

    fun logout() {
        updateState { it.copy(isLoading = true, notice = null) }
        val callback = object : ILogoutCallback {
            override fun onSuccess() {
                updateState { it.copy(isLoading = false, notice = UiNotice(resId = R.string.message_logout_success), navigateToAuth = true) }
            }
            override fun onError(code: String, error: String) = fail(code, error)
        }
        if (currentState().summary.isTourist) {
            ThingHomeSdk.getUserInstance().touristLogOut(callback)
        } else {
            ThingHomeSdk.getUserInstance().logout(callback)
        }
    }

    fun cancelAccount() {
        updateState { it.copy(isLoading = true, notice = null) }
        ThingHomeSdk.getUserInstance().cancelAccount(object : IResultCallback {
            override fun onSuccess() {
                updateState { it.copy(isLoading = false, notice = UiNotice(resId = R.string.message_cancel_account_success), navigateToAuth = true) }
            }
            override fun onError(code: String, error: String) = fail(code, error)
        })
    }

    fun clearNotice() = updateState { it.copy(notice = null) }

    fun consumeNavigateToAuth() = updateState { it.copy(navigateToAuth = false) }

    private fun fail(code: String, error: String) {
        updateState { it.copy(isLoading = false, notice = UiNotice(text = formatTuyaError(code, error))) }
    }

    private fun updateState(transform: (ProfileUiState) -> ProfileUiState) {
        _state.value = transform(currentState())
    }

    private fun currentState(): ProfileUiState = _state.value ?: ProfileUiState()
}
