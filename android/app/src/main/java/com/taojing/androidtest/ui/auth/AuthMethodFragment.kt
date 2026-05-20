package com.taojing.androidtest.ui.auth

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.taojing.androidtest.R
import com.taojing.androidtest.databinding.FragmentAuthMethodBinding
import com.taojing.androidtest.ui.common.updateTextIfChanged
import com.taojing.androidtest.ui.main.MainActivity

class AuthMethodFragment : Fragment() {

    interface Callbacks {
        fun onOpenPhoneAuth(countryCode: String)
        fun onOpenEmailAuth(countryCode: String)
    }

    private var _binding: FragmentAuthMethodBinding? = null
    private val binding get() = checkNotNull(_binding)

    private lateinit var viewModel: AuthMethodViewModel
    private var callbacks: Callbacks? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as? Callbacks
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAuthMethodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[AuthMethodViewModel::class.java]

        binding.etAuthCountryCode.doAfterTextChanged {
            viewModel.updateCountryCode(it?.toString().orEmpty())
        }

        binding.btnAuthPhone.setOnClickListener {
            callbacks?.onOpenPhoneAuth(binding.etAuthCountryCode.text.toString())
        }
        binding.btnAuthEmail.setOnClickListener {
            callbacks?.onOpenEmailAuth(binding.etAuthCountryCode.text.toString())
        }
        binding.btnAuthTourist.setOnClickListener { viewModel.touristLogin() }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.etAuthCountryCode.updateTextIfChanged(state.countryCode)
            binding.progressAuthMethod.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.btnAuthPhone.isEnabled = !state.isLoading
            binding.btnAuthEmail.isEnabled = !state.isLoading
            binding.btnAuthTourist.isEnabled = !state.isLoading
            binding.etAuthCountryCode.isEnabled = !state.isLoading

            state.notice?.let { notice ->
                Toast.makeText(requireContext(), notice.resolve(requireContext()), Toast.LENGTH_SHORT).show()
                viewModel.clearNotice()
            }

            if (state.navigateToProfile) {
                viewModel.consumeProfileNavigation()
                MainActivity.start(requireContext())
                requireActivity().finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = getString(R.string.auth_title)
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
