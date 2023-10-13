package com.example.viewdebugtrans.interceptor

import com.example.viewdebugtrans.PushFileManager
import com.example.viewdebugtrans.action.AdbSendAction
import com.example.viewdebugtrans.agreement.AdbAgreement
import com.example.viewdebugtrans.agreement.Device
import com.example.viewdebugtrans.show
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import java.io.File

/**
 * kotlin 文件推送后的逻辑，删除产物文件
 */
class KotlinAfterSend : IAfterSend {
    override fun afterSend(
        project: Project,
        e: AnActionEvent,
        fileInfo: AdbSendAction.FileInfo,
        device: Device,
        agreement: AdbAgreement
    ) {
        val target = File(fileInfo.path)
        if (target.exists() && fileInfo.type == PushFileManager.TYPE_DEX) {
            val dest = File(target.parent, "view-debug-delete.dex")
            if (dest.exists()) {
                // 删除原产物
                dest.delete()
            }
            // 重命名产物文件
            val renameResult = target.renameTo(dest)
            show(null, "last rename $renameResult")
        }
    }
}