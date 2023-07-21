package com.example.viewdebugtrans

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class DestInputAction : AnAction("设置输出路径") {
    override fun actionPerformed(e: AnActionEvent) {
        val result = Messages.showInputDialog(e.project, null, "请输入包名", null, Config.getPackageName(), null)
        if (result != null) {
            Config.savePackage(result)
        }
    }
}

class DestDxAction : AnAction("设置dx路径") {
    override fun actionPerformed(e: AnActionEvent) {
        val result = Messages.showInputDialog(e.project, null, "请输入dx路径", null, Config.getPackageName(), null)
        if (result != null) {
            Config.savePackage(result)
        }
    }
}

class DestJavaAction : AnAction("设置java1.8路径") {
    override fun actionPerformed(e: AnActionEvent) {
        val result = Messages.showInputDialog(e.project, null, "请输入java路径", null, Config.getPackageName(), null)
        if (result != null) {
            Config.savePackage(result)
        }
    }
}