package com.taojing.androidtest.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.thingclips.smart.api.router.UrlRouter
import com.thingclips.smart.api.service.MicroServiceManager
import com.thingclips.smart.bizbundle.initializer.BizBundleInitializer
import com.thingclips.smart.commonbiz.bizbundle.family.api.AbsBizBundleFamilyService
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.bean.HomeBean
import com.thingclips.smart.home.sdk.callback.IThingGetHomeListCallback
import com.taojing.androidtest.R
import com.taojing.androidtest.databinding.FragmentProfileBinding
import com.taojing.androidtest.ui.auth.AuthActivity
import com.taojing.androidtest.ui.common.updateTextIfChanged

class ProfileFragment : Fragment() {

    interface Callbacks {
        fun onOpenConvertAccount()
    }

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = checkNotNull(_binding)

    private lateinit var viewModel: ProfileViewModel
    private var callbacks: Callbacks? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = parentFragment as? Callbacks ?: context as? Callbacks
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        binding.etProfileNickname.doAfterTextChanged { viewModel.onNicknameChanged(it?.toString().orEmpty()) }

        binding.btnProfileUpdateNickname.setOnClickListener { viewModel.updateNickname() }
        binding.btnProfileRefresh.setOnClickListener { viewModel.refreshUserInfo() }
        binding.btnProfileFamilyManage.setOnClickListener { openFamilyManage() }
        binding.btnProfileLogout.setOnClickListener { viewModel.logout() }
        binding.btnProfileCancelAccount.setOnClickListener { showCancelAccountDialog() }
        binding.btnProfileConvert.setOnClickListener { callbacks?.onOpenConvertAccount() }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.tvProfileDisplayName.text = state.summary.displayName
            binding.tvProfileAccount.text = state.summary.email ?: state.summary.mobile ?: state.summary.displayName
            binding.tvProfileUid.text = state.summary.uid ?: "-"
            binding.tvProfileSid.text = state.summary.sid ?: "-"
            binding.tvProfileGuestHint.isVisible = state.summary.isTourist
            binding.btnProfileConvert.isVisible = state.summary.isTourist
            binding.btnProfileLogout.setText(
                if (state.summary.isTourist) R.string.profile_tourist_logout else R.string.profile_logout
            )
            binding.etProfileNickname.updateTextIfChanged(state.nicknameInput)
            binding.progressProfile.isVisible = state.isLoading

            binding.etProfileNickname.isEnabled = !state.isLoading
            binding.btnProfileUpdateNickname.isEnabled = !state.isLoading
            binding.btnProfileRefresh.isEnabled = !state.isLoading
            binding.btnProfileFamilyManage.isEnabled = !state.isLoading
            binding.btnProfileConvert.isEnabled = !state.isLoading
            binding.btnProfileLogout.isEnabled = !state.isLoading
            binding.btnProfileCancelAccount.isEnabled = !state.isLoading

            state.notice?.let { notice ->
                Toast.makeText(requireContext(), notice.resolve(requireContext()), Toast.LENGTH_SHORT).show()
                viewModel.clearNotice()
            }

            if (state.navigateToAuth) {
                viewModel.consumeNavigateToAuth()
                BizBundleInitializer.onLogout(requireContext())
                AuthActivity.startNewTask(requireContext())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadCurrentUser()
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun openFamilyManage() {
        val familyService = MicroServiceManager.getInstance()
            .findServiceByInterface(AbsBizBundleFamilyService::class.java.name)
                as? AbsBizBundleFamilyService ?: return

        ThingHomeSdk.getHomeManagerInstance().queryHomeList(object : IThingGetHomeListCallback {
            override fun onSuccess(homeBeans: List<HomeBean>) {
                val home = homeBeans.firstOrNull()
                if (home != null) familyService.shiftCurrentFamily(home.homeId, home.name)
                UrlRouter.execute(UrlRouter.makeBuilder(requireContext(), "family_manage"))
            }
            override fun onError(errorCode: String, error: String) {
                UrlRouter.execute(UrlRouter.makeBuilder(requireContext(), "family_manage"))
            }
        })
    }

    private fun showCancelAccountDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.cancel_account_confirm_title)
            .setMessage(R.string.cancel_account_confirm_message)
            .setPositiveButton(R.string.confirm) { _, _ -> viewModel.cancelAccount() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
