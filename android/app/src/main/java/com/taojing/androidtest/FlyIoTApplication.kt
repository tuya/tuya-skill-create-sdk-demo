package com.taojing.androidtest

import android.app.Application
import android.content.Context
import android.util.Log
import com.facebook.soloader.SoLoader
import com.taojing.androidtest.ui.auth.AuthActivity
import com.thingclips.smart.api.service.RouteEventListener
import com.thingclips.smart.api.service.ServiceEventListener
import com.thingclips.smart.bizbundle.initializer.BizBundleInitializer
import com.thingclips.smart.commonbiz.bizbundle.family.api.AbsBizBundleFamilyService
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.sdk.api.INeedLoginListener
import com.thingclips.smart.thingpackconfig.PackConfig

class FlyIoTApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        SoLoader.init(this, false)

        PackConfig.addValueDelegate(AppConfig::class.java)

        BizBundleInitializer.init(
            this,
            RouteEventListener { _, urlBuilder ->
                Log.e("FlyIoT/Router", "route not impl: ${urlBuilder.target}")
            },
            ServiceEventListener { serviceName ->
                Log.e("FlyIoT/Service", "service not impl: $serviceName")
            }
        )

        ThingHomeSdk.init(this)

        BizBundleInitializer.registerService(
            AbsBizBundleFamilyService::class.java,
            BizBundleFamilyServiceImpl()
        )

        CurrentHomeManager.init(this)

        if (BuildConfig.DEBUG) {
            ThingHomeSdk.setDebugMode(true)
        }

        ThingHomeSdk.setOnNeedLoginListener(object : INeedLoginListener {
            override fun onNeedLogin(context: Context?) {
                AuthActivity.startNewTask(context ?: this@FlyIoTApplication)
            }
        })
    }
}
