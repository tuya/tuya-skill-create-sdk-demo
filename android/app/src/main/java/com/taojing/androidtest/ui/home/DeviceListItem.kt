package com.taojing.androidtest.ui.home

sealed class DeviceListItem {

    data class Header(val title: String) : DeviceListItem()

    data class Device(
        val devId: String,
        val name: String,
        val iconUrl: String,
        val isOnline: Boolean,
        val isShared: Boolean,
    ) : DeviceListItem()
}
