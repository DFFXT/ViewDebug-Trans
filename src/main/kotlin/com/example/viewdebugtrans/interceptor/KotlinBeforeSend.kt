package com.example.viewdebugtrans.interceptor

import com.example.viewdebugtrans.CompileFileAndSend
import com.example.viewdebugtrans.PushFileManager
import com.example.viewdebugtrans.R.MakeRClass
import com.example.viewdebugtrans.action.AdbSendAction
import com.example.viewdebugtrans.agreement.AdbAgreement
import com.example.viewdebugtrans.agreement.Device
import com.example.viewdebugtrans.util.showTip
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

/**
 * kotlin 代码发生前期工作
 */
class KotlinBeforeSend : IBeforeSend {
    override fun beforeSend(
        project: Project,
        e: AnActionEvent,
        fileInfo: AdbSendAction.FileInfo,
        device: Device,
        agreement: AdbAgreement
    ) {
        val originPath = fileInfo.originPath
        if (originPath.endsWith(".java") || originPath.endsWith(".kt")) {
            // 代码文件，需要编译和R文件
            val vf = LocalFileSystem.getInstance().findFileByIoFile(File(originPath)) ?: return showTip(
                project,
                "文件不是真实文件"
            )
            val module = ModuleUtil.findModuleForFile(vf, project) ?: return showTip(project, "文件不属于任何模块")
            val makeRClass = MakeRClass()

            makeRClass.make(module, originPath) {
                // 经过编译的产物路径
                fileInfo.path = CompileFileAndSend(module).compile(fileInfo, e)

                makeRClass.delete()
                if (fileInfo.path.endsWith(".dex")) {
                    fileInfo.type = PushFileManager.TYPE_DEX
                }
                // send(fileInfo, e)
            }


        }
    }
}