package com.taojing.androidtest

import com.thingclips.smart.commonbiz.bizbundle.family.api.AbsBizBundleFamilyService

class BizBundleFamilyServiceImpl : AbsBizBundleFamilyService() {

    override fun getCurrentHomeId(): Long = CurrentHomeManager.currentHomeId ?: 0L

    override fun shiftCurrentFamily(familyId: Long, curName: String) {
        super.shiftCurrentFamily(familyId, curName)
        CurrentHomeManager.switchHome(familyId, curName)
    }
}
