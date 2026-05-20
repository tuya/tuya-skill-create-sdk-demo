package com.taojing.androidtest.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.taojing.androidtest.CurrentHomeManager
import com.taojing.androidtest.R
import com.taojing.androidtest.databinding.FragmentHomeBinding
import com.taojing.androidtest.ui.home.current.HomeSwitchBottomSheet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.thingclips.smart.activator.plug.mesosphere.ThingDeviceActivatorManager
import com.thingclips.smart.api.MicroContext
import com.thingclips.smart.api.service.MicroServiceManager
import com.thingclips.smart.commonbiz.bizbundle.family.api.AbsBizBundleFamilyService
import com.thingclips.smart.panelcaller.api.AbsPanelCallerService
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = checkNotNull(_binding)

    private val viewModel: HomeViewModel by viewModels()

    private val adapter by lazy {
        DeviceListAdapter(
            onItemClick = ::onDeviceClick,
            onItemLongClick = ::onDeviceLongClick,
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
        observeCurrentHome()
    }

    private fun setupToolbar() {
        binding.toolbar.inflateMenu(R.menu.menu_home)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_add_device -> { ThingDeviceActivatorManager.startDeviceActiveAction(requireActivity()); true }
                else -> false
            }
        }
        binding.tvCurrentHome.setOnClickListener {
            HomeSwitchBottomSheet().show(childFragmentManager, HomeSwitchBottomSheet.TAG)
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerDevices.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerDevices.adapter = adapter
        val swipeCallback = SwipeDeleteCallback(adapter) { _, device -> showDeleteConfirmDialog(device) }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerDevices)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.devices.collect { adapter.submitList(it) } }
                launch { viewModel.isRefreshing.collect { binding.swipeRefresh.isRefreshing = it } }
                launch { viewModel.isEmpty.collect { binding.layoutEmpty.visibility = if (it) View.VISIBLE else View.GONE } }
            }
        }
    }

    private fun observeCurrentHome() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                CurrentHomeManager.currentHomeIdFlow.collect { homeId ->
                    val homeName = CurrentHomeManager.currentHomeNameFlow.value.orEmpty()
                    binding.tvCurrentHome.text = homeName.ifEmpty { getString(R.string.tab_home) }
                    if (homeId != null) {
                        syncFamilyService(homeId, homeName)
                        viewModel.loadDevices(homeId)
                    }
                }
            }
        }
    }

    private fun syncFamilyService(homeId: Long, homeName: String) {
        val familyService = MicroServiceManager.getInstance()
            .findServiceByInterface(AbsBizBundleFamilyService::class.java.name)
                as? AbsBizBundleFamilyService
        familyService?.shiftCurrentFamily(homeId, homeName)
    }

    private fun onDeviceClick(device: DeviceListItem.Device) {
        val panelService = MicroContext.getServiceManager()
            .findServiceByInterface(AbsPanelCallerService::class.java.name)
                as? AbsPanelCallerService
        if (panelService == null) {
            Toast.makeText(requireContext(), R.string.panel_service_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        panelService.goPanelWithCheckAndTip(requireActivity(), device.devId)
    }

    private fun onDeviceLongClick(device: DeviceListItem.Device) {
        if (device.isShared) showSharedDeviceInfo(device) else showRenameDialog(device)
    }

    private fun showRenameDialog(device: DeviceListItem.Device) {
        val dialogContext = android.view.ContextThemeWrapper(requireContext(), com.google.android.material.R.style.Theme_MaterialComponents_DayNight_Dialog_Alert)
        val editText = EditText(dialogContext).apply { setText(device.name); setSelection(device.name.length); setPadding(48, 32, 48, 16) }
        MaterialAlertDialogBuilder(dialogContext)
            .setTitle(R.string.device_rename_title)
            .setView(editText)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != device.name) {
                    viewModel.renameDevice(device.devId, newName) { success ->
                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), if (success) R.string.device_rename_success else R.string.device_rename_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showSharedDeviceInfo(device: DeviceListItem.Device) {
        val statusText = if (device.isOnline) getString(R.string.device_online) else getString(R.string.device_offline)
        val dialogContext = android.view.ContextThemeWrapper(requireContext(), com.google.android.material.R.style.Theme_MaterialComponents_DayNight_Dialog_Alert)
        MaterialAlertDialogBuilder(dialogContext)
            .setTitle(R.string.device_shared_info_title)
            .setMessage(getString(R.string.device_shared_info_message, device.name, statusText))
            .setPositiveButton(R.string.confirm, null)
            .show()
    }

    private fun showDeleteConfirmDialog(device: DeviceListItem.Device) {
        val dialogContext = android.view.ContextThemeWrapper(requireContext(), com.google.android.material.R.style.Theme_MaterialComponents_DayNight_Dialog_Alert)
        val (title, message) = if (device.isShared) {
            R.string.device_remove_shared_title to getString(R.string.device_remove_shared_message, device.name)
        } else {
            R.string.device_remove_title to getString(R.string.device_remove_message, device.name)
        }
        MaterialAlertDialogBuilder(dialogContext)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.confirm) { _, _ -> performDeleteDevice(device) }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener { adapter.notifyDataSetChanged() }
            .show()
    }

    private fun performDeleteDevice(device: DeviceListItem.Device) {
        val callback: (Boolean) -> Unit = { success ->
            activity?.runOnUiThread {
                Toast.makeText(requireContext(), if (success) R.string.device_remove_success else R.string.device_remove_failed, Toast.LENGTH_SHORT).show()
            }
        }
        if (device.isShared) viewModel.removeSharedDevice(device.devId, callback) else viewModel.removeDevice(device.devId, callback)
    }

    fun notifySharedDevicesChanged(devices: List<com.thingclips.smart.sdk.bean.DeviceBean>) {
        viewModel.updateSharedDevices(devices)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
