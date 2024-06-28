package com.example.viewdebugtrans

import com.intellij.ide.actions.OpenFileAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.testFramework.LightVirtualFile

class ShowLogAction: AnAction("显示日志") {
    companion object {
        private val builder = StringBuffer()
        fun clear() {
            builder.delete(0, builder.length)
        }
        fun append(text: String) {
            builder.append(text)
        }
    }
    override fun actionPerformed(e: AnActionEvent) {
        //var msg = builder.toString()
        //Messages.showDialog(e.project, msg, "编译日志", arrayOf("确定"), 0, null)
        ApplicationManager.getApplication().runWriteAction {
            val p = e.project
            val  vf = object: LightVirtualFile("view-Trans-plug-log", builder.toString()) {
                override fun isWritable(): Boolean {
                    return false
                }
            }
            if (p != null) {
                ApplicationManager.getApplication().invokeLater {
                    OpenFileAction.openFile(vf, p)
                }
            }
        }

        // File(e.project?.getViewDebugDir(), "view-debug-log.txt").writeText(msg)
    }
}
/*class DestInputAction : AnAction("设置输出路径") {
    override fun actionPerformed(e: AnActionEvent) {
        val result = Messages.showInputDialog(e.project, null, "请输入externalCache包名路径", null, Config.getPackageName(), null)
        if (result != null) {
            Config.savePackage(result)
        }
    }
}*/

class DestADBAction : AnAction("设置adb文件路径") {
    override fun actionPerformed(e: AnActionEvent) {
        val result = Messages.showInputDialog(e.project, null, "请输入adb路径", null, Config.adbPath, null)
        if (result != null) {
            Config.adbPath = result
        }
    }
}

/*class DestDxAction : AnAction("设置dx或者d8路径") {
    override fun actionPerformed(e: AnActionEvent) {
        val result = Messages.showInputDialog(e.project, null, "请输入dx或者d8路径", null, Config.dxPath, null)
        if (result != null) {
            Config.dxPath = result
        }
    }
}*/

/*class DestJavaAction : AnAction("设置java1.8路径") {
    override fun actionPerformed(e: AnActionEvent) {
        val result = Messages.showInputDialog(e.project, null, "请输入java路径", null, Config.javaPath, null)
        if (result != null) {
            Config.javaPath = result
        }
    }
}*/

/*class DeviceConnect : AnAction("设备对接") {
    override fun actionPerformed(e: AnActionEvent) {
        AdbSendGroup.currentDevices?.forEach {
            try {
                // 删除连接
                execute(arrayOf("adb", "-s", it, "forward", "--remove", "tcp:${AdbServerRequest.remotePort}"))
                // 建立连接
                execute(arrayOf("adb", "-s", it, "forward", "tcp:${AdbServerRequest.remotePort}", "tcp:12349"))
                val result = AdbServerRequest.requestRemotePushAgreement()
                AdbDevicesManager.saveDeviceAgreement(it, result)
                System.out.println("连接结果：$result")
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        *//*val result = Messages.showInputDialog(e.project, null, "请输入R文件路径", null, Config.RFilePath ?: "例如：HMI\\AppShell\\build\\intermediates\\runtime_symbol_list\\debug\\R.txt", object : InputValidator {
            override fun checkInput(inputString: String?): Boolean {
                val project = e.project ?: return false
                val rPath = File("${project.basePath}/$inputString")
                return rPath.exists() && rPath.isFile
            }

            override fun canClose(inputString: String?): Boolean {
                return true
            }
        })
        if (result != null) {
            Config.RFilePath = result
        }*//*
    }
}*/
fun show(project: Project? = null, text: String) {
    ShowLogAction.append(text)
    ShowLogAction.append("\n")
    ShowLogAction.append("\n")
   //Messages.showInputDialog(project, null, "提示", null, text, null)
}

fun show(t: Throwable) {
    show(null, t.message + "\n" +
            t.stackTraceToString())
}