package com.example.viewdebugtrans.action

import com.example.viewdebugtrans.ProjectListener
import com.example.viewdebugtrans.agreement.AdbDevicesManager
import com.example.viewdebugtrans.agreement.Device
import com.example.viewdebugtrans.execute
import com.example.viewdebugtrans.show
import com.example.viewdebugtrans.socket.AdbServerRequest
import com.example.viewdebugtrans.util.getPackageName
import com.example.viewdebugtrans.util.showDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import kotlin.concurrent.thread

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
                val agreement = device.getAgreement(pkgName)
                if (agreement == null) {
                    showDialog(e.project, "当前设备不支持，请确保${pkgName}集成了调试插件0.20+", "失败", arrayOf("确定"), 0)
                } else {
                    showDialog(e.project, "配对成功", "成功", arrayOf("确定"), 0)
                    if (agreement.serverPort != null && agreement.clientPort != null) {
                        val adbServerRequest = ProjectListener.getProjectTable(e.project!!).adbServerRequest
                        adbServerRequest.create(pkgName, device.serialNumber, agreement.serverPort, agreement.clientPort)
                        thread {
                            adbServerRequest.send(pkgName, device.serialNumber, "test", "contet") {

                            }
                        }
                    } else {
                        show(null, "设备${device.serialNumber}端口异常：${agreement.serverPort} ${agreement.clientPort}")
                    }
                }
            } else {
                showDialog(e.project, "未找到包名", "失败", arrayOf("确定"), 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}