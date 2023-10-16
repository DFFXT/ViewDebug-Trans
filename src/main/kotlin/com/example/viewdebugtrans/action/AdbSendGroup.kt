package com.example.viewdebugtrans.action

import com.example.viewdebugtrans.Config
import com.example.viewdebugtrans.ShowLogAction
import com.example.viewdebugtrans.agreement.AdbDevicesManager
import com.example.viewdebugtrans.agreement.Device
import com.example.viewdebugtrans.util.getPackageName
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * action group
 * 根据当前设备数量动态设置action按钮
 */
class AdbSendGroup : ActionGroup() {
    companion object {
        var currentDevices: List<Device>? = null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return super.getActionUpdateThread()
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        e?.project?.let {
            Config.updateProject(it)
        }
        val devices = AdbDevicesManager.getDevices()
        currentDevices = devices

        val actions = ArrayList<AnAction>()
        for (d in devices) {
            if (d.online) {
                val agreement = d.getAgreement(e?.project?.getPackageName())
                if (agreement != null) {
                    actions.add(AdbDeviceSendGroup(d, agreement))
                } else {
                    actions.add(AdbDeviceConnectAction(d))
                }
            } else {
                actions.add(AdbDeviceOfflineAction(d))
            }

        }
        // actions.add(0, DeviceConnect())
        // actions.add(0, DestJavaAction())
        //actions.add(0, DestDxAction())
        // actions.add(0, DestADBAction())
        // actions.add(0, DestInputAction())
        actions.add(0, ShowLogAction())
        return actions.toTypedArray()
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
        e.presentation.isEnabled = project != null
    }

    /*    private fun execute(cmd: String): String {
            var changedCmd = cmd
            val adbPath = Config.adbPath
            if (cmd.startsWith("adb") && !adbPath.isNullOrEmpty()) {
                changedCmd = "$adbPath/adb.exe"
            }
            return String(Runtime.getRuntime().exec(changedCmd).inputStream.readBytes())
        }*/

}