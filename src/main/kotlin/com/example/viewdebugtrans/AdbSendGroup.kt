package com.example.viewdebugtrans

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import java.util.*

/**
 * action group
 * 根据当前设备数量动态设置action按钮
 */
class AdbSendGroup : ActionGroup() {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        e?.project?.let {
            Config.updateProject(it)
        }
        val devices = getDevices()

        val actions = ArrayList<AnAction>()
        for (d in devices) {
            actions.add(AdbSendAction(d))
        }
        actions.add(0, DestRAction())
        actions.add(0, DestJavaAction())
        actions.add(0, DestDxAction())
        actions.add(0, DestADBAction())
        actions.add(0, DestInputAction())
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

    private fun getDevices(): List<String> {
        val result = execute("adb devices")
        val list = result.split("\n")
        val devices = LinkedList<String>()
        for (i in 1 until list.size) {
            val item = list[i].trim()
            if (item.endsWith("device")) {
                val index = item.indexOf("\t")
                devices.add(item.substring(0, index))
            }
        }
        return devices
    }
}