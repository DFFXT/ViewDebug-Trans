package com.example.viewdebugtrans

import com.example.viewdebugtrans.R.MakeRClass
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.Messages
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

class AdbSendAction(private val device: String) : AnAction(device) {
    override fun actionPerformed(e: AnActionEvent) {
        try {
            PushFileManager.init(device)
            ShowLogAction.builder.clear()
            val editor = e.getData(PlatformDataKeys.EDITOR)
            val project = e.project ?: return
            if (editor is EditorImpl) {
                var path = FileDocumentManager.getInstance().getFile(editor.document)?.path ?: return
                val originPath = path
                var fileType: String = getFileType(originPath)
                if (path.endsWith(".java") || path.endsWith(".kt")) {
                    // 代码文件，需要编译和R文件
                    val makeRClass = MakeRClass()
                    makeRClass.make(e, path)
                    // 经过编译的产物路径
                    path = CompileFileAndSend(project).compile(path, e)
                    makeRClass.delete()
                    if (path.endsWith(".dex")) {
                        fileType = PushFileManager.TYPE_DEX
                    }
                } else if (path.endsWith(".xml") && fileType == PushFileManager.TYPE_LAYOUT) {
                    XmlRulesSend().send(project)
                }

                val target = File(path)
                if (target.exists()) {
                    val pkgName = Config.getPackageName()
                    if (pkgName == null) {
                        DestInputAction().actionPerformed(e);
                    } else {
                        val destFolder = Config.getTargetFileDestPath()
                        PushFileManager.checkRemoteFolder(device, destFolder)
                        PushFileManager.pushFile(path, destFolder + target.name, fileType)
                        Messages.showDialog(e.project, "推送成功", "提示", arrayOf("确定"), 0, null)
                    }
                    PushFileManager.pushApply()
                    if (fileType == PushFileManager.TYPE_DEX) {
                        // 重命名产物文件
                        target.renameTo(File(target.parent, "view-debug-delete.dex"))
                    }

                    //showDialog(editor.component)
                } else {
                    Messages.showDialog(e.project, "推送失败，产物文件不存在: $path", "提示", arrayOf("确定"), 0, null)
                    show(e.project!!, "不存在$path")
                }
            }
        } catch (exception: Exception) {
            show(
                project = e.project!!, exception.message + "\n" +
                        exception.stackTraceToString()
            )
        }finally {
            PushFileManager.reset()
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