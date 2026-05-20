package com.taojing.androidtest.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.taojing.androidtest.R
import com.taojing.androidtest.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity(), ProfileFragment.Callbacks, ConvertAccountFragment.Callbacks {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.addOnBackStackChangedListener { updateChrome() }

        if (savedInstanceState == null) {
            replaceFragment(ProfileFragment(), addToBackStack = false)
        }

        updateChrome()
    }

    override fun onOpenConvertAccount() {
        replaceFragment(ConvertAccountFragment(), addToBackStack = true)
    }

    override fun onConvertAccountClosed() {
        onBackPressedDispatcher.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.profileFragmentContainer, fragment)
        if (addToBackStack) transaction.addToBackStack(fragment::class.java.simpleName)
        transaction.commit()
    }

    private fun updateChrome() {
        supportActionBar?.setDisplayHomeAsUpEnabled(supportFragmentManager.backStackEntryCount > 0)
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ProfileActivity::class.java))
        }
    }
}
