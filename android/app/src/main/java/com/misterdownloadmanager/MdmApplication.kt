package com.misterdownloadmanager

import android.app.Application
import com.misterdownloadmanager.core.MdmNativeBridge
import java.io.File

class MdmApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val metaDir = File(filesDir, "mdm_state").apply { mkdirs() }.absolutePath
        MdmNativeBridge.initialize(context = this, persistenceDir = metaDir)
    }
}
