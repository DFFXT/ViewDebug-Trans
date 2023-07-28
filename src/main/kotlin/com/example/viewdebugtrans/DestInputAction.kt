package com.example.viewdebugtrans

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.io.File

class ShowLogAction: AnAction("显示日志") {
    companion object {
        val builder = StringBuilder()
    }
    override fun actionPerformed(e: AnActionEvent) {
        var msg = builder.toString()
        if (msg.isEmpty()) {
            msg = "NULL"
        }
        Messages.showDialog(e.project, msg, "编译日志", arrayOf("确定"), 0, null)
        File(Config.getIdeaFolder(), "view-debug-log.txt").writeText(msg)
    }
}
class DestInputAction : AnAction("设置输出路径") {
    override fun actionPerformed(e: AnActionEvent) {
        val result = Messages.showInputDialog(e.project, null, "请输入externalCache包名路径", null, Config.getPackageName(), null)
        if (result != null) {
            Config.savePackage(result)
        }
    }
}

class DestDxAction : AnAction("设置dx路径") {
    override fun actionPerformed(e: AnActionEvent) {
        val result = Messages.showInputDialog(e.project, null, "请输入dx路径", null, Config.dxPath, null)
        if (result != null) {
            Config.dxPath = result
        }
    }
}

class DestJavaAction : AnAction("设置java1.8路径") {
    override fun actionPerformed(e: AnActionEvent) {
        val result = Messages.showInputDialog(e.project, null, "请输入java路径", null, Config.javaPath, null)
        if (result != null) {
            Config.javaPath = result
        }
    }
}

class DestRAction : AnAction("设置R文件相对路径") {
    override fun actionPerformed(e: AnActionEvent) {
        val result = Messages.showInputDialog(e.project, null, "请输入R文件路径", null, Config.RFilePath, null)
        if (result != null) {
            Config.RFilePath = result
        }
    }
}
fun show(project: Project?, text: String) {
    ShowLogAction.builder.append(text)
    ShowLogAction.builder.append("\n")
    ShowLogAction.builder.append("\n")
   //Messages.showInputDialog(project, null, "提示", null, text, null)
}