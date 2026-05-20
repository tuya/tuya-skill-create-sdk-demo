package com.taojing.androidtest.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.taojing.androidtest.R
import com.taojing.androidtest.databinding.FragmentPhoneAuthBinding
import com.taojing.androidtest.ui.auth.phone.PhoneAuthMode
import com.taojing.androidtest.ui.auth.phone.PhoneAuthViewModel
import com.taojing.androidtest.ui.common.updateTextIfChanged
import com.taojing.androidtest.ui.main.MainActivity

class PhoneAuthFragment : Fragment() {

    private var _binding: FragmentPhoneAuthBinding? = null
    private val binding get() = checkNotNull(_binding)

    private lateinit var viewModel: PhoneAuthViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPhoneAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[PhoneAuthViewModel::class.java]
        viewModel.initializeCountryCode(arguments?.getString(ARG_COUNTRY_CODE).orEmpty())

        binding.rgPhoneMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rbPhoneRegister -> PhoneAuthMode.REGISTER
                R.id.rbPhoneCodeLogin -> PhoneAuthMode.CODE_LOGIN
                R.id.rbPhoneReset -> PhoneAuthMode.RESET_PASSWORD
                else -> PhoneAuthMode.PASSWORD_LOGIN
            }
            viewModel.updateMode(mode)
        }

        binding.etPhoneCountryCode.doAfterTextChanged { viewModel.updateCountryCode(it?.toString().orEmpty()) }
        binding.etPhoneNumber.doAfterTextChanged { viewModel.updatePhone(it?.toString().orEmpty()) }
        binding.etPhonePassword.doAfterTextChanged { viewModel.updatePassword(it?.toString().orEmpty()) }
        binding.etPhoneCode.doAfterTextChanged { viewModel.updateCode(it?.toString().orEmpty()) }

        binding.btnPhoneSendCode.setOnClickListener { viewModel.sendCode() }
        binding.btnPhoneSubmit.setOnClickListener { viewModel.submit() }
        binding.btnPhoneBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.etPhoneCountryCode.updateTextIfChanged(state.draft.countryCode)
            binding.etPhoneNumber.updateTextIfChanged(state.draft.phone)
            binding.etPhonePassword.updateTextIfChanged(state.draft.password)
            binding.etPhoneCode.updateTextIfChanged(state.draft.code)

            val checkedButton = when (state.draft.mode) {
                PhoneAuthMode.REGISTER -> R.id.rbPhoneRegister
                PhoneAuthMode.PASSWORD_LOGIN -> R.id.rbPhonePasswordLogin
                PhoneAuthMode.CODE_LOGIN -> R.id.rbPhoneCodeLogin
                PhoneAuthMode.RESET_PASSWORD -> R.id.rbPhoneReset
            }
            if (binding.rgPhoneMode.checkedRadioButtonId != checkedButton) binding.rgPhoneMode.check(checkedButton)

            binding.etPhonePassword.isVisible = state.draft.mode != PhoneAuthMode.CODE_LOGIN
            binding.etPhoneCode.isVisible = state.draft.mode != PhoneAuthMode.PASSWORD_LOGIN
            binding.btnPhoneSendCode.isVisible = state.draft.mode != PhoneAuthMode.PASSWORD_LOGIN
            binding.progressPhone.isVisible = state.isLoading

            binding.etPhoneCountryCode.isEnabled = !state.isLoading
            binding.etPhoneNumber.isEnabled = !state.isLoading
            binding.etPhonePassword.isEnabled = !state.isLoading
            binding.etPhoneCode.isEnabled = !state.isLoading
            binding.btnPhoneSendCode.isEnabled = !state.isLoading
            binding.btnPhoneSubmit.isEnabled = !state.isLoading
            binding.btnPhoneBack.isEnabled = !state.isLoading

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
        requireActivity().title = getString(R.string.phone_auth_title)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_COUNTRY_CODE = "country_code"

        fun newInstance(countryCode: String) = PhoneAuthFragment().apply {
            arguments = Bundle().apply { putString(ARG_COUNTRY_CODE, countryCode) }
        }
    }
}
