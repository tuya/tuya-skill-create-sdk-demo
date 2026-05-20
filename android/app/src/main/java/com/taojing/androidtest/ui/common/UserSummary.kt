package com.taojing.androidtest.ui.common

import com.thingclips.smart.android.user.bean.User

data class UserSummary(
    val displayName: String,
    val uid: String?,
    val sid: String?,
    val email: String?,
    val mobile: String?,
    val isTourist: Boolean,
) {
    companion object {
        fun fromUser(user: User?): UserSummary {
            if (user == null) {
                return UserSummary(
                    displayName = "未登录",
                    uid = null,
                    sid = null,
                    email = null,
                    mobile = null,
                    isTourist = false,
                )
            }

            val displayName = user.nickName
                ?.takeIf { it.isNotBlank() }
                ?: user.username
                    ?.takeIf { it.isNotBlank() }
                ?: user.email
                    ?.takeIf { it.isNotBlank() }
                ?: user.mobile
                    ?.takeIf { it.isNotBlank() }
                ?: "未命名用户"

            val usernameLooksLikeAccount = user.username?.let { value ->
                value.contains("@") || value.any(Char::isDigit)
            } == true

            val isTourist = user.mobile.isNullOrBlank() &&
                user.email.isNullOrBlank() &&
                !usernameLooksLikeAccount

            return UserSummary(
                displayName = displayName,
                uid = user.uid,
                sid = user.sid,
                email = user.email,
                mobile = user.mobile,
                isTourist = isTourist,
            )
        }
    }
}
