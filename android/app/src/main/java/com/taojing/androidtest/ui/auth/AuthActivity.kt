package com.taojing.androidtest.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.taojing.androidtest.R
import com.taojing.androidtest.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity(), AuthMethodFragment.Callbacks {

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.addOnBackStackChangedListener { updateChrome() }

        if (savedInstanceState == null) {
            replaceFragment(AuthMethodFragment(), addToBackStack = false)
        }

        updateChrome()
    }

    override fun onOpenPhoneAuth(countryCode: String) {
        replaceFragment(PhoneAuthFragment.newInstance(countryCode), addToBackStack = true)
    }

    override fun onOpenEmailAuth(countryCode: String) {
        replaceFragment(EmailAuthFragment.newInstance(countryCode), addToBackStack = true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.authFragmentContainer, fragment)
        if (addToBackStack) transaction.addToBackStack(fragment::class.java.simpleName)
        transaction.commit()
    }

    private fun updateChrome() {
        supportActionBar?.setDisplayHomeAsUpEnabled(supportFragmentManager.backStackEntryCount > 0)
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, AuthActivity::class.java))
        }

        fun startNewTask(context: Context) {
            context.startActivity(
                Intent(context, AuthActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                },
            )
        }
    }
}
