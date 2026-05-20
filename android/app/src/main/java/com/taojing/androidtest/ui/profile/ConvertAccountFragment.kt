package com.taojing.androidtest.ui.profile

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
import com.taojing.androidtest.databinding.FragmentConvertAccountBinding
import com.taojing.androidtest.ui.common.updateTextIfChanged

class ConvertAccountFragment : Fragment() {

    interface Callbacks {
        fun onConvertAccountClosed()
    }

    private var _binding: FragmentConvertAccountBinding? = null
    private val binding get() = checkNotNull(_binding)

    private lateinit var viewModel: ConvertAccountViewModel
    private var callbacks: Callbacks? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = parentFragment as? Callbacks ?: context as? Callbacks
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConvertAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[ConvertAccountViewModel::class.java]

        binding.rgConvertMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = if (checkedId == R.id.rbConvertEmail) ConvertAccountMode.EMAIL else ConvertAccountMode.PHONE
            viewModel.updateMode(mode)
        }

        binding.etConvertCountryCode.doAfterTextChanged { viewModel.updateCountryCode(it?.toString().orEmpty()) }
        binding.etConvertUserName.doAfterTextChanged { viewModel.updateUserName(it?.toString().orEmpty()) }
        binding.etConvertCode.doAfterTextChanged { viewModel.updateCode(it?.toString().orEmpty()) }
        binding.etConvertPassword.doAfterTextChanged { viewModel.updatePassword(it?.toString().orEmpty()) }

        binding.btnConvertSendCode.setOnClickListener { viewModel.sendCode() }
        binding.btnConvertSubmit.setOnClickListener { viewModel.submit() }
        binding.btnConvertBack.setOnClickListener { callbacks?.onConvertAccountClosed() }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.etConvertCountryCode.updateTextIfChanged(state.draft.countryCode)
            binding.etConvertUserName.updateTextIfChanged(state.draft.userName)
            binding.etConvertCode.updateTextIfChanged(state.draft.code)
            binding.etConvertPassword.updateTextIfChanged(state.draft.password)

            val checkedButton = if (state.draft.mode == ConvertAccountMode.EMAIL) R.id.rbConvertEmail else R.id.rbConvertPhone
            if (binding.rgConvertMode.checkedRadioButtonId != checkedButton) binding.rgConvertMode.check(checkedButton)

            binding.etConvertUserName.hint = getString(
                if (state.draft.mode == ConvertAccountMode.EMAIL) R.string.email_address else R.string.phone_number
            )

            binding.progressConvert.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.etConvertCountryCode.isEnabled = !state.isLoading
            binding.etConvertUserName.isEnabled = !state.isLoading
            binding.etConvertCode.isEnabled = !state.isLoading
            binding.etConvertPassword.isEnabled = !state.isLoading
            binding.btnConvertSendCode.isEnabled = !state.isLoading
            binding.btnConvertSubmit.isEnabled = !state.isLoading
            binding.btnConvertBack.isEnabled = !state.isLoading

            state.notice?.let { notice ->
                Toast.makeText(requireContext(), notice.resolve(requireContext()), Toast.LENGTH_SHORT).show()
                viewModel.clearNotice()
            }

            if (state.navigateBack) {
                viewModel.consumeNavigateBack()
                callbacks?.onConvertAccountClosed()
            }
        }
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
