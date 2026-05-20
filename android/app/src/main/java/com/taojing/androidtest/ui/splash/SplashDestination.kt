package com.taojing.androidtest.ui.splash

enum class SplashDestination {
    AUTH,
    PROFILE;

    companion object {
        fun fromLoginState(isLoggedIn: Boolean): SplashDestination {
            return if (isLoggedIn) PROFILE else AUTH
        }
    }
}
