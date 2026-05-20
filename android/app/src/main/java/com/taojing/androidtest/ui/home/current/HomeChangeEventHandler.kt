package com.taojing.androidtest.ui.home.current

import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.taojing.androidtest.CurrentHomeManager
import com.taojing.androidtest.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.api.IThingHomeChangeListener
import com.thingclips.smart.home.sdk.bean.HomeBean
import com.thingclips.smart.sdk.api.IResultCallback
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class HomeChangeEventHandler(
    private val activity: FragmentActivity,
) {
    companion object {
        private const val TAG = "HomeChangeEvent"
    }

    private val _sharedDeviceFlow = MutableSharedFlow<List<com.thingclips.smart.sdk.bean.DeviceBean>>(
        replay = 1,
        extraBufferCapacity = 1,
    )
    val sharedDeviceFlow: SharedFlow<List<com.thingclips.smart.sdk.bean.DeviceBean>> = _sharedDeviceFlow.asSharedFlow()

    private val listener = object : IThingHomeChangeListener {

        override fun onHomeInvite(homeId: Long, homeName: String) {
            Log.d(TAG, "onHomeInvite: $homeId ($homeName)")
            activity.runOnUiThread { showInviteDialog(homeId, homeName) }
        }

        override fun onHomeRemoved(homeId: Long) {
            Log.d(TAG, "onHomeRemoved: $homeId")
            activity.runOnUiThread { handleHomeRemoved(homeId) }
        }

        override fun onHomeAdded(homeId: Long) {
            Log.d(TAG, "onHomeAdded: $homeId")
            CurrentHomeManager.refreshHomeList()
            activity.runOnUiThread { Toast.makeText(activity, R.string.home_event_added, Toast.LENGTH_SHORT).show() }
        }

        override fun onHomeInfoChanged(homeId: Long) {
            Log.d(TAG, "onHomeInfoChanged: $homeId")
            if (homeId == CurrentHomeManager.currentHomeId) CurrentHomeManager.refreshHomeList()
        }

        override fun onSharedDeviceList(sharedDeviceList: MutableList<com.thingclips.smart.sdk.bean.DeviceBean>?) {
            Log.d(TAG, "onSharedDeviceList: ${sharedDeviceList?.size} devices")
            _sharedDeviceFlow.tryEmit(sharedDeviceList?.toList().orEmpty())
        }

        override fun onSharedGroupList(sharedGroupList: MutableList<com.thingclips.smart.sdk.bean.GroupBean>?) {
            Log.d(TAG, "onSharedGroupList: ${sharedGroupList?.size} groups")
        }

        override fun onServerConnectSuccess() {
            Log.d(TAG, "onServerConnectSuccess")
            CurrentHomeManager.currentHomeId?.let { homeId -> CurrentHomeManager.switchHome(homeId) }
        }
    }

    fun register() {
        ThingHomeSdk.getHomeManagerInstance().registerThingHomeChangeListener(listener)
        Log.d(TAG, "Registered home change listener")
    }

    fun unregister() {
        ThingHomeSdk.getHomeManagerInstance().unRegisterThingHomeChangeListener(listener)
        Log.d(TAG, "Unregistered home change listener")
    }

    private fun showInviteDialog(homeId: Long, homeName: String) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.home_invite_title)
            .setMessage(activity.getString(R.string.home_invite_message, homeName))
            .setPositiveButton(R.string.home_invite_accept) { _, _ -> processInvitation(homeId, homeName, accept = true) }
            .setNegativeButton(R.string.home_invite_reject) { _, _ -> processInvitation(homeId, homeName, accept = false) }
            .setCancelable(false)
            .show()
    }

    private fun processInvitation(homeId: Long, homeName: String, accept: Boolean) {
        ThingHomeSdk.getMemberInstance().processInvitation(homeId, accept, object : IResultCallback {
            override fun onSuccess() {
                activity.runOnUiThread {
                    if (accept) {
                        Toast.makeText(activity, R.string.home_invite_accepted, Toast.LENGTH_SHORT).show()
                        CurrentHomeManager.switchHome(homeId, homeName)
                        CurrentHomeManager.refreshHomeList()
                    } else {
                        Toast.makeText(activity, R.string.home_invite_rejected, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onError(code: String, error: String) {
                Log.e(TAG, "processInvitation failed: $code $error")
                activity.runOnUiThread { Toast.makeText(activity, "操作失败: $code", Toast.LENGTH_SHORT).show() }
            }
        })
    }

    private fun handleHomeRemoved(homeId: Long) {
        val isCurrentHome = homeId == CurrentHomeManager.currentHomeId

        if (isCurrentHome) {
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.home_removed_title)
                .setMessage(R.string.home_removed_current_message)
                .setPositiveButton(R.string.confirm) { _, _ ->
                    CurrentHomeManager.refreshHomeList { homes ->
                        if (homes.isNotEmpty()) {
                            val fallback = homes[0]
                            CurrentHomeManager.switchHome(fallback.homeId, fallback.name)
                        } else {
                            CurrentHomeManager.clear()
                        }
                    }
                }
                .setCancelable(false)
                .show()
        } else {
            Toast.makeText(activity, R.string.home_removed_other_message, Toast.LENGTH_SHORT).show()
            CurrentHomeManager.refreshHomeList()
        }
    }
}
