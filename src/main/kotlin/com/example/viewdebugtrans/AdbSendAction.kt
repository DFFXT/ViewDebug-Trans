package com.example.viewdebugtrans

import com.example.viewdebugtrans.R.MakeRClass
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import java.io.File
import java.util.LinkedList


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
                if (path.endsWith(".java") || path.endsWith(".kt")) {
                    // 代码文件，需要编译和R文件
                    val makeRClass = MakeRClass()
                    makeRClass.make(e, path)
                    // 经过编译的产物路径
                    path = CompileFileAndSend().compile(path, e)
                    makeRClass.delete()
                } else if (path.endsWith(".xml")) {
                    XmlRulesSend().send(project)
                }

                val target = File(path)
                if (target.exists()) {
                    val pkgName = Config.getPackageName()
                    if (pkgName == null) {
                        DestInputAction().actionPerformed(e);
                    } else {
                        /*val target = File(projectPath + File.separator + Project.DIRECTORY_STORE_FOLDER + File.separator + "1")
                        File(path).copyTo(target, true)*/
                        val destFolder = Config.getTargetFileDestPath()
                        PushFileManager.checkRemoteFolder(device, destFolder)
                        val result = PushFileManager.pushFile("\"$path\"", destFolder + target.name, device)
                        show(e.project!!, result)
                        Messages.showDialog(e.project, "推送成功", "提示", arrayOf("确定"), 0, null)
                    }
                    PushFileManager.pushApply()

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

}