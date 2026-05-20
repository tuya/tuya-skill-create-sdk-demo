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
import com.taojing.androidtest.databinding.FragmentEmailAuthBinding
import com.taojing.androidtest.ui.auth.email.EmailAuthMode
import com.taojing.androidtest.ui.auth.email.EmailAuthViewModel
import com.taojing.androidtest.ui.common.updateTextIfChanged
import com.taojing.androidtest.ui.main.MainActivity

class EmailAuthFragment : Fragment() {

    private var _binding: FragmentEmailAuthBinding? = null
    private val binding get() = checkNotNull(_binding)

    private lateinit var viewModel: EmailAuthViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEmailAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[EmailAuthViewModel::class.java]
        viewModel.initializeCountryCode(arguments?.getString(ARG_COUNTRY_CODE).orEmpty())

        binding.rgEmailMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rbEmailRegister -> EmailAuthMode.REGISTER
                R.id.rbEmailCodeLogin -> EmailAuthMode.CODE_LOGIN
                R.id.rbEmailReset -> EmailAuthMode.RESET_PASSWORD
                else -> EmailAuthMode.PASSWORD_LOGIN
            }
            viewModel.updateMode(mode)
        }

        binding.etEmailCountryCode.doAfterTextChanged { viewModel.updateCountryCode(it?.toString().orEmpty()) }
        binding.etEmailAddress.doAfterTextChanged { viewModel.updateEmail(it?.toString().orEmpty()) }
        binding.etEmailPassword.doAfterTextChanged { viewModel.updatePassword(it?.toString().orEmpty()) }
        binding.etEmailCode.doAfterTextChanged { viewModel.updateCode(it?.toString().orEmpty()) }

        binding.btnEmailSendCode.setOnClickListener { viewModel.sendCode() }
        binding.btnEmailSubmit.setOnClickListener { viewModel.submit() }
        binding.btnEmailBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.etEmailCountryCode.updateTextIfChanged(state.draft.countryCode)
            binding.etEmailAddress.updateTextIfChanged(state.draft.email)
            binding.etEmailPassword.updateTextIfChanged(state.draft.password)
            binding.etEmailCode.updateTextIfChanged(state.draft.code)

            val checkedButton = when (state.draft.mode) {
                EmailAuthMode.REGISTER -> R.id.rbEmailRegister
                EmailAuthMode.PASSWORD_LOGIN -> R.id.rbEmailPasswordLogin
                EmailAuthMode.CODE_LOGIN -> R.id.rbEmailCodeLogin
                EmailAuthMode.RESET_PASSWORD -> R.id.rbEmailReset
            }
            if (binding.rgEmailMode.checkedRadioButtonId != checkedButton) binding.rgEmailMode.check(checkedButton)

            binding.etEmailPassword.isVisible = state.draft.mode != EmailAuthMode.CODE_LOGIN
            binding.etEmailCode.isVisible = state.draft.mode != EmailAuthMode.PASSWORD_LOGIN
            binding.btnEmailSendCode.isVisible = state.draft.mode != EmailAuthMode.PASSWORD_LOGIN
            binding.progressEmail.isVisible = state.isLoading

            binding.etEmailCountryCode.isEnabled = !state.isLoading
            binding.etEmailAddress.isEnabled = !state.isLoading
            binding.etEmailPassword.isEnabled = !state.isLoading
            binding.etEmailCode.isEnabled = !state.isLoading
            binding.btnEmailSendCode.isEnabled = !state.isLoading
            binding.btnEmailSubmit.isEnabled = !state.isLoading
            binding.btnEmailBack.isEnabled = !state.isLoading

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
        requireActivity().title = getString(R.string.email_auth_title)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_COUNTRY_CODE = "country_code"

        fun newInstance(countryCode: String) = EmailAuthFragment().apply {
            arguments = Bundle().apply { putString(ARG_COUNTRY_CODE, countryCode) }
        }
    }
}
