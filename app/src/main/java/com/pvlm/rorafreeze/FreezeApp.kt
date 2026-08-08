package com.pvlm.rorafreeze

import android.app.Application
import com.pvlm.rorafreeze.utils.HapticUtil
import com.pvlm.rorafreeze.utils.ShizukuUtils

class FreezeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        context = this
        ShizukuUtils.initialize()
        HapticUtil.initialize(this)
    }

    companion object {
        lateinit var context: FreezeApp
            private set
    }
}