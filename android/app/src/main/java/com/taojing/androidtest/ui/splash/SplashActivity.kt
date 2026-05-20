package com.taojing.androidtest.ui.splash

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.taojing.androidtest.databinding.ActivitySplashBinding
import com.taojing.androidtest.ui.auth.AuthActivity
import com.taojing.androidtest.ui.main.MainActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val destination = SplashDestination.fromLoginState(ThingHomeSdk.getUserInstance().isLogin)

        when (destination) {
            SplashDestination.AUTH -> AuthActivity.start(this)
            SplashDestination.PROFILE -> MainActivity.start(this)
        }

        finish()
    }
}
