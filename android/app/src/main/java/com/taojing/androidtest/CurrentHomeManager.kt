package com.taojing.androidtest

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.bean.HomeBean
import com.thingclips.smart.home.sdk.callback.IThingGetHomeListCallback
import com.thingclips.smart.home.sdk.callback.IThingHomeResultCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CurrentHomeManager {

    private const val TAG = "CurrentHomeManager"
    private const val PREFS_NAME = "current_home_prefs"
    private const val KEY_PREFIX = "current_home_id_"

    private lateinit var prefs: SharedPreferences

    private val _currentHomeIdFlow = MutableStateFlow<Long?>(null)
    val currentHomeIdFlow: StateFlow<Long?> = _currentHomeIdFlow.asStateFlow()

    private val _currentHomeNameFlow = MutableStateFlow<String?>(null)
    val currentHomeNameFlow: StateFlow<String?> = _currentHomeNameFlow.asStateFlow()

    val currentHomeId: Long? get() = _currentHomeIdFlow.value

    private val _homeListFlow = MutableStateFlow<List<HomeBean>>(emptyList())
    val homeListFlow: StateFlow<List<HomeBean>> = _homeListFlow.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun initialize(
        onReady: ((Long, String) -> Unit)? = null,
        onEmpty: (() -> Unit)? = null,
    ) {
        ThingHomeSdk.getHomeManagerInstance().queryHomeList(object : IThingGetHomeListCallback {
            override fun onSuccess(homeBeans: List<HomeBean>) {
                _homeListFlow.value = homeBeans
                if (homeBeans.isEmpty()) {
                    Log.w(TAG, "Home list is empty")
                    _currentHomeIdFlow.value = null
                    _currentHomeNameFlow.value = null
                    onEmpty?.invoke()
                    return
                }

                val savedId = loadPersistedHomeId()
                val target = homeBeans.firstOrNull { it.homeId == savedId } ?: homeBeans[0]
                applySwitchHome(target.homeId, target.name)
                warmUpHomeDetail(target.homeId)
                onReady?.invoke(target.homeId, target.name)
            }

            override fun onError(errorCode: String, error: String) {
                Log.e(TAG, "queryHomeList failed: $errorCode $error")
            }
        })
    }

    fun switchHome(homeId: Long, homeName: String? = null) {
        val name = homeName ?: _homeListFlow.value.firstOrNull { it.homeId == homeId }?.name.orEmpty()
        applySwitchHome(homeId, name)
        warmUpHomeDetail(homeId)
    }

    fun refreshHomeList(onDone: ((List<HomeBean>) -> Unit)? = null) {
        ThingHomeSdk.getHomeManagerInstance().queryHomeList(object : IThingGetHomeListCallback {
            override fun onSuccess(homeBeans: List<HomeBean>) {
                _homeListFlow.value = homeBeans
                onDone?.invoke(homeBeans)
            }

            override fun onError(errorCode: String, error: String) {
                Log.e(TAG, "refreshHomeList failed: $errorCode $error")
                onDone?.invoke(_homeListFlow.value)
            }
        })
    }

    fun clear() {
        _currentHomeIdFlow.value = null
        _currentHomeNameFlow.value = null
        _homeListFlow.value = emptyList()
    }

    private fun applySwitchHome(homeId: Long, homeName: String) {
        _currentHomeIdFlow.value = homeId
        _currentHomeNameFlow.value = homeName
        persistHomeId(homeId)
        Log.d(TAG, "Switched to home: $homeId ($homeName)")
    }

    private fun warmUpHomeDetail(homeId: Long) {
        ThingHomeSdk.newHomeInstance(homeId).getHomeDetail(object : IThingHomeResultCallback {
            override fun onSuccess(bean: HomeBean) {
                Log.d(TAG, "Home detail cached for $homeId")
            }

            override fun onError(errorCode: String, errorMsg: String) {
                Log.e(TAG, "getHomeDetail failed: $errorCode $errorMsg")
            }
        })
    }

    private fun persistHomeId(homeId: Long) {
        val uid = ThingHomeSdk.getUserInstance()?.user?.uid ?: return
        prefs.edit().putLong("$KEY_PREFIX$uid", homeId).apply()
    }

    private fun loadPersistedHomeId(): Long? {
        val uid = ThingHomeSdk.getUserInstance()?.user?.uid ?: return null
        val key = "$KEY_PREFIX$uid"
        return if (prefs.contains(key)) prefs.getLong(key, 0L) else null
    }
}
