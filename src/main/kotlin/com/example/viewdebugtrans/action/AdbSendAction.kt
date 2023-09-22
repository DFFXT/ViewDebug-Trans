package com.example.viewdebugtrans.action

import com.example.viewdebugtrans.*
import com.example.viewdebugtrans.R.MakeRClass
import com.example.viewdebugtrans.agreement.AdbAgreement
import com.example.viewdebugtrans.agreement.AdbDevicesManager
import com.example.viewdebugtrans.agreement.Device
import com.example.viewdebugtrans.util.showDialog
import com.example.viewdebugtrans.util.showTip
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File


/**
 * val window = ToolWindowManager.getInstance(e.project!!).getToolWindow("Kotlin Bytecode")!!
val KtFile = e.getData(CommonDataKeys.PSI_FILE)!!
/*KtFile::class.java.classLoader.loadClass("org.jetbrains.kotlin.idea.internal.KotlinBytecodeToolWindow\$Companion").getMethod("getBytecodeForFile", KtFile::class.java, CompilerConfiguration).invoke(null, KtFile, CompilerConfiguration.newInstance())*/

val windo = KtFile::class.java.classLoader.loadClass("org.jetbrains.kotlin.idea.internal.KotlinBytecodeToolWindow")
val wi = windo.constructors[0].newInstance(e.project!!, window)
val task = KtFile::class.java.classLoader.loadClass("org.jetbrains.kotlin.idea.internal.KotlinBytecodeToolWindow\$UpdateBytecodeToolWindowTask")
val c = task.constructors[0]
c.isAccessible = true
val tasktI = c.newInstance(wi)

task.methods.find { it.name == "processRequest" }
val processRequestF = task.getMethod("processRequest", Any::class.java)
processRequestF.isAccessible = true

val locationClass = KtFile::class.java.classLoader.loadClass("org.jetbrains.kotlin.idea.internal.Location")
val locationConstactor = locationClass.declaredConstructors[0]
locationConstactor.isAccessible = true

val location = locationConstactor.newInstance(e.getData(PlatformDataKeys.EDITOR), e.project!!)

processRequestF.invoke(tasktI, location)

//UpdateBytecodeToolWindowTask

//window.javaClass.classLoader.loadClass("org.jetbrains.kotlin.idea.internal.KotlinBytecodeToolWindow")
//getBytecodeForFile
 */

class AdbSendAction(private val device: Device, private val agreement: AdbAgreement) : AnAction(device.serialNumber +" - "+ agreement.pkgName) {
    override fun actionPerformed(e: AnActionEvent) {
        try {
            ShowLogAction.builder.clear()
            val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
            val project = e.project ?: return
            var path = FileDocumentManager.getInstance().getFile(editor.document)?.path ?: return
            val originPath = path
            var fileType: String = getFileType(originPath)
            val adbP = AdbDevicesManager.getAdbPath(project)
            if ( adbP== null) {
                showTip(project, "没有adb可用")
                return
            }
            PushFileManager.init(device, agreement, adbP)

            if (path.endsWith(".java") || path.endsWith(".kt")) {
                // 代码文件，需要编译和R文件
                val vf = LocalFileSystem.getInstance().findFileByIoFile(File(path)) ?: return showTip(project, "文件不是真实文件")
                val module = ModuleUtil.findModuleForFile(vf, project) ?: return showTip(project, "文件不属于任何模块")
                val makeRClass = MakeRClass()
                val p = path
                makeRClass.make(module, path) {
                    // 经过编译的产物路径
                    path = CompileFileAndSend(module).compile(p, e)

                    makeRClass.delete()
                    if (path.endsWith(".dex")) {
                        fileType = PushFileManager.TYPE_DEX
                    }
                    send(path, fileType, originPath, e)
                }

            } else if (path.endsWith(".xml") && fileType == PushFileManager.TYPE_LAYOUT) {
                XmlRulesSend().send(project,agreement)
                send(path, fileType, originPath, e)
            }
        } catch (exception: Exception) {
            show(
                project = e.project!!, exception.message + "\n" +
                        exception.stackTraceToString()
            )
        }

    }
    
    private fun send(path: String, fileType: String, originPath: String, e: AnActionEvent) {
        val target = File(path)
        if (target.exists()) {
            val destFolder = agreement.destDir
            // PushFileManager.checkRemoteFolder(device, destFolder)
            PushFileManager.pushFile(path, destFolder + "/" + target.name, fileType, originPath)
            PushFileManager.pushApply()
            showDialog(e.project!!, "推送成功", "提示", arrayOf("确定"), 0)
            if (fileType == PushFileManager.TYPE_DEX) {
                val dest = File(target.parent, "view-debug-delete.dex")
                if (dest.exists()) {
                    // 删除原产物
                    dest.delete()
                }
                // 重命名产物文件
                val renameResult = target.renameTo(dest)
                show(null, "last rename $renameResult")
            }
            PushFileManager.reset()
            //showDialog(editor.component)
        } else {
            showDialog(e.project!!, "推送失败，产物文件不存在: $path", "提示", arrayOf("确定"), 0)
            show(e.project!!, "不存在$path")
        }
    }




    private fun getFileType(path: String): String {
        val file = File(path)
        val parent = file.parent
        if (parent.contains(PushFileManager.TYPE_DRAWABLE)) {
            return PushFileManager.TYPE_DRAWABLE
        }
        if (parent.contains(PushFileManager.TYPE_LAYOUT)) {
            return PushFileManager.TYPE_LAYOUT
        }
        if (parent.contains(PushFileManager.TYPE_ANIM)) {
            return PushFileManager.TYPE_ANIM
        }
        if (parent.contains(PushFileManager.TYPE_COLOR)) {
            return PushFileManager.TYPE_COLOR
        }
        return PushFileManager.TYPE_FILE
    }

}