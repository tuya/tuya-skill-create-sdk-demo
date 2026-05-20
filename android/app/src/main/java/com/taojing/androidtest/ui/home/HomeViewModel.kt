package com.taojing.androidtest.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import com.taojing.androidtest.CurrentHomeManager
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.bean.HomeBean
import com.thingclips.smart.home.sdk.callback.IThingHomeResultCallback
import com.thingclips.smart.sdk.api.IResultCallback
import com.thingclips.smart.sdk.bean.DeviceBean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _devices = MutableStateFlow<List<DeviceListItem>>(emptyList())
    val devices: StateFlow<List<DeviceListItem>> = _devices.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isEmpty = MutableStateFlow(true)
    val isEmpty: StateFlow<Boolean> = _isEmpty.asStateFlow()

    private var homeDevices: List<DeviceBean> = emptyList()
    private var sharedDevices: List<DeviceBean> = emptyList()

    private var currentListeningHomeId: Long? = null
    private var homeStatusListener: com.thingclips.smart.home.sdk.api.IThingHomeStatusListener? = null

    fun loadDevices(homeId: Long) {
        _isRefreshing.value = true
        unregisterHomeListener()

        ThingHomeSdk.newHomeInstance(homeId).getHomeDetail(object : IThingHomeResultCallback {
            override fun onSuccess(bean: HomeBean) {
                homeDevices = bean.deviceList ?: emptyList()
                sharedDevices = bean.sharedDeviceList ?: emptyList()
                rebuildList()
                _isRefreshing.value = false
                registerHomeListener(homeId)
            }

            override fun onError(errorCode: String, errorMsg: String) {
                Log.e(TAG, "getHomeDetail failed: $errorCode $errorMsg")
                _isRefreshing.value = false
            }
        })
    }

    fun refresh() {
        val homeId = CurrentHomeManager.currentHomeId ?: return
        loadDevices(homeId)
    }

    fun updateSharedDevices(devices: List<DeviceBean>) {
        sharedDevices = devices
        rebuildList()
    }

    fun renameDevice(devId: String, newName: String, onResult: (Boolean) -> Unit) {
        ThingHomeSdk.newDeviceInstance(devId).renameDevice(newName, object : IResultCallback {
            override fun onSuccess() { refresh(); onResult(true) }
            override fun onError(code: String, error: String) { Log.e(TAG, "renameDevice failed: $code $error"); onResult(false) }
        })
    }

    fun removeDevice(devId: String, onResult: (Boolean) -> Unit) {
        ThingHomeSdk.newDeviceInstance(devId).removeDevice(object : IResultCallback {
            override fun onSuccess() { homeDevices = homeDevices.filter { it.devId != devId }; rebuildList(); onResult(true) }
            override fun onError(code: String, error: String) { Log.e(TAG, "removeDevice failed: $code $error"); onResult(false) }
        })
    }

    fun removeSharedDevice(devId: String, onResult: (Boolean) -> Unit) {
        ThingHomeSdk.getDeviceShareInstance()
            ?.removeReceivedDevShare(devId, object : IResultCallback {
                override fun onSuccess() { sharedDevices = sharedDevices.filter { it.devId != devId }; rebuildList(); onResult(true) }
                override fun onError(code: String, error: String) { Log.e(TAG, "removeReceivedDevShare failed: $code $error"); onResult(false) }
            })
    }

    private fun registerHomeListener(homeId: Long) {
        currentListeningHomeId = homeId
        val listener = object : com.thingclips.smart.home.sdk.api.IThingHomeStatusListener {
            override fun onDeviceAdded(devId: String) { Log.d(TAG, "onDeviceAdded: $devId"); refresh() }
            override fun onDeviceRemoved(devId: String) { Log.d(TAG, "onDeviceRemoved: $devId"); homeDevices = homeDevices.filter { it.devId != devId }; rebuildList() }
            override fun onGroupAdded(groupId: Long) { Log.d(TAG, "onGroupAdded: $groupId") }
            override fun onGroupRemoved(groupId: Long) { Log.d(TAG, "onGroupRemoved: $groupId") }
            override fun onMeshAdded(meshId: String) { Log.d(TAG, "onMeshAdded: $meshId") }
        }
        homeStatusListener = listener
        ThingHomeSdk.newHomeInstance(homeId).registerHomeStatusListener(listener)
    }

    private fun unregisterHomeListener() {
        val homeId = currentListeningHomeId ?: return
        homeStatusListener?.let { ThingHomeSdk.newHomeInstance(homeId).unRegisterHomeStatusListener(it) }
        homeStatusListener = null
        currentListeningHomeId = null
    }

    private fun rebuildList() {
        val items = mutableListOf<DeviceListItem>()
        if (homeDevices.isNotEmpty()) {
            items += DeviceListItem.Header("我的设备")
            items += homeDevices.map { it.toListItem(isShared = false) }
        }
        if (sharedDevices.isNotEmpty()) {
            items += DeviceListItem.Header("共享设备")
            items += sharedDevices.map { it.toListItem(isShared = true) }
        }
        _devices.value = items
        _isEmpty.value = items.isEmpty()
    }

    private fun DeviceBean.toListItem(isShared: Boolean) = DeviceListItem.Device(
        devId = devId,
        name = name.orEmpty(),
        iconUrl = iconUrl.orEmpty(),
        isOnline = getIsOnline() == true,
        isShared = isShared,
    )

    override fun onCleared() {
        super.onCleared()
        unregisterHomeListener()
    }
}
