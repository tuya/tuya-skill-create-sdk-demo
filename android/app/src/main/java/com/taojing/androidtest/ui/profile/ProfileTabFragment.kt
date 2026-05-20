package com.taojing.androidtest.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.taojing.androidtest.R
import com.taojing.androidtest.databinding.FragmentProfileTabBinding

class ProfileTabFragment : Fragment(),
    ProfileFragment.Callbacks,
    ConvertAccountFragment.Callbacks {

    private var _binding: FragmentProfileTabBinding? = null
    private val binding get() = checkNotNull(_binding)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (childFragmentManager.findFragmentById(R.id.profileTabContainer) == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.profileTabContainer, ProfileFragment())
                .commit()
        }
    }

    override fun onOpenConvertAccount() {
        childFragmentManager.beginTransaction()
            .replace(R.id.profileTabContainer, ConvertAccountFragment())
            .addToBackStack(ConvertAccountFragment::class.java.simpleName)
            .commit()
    }

    override fun onConvertAccountClosed() {
        childFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
