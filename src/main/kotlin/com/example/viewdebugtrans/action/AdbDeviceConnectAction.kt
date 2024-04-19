package com.example.viewdebugtrans.action

import com.example.viewdebugtrans.agreement.AdbDevicesManager
import com.example.viewdebugtrans.agreement.Device
import com.example.viewdebugtrans.util.getPackageName
import com.example.viewdebugtrans.util.showDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * 未配对action
 */
class AdbDeviceConnectAction(private val device: Device): AnAction("${device.serialNumber} - 未配对") {
    override fun actionPerformed(e: AnActionEvent) {
        try {
            /*// 删除连接
            execute(arrayOf("adb", "-s", device.serialNumber, "forward", "--remove", "tcp:${AdbServerRequest.remotePort}"))
            // 建立连接
            execute(arrayOf("adb", "-s", device.serialNumber, "forward", "tcp:${AdbServerRequest.remotePort}", "tcp:12349"))
            val result = AdbServerRequest.requestRemotePushAgreement()
            AdbDevicesManager.saveDeviceAgreement(device.serialNumber, result)
            System.out.println("连接结果：$result")*/
            val pkgName = e.project?.getPackageName()
            if (pkgName != null) {
                AdbDevicesManager.fetchRemoteAgreement(e.project!!, device, pkgName)
                if (device.getAgreement(pkgName) == null) {
                    showDialog(e.project, "当前设备不支持，请确保${pkgName}集成了调试插件0.20+", "失败", arrayOf("确定"), 0)
                } else {
                    showDialog(e.project, "配对成功", "成功", arrayOf("确定"), 0)
                }
            } else {
                showDialog(e.project, "未找到包名", "失败", arrayOf("确定"), 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}