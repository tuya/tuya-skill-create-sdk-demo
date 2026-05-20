package com.taojing.androidtest

import androidx.annotation.Keep

@Keep
class AppConfig {
    companion object {
        @JvmField
        val is_darkMode_support: Boolean = true

        @JvmField
        val is_need_ble_support: Boolean = true

        @JvmField
        val is_need_blemesh_support: Boolean = true

        @JvmField
        val is_scan_support: Boolean = true
    }
}
