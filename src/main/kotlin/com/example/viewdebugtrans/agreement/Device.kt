package com.example.viewdebugtrans.agreement

import com.android.ddmlib.IDevice
import com.android.tools.idea.adb.AdbFileProvider
import com.android.tools.idea.concurrency.executeAsync
import com.android.tools.idea.explorer.adbimpl.AdbDeviceFileSystem
import com.example.viewdebugtrans.show
import com.intellij.util.concurrency.EdtExecutorService
import org.jetbrains.ide.PooledThreadExecutor

data class Device(
    val id: String,
    val serialNumber: String,
    var online: Boolean,
    val device: IDevice,
) {
    var agreement: ArrayList<AdbAgreement> = ArrayList()
    val fileSystem by lazy {
        show(null,"create")
        EdtExecutorService.getInstance()
        show(null,"create1")
        PooledThreadExecutor.INSTANCE
        show(null,"create2")
        var d: AdbDeviceFileSystem? = null
        d = EdtExecutorService.getInstance().executeAsync {
            AdbDeviceFileSystem(device, EdtExecutorService.getInstance(), PooledThreadExecutor.INSTANCE)
        }.get()
        show(null,"create end")
        d!!
    }
    fun addAgreement(agreement: AdbAgreement?) {
        agreement ?: return
        if (getAgreement(agreement.pkgName) == null) {
            this.agreement.add(agreement)
        }
    }

    fun getAgreement(pkgName: String?): AdbAgreement? = this.agreement.find { it.pkgName == pkgName }
}