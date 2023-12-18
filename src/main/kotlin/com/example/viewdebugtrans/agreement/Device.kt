package com.example.viewdebugtrans.agreement

import com.example.viewdebugtrans.execute
import com.example.viewdebugtrans.util.getPackageName
import com.intellij.openapi.project.Project

data class Device(
    // id == serialNumber
    val id: String,
    val serialNumber: String,
    val online: Boolean
) {
    private var isSuRoot: Boolean? = null
    var agreement: ArrayList<AdbAgreement> = ArrayList()

    fun addAgreement(agreement: AdbAgreement?) {
        agreement ?: return
        if (getAgreement(agreement.pkgName) == null) {
            this.agreement.add(agreement)
        }
    }

    fun isSuRoot(): Boolean {
        if (isSuRoot != null) {
            return isSuRoot!!
        }
        val result = execute(arrayOf(AdbDevicesManager.getAnyAdbPath()!!, "-s", serialNumber, "shell", "su", "0", "sh", "-c", "id"))
        isSuRoot = !result.error
        return isSuRoot!!
    }

    fun getAgreement(pkgName: String?): AdbAgreement? = this.agreement.find { it.pkgName == pkgName }

    /**
     * 是否配对
     */
    fun isAgreementConnected(project: Project): Boolean {
        return getAgreement(project.getPackageName()) != null
    }
}