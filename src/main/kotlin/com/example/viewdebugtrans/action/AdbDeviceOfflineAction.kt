package com.example.viewdebugtrans.action

import com.example.viewdebugtrans.agreement.Device
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * 设备离线
 */
class AdbDeviceOfflineAction(device: Device): AnAction(device.serialNumber + " - offline") {
    override fun actionPerformed(e: AnActionEvent) {

    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabled = false
    }
}