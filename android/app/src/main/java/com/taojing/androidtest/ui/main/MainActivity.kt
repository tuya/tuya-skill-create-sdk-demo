package com.taojing.androidtest.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.taojing.androidtest.CurrentHomeManager
import com.taojing.androidtest.R
import com.taojing.androidtest.databinding.ActivityMainBinding
import com.taojing.androidtest.ui.home.HomeFragment
import com.taojing.androidtest.ui.home.current.HomeChangeEventHandler
import com.taojing.androidtest.ui.profile.ProfileTabFragment
import com.thingclips.smart.bizbundle.initializer.BizBundleInitializer
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var homeChangeEventHandler: HomeChangeEventHandler

    private val homeFragment: HomeFragment by lazy { HomeFragment() }
    private val profileTabFragment: ProfileTabFragment by lazy { ProfileTabFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.mainFragmentContainer, homeFragment, TAG_HOME)
                .add(R.id.mainFragmentContainer, profileTabFragment, TAG_PROFILE)
                .hide(profileTabFragment)
                .commit()
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> showTab(homeFragment)
                R.id.nav_profile -> showTab(profileTabFragment)
            }
            true
        }

        CurrentHomeManager.initialize(
            onReady = { homeId, homeName -> Log.d(TAG, "Current home ready: $homeId ($homeName)") },
            onEmpty = { Log.w(TAG, "No home available") }
        )

        BizBundleInitializer.onLogin()

        homeChangeEventHandler = HomeChangeEventHandler(this)
        homeChangeEventHandler.register()

        lifecycleScope.launch {
            homeChangeEventHandler.sharedDeviceFlow.collect { devices ->
                homeFragment.notifySharedDevicesChanged(devices)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        homeChangeEventHandler.unregister()
    }

    private fun showTab(target: Fragment) {
        val tx = supportFragmentManager.beginTransaction()
        listOf(homeFragment, profileTabFragment).forEach { fragment ->
            if (fragment === target) tx.show(fragment) else tx.hide(fragment)
        }
        tx.commit()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val TAG_HOME = "tag_home"
        private const val TAG_PROFILE = "tag_profile"

        fun start(context: Context) {
            context.startActivity(Intent(context, MainActivity::class.java))
        }
    }
}
