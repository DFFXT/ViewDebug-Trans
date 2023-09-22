package com.example.viewdebugtrans.action

import com.example.viewdebugtrans.agreement.AdbAgreement
import com.example.viewdebugtrans.agreement.Device
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class DevicesMultiPkgAction(val device: Device, val agreements: List<AdbAgreement>): ActionGroup() {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return agreements.map { AdbSendAction(device, it) }.toTypedArray()
    }
}