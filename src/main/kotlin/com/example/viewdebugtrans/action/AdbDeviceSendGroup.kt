package com.example.viewdebugtrans.action

import com.example.viewdebugtrans.agreement.AdbAgreement
import com.example.viewdebugtrans.agreement.Device
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * 选中设备后，弹出重启和不重启
 */
class AdbDeviceSendGroup(private val device: Device, private val agreement: AdbAgreement) : ActionGroup(device.run {
    if (this.online) {
        this.serialNumber + " - " + agreement.pkgName
    } else {
        this.serialNumber + " - " + "offline"
    }
}, true) {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return arrayOf(
            AdbSendAction(device, agreement, false),
            AdbSendAction(device, agreement, true)
        )
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabled = device.online
    }

}