package com.example.viewdebugtrans

import com.example.viewdebugtrans.R.MakeRClass
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.ProjectManager
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
            ShowLogAction.builder.clear()
            val editor = e.getData(PlatformDataKeys.EDITOR)
            if (editor is EditorImpl) {
                var path = FileDocumentManager.getInstance().getFile(editor.document)?.path ?: return
                val makeRClass = MakeRClass()
                makeRClass.make(e, path)
                // 经过编译的产物路径
                path = CompileFileAndSend().compile(path, e)
                makeRClass.delete()

                val target = File(path)
                if (target.exists()) {
                    val pkgName = Config.getPackageName()
                    if (pkgName == null) {
                        DestInputAction().actionPerformed(e);
                    } else {
                        /*val target = File(projectPath + File.separator + Project.DIRECTORY_STORE_FOLDER + File.separator + "1")
                        File(path).copyTo(target, true)*/
                        val destFolder = Config.getTargetFileDestPath()
                        checkRemoteFolder(device, destFolder)
                        val result = pushFile("\"$path\"", destFolder + target.name, device)
                        show(e.project!!, result)
                        Messages.showDialog(e.project, "推送成功", "提示", arrayOf("确定"), 0, null)
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
        }

    }

    private fun execute(cmd: String): String {
        show(ProjectManager.getInstance().openProjects.getOrNull(0)!!, cmd)
        return String(Runtime.getRuntime().exec(cmd).inputStream.readBytes())
    }

    private fun pushFile(target: String, dest: String, device: String): String {
        Config.saveConfig(dest)
        // 先推送文件
        execute("adb -s $device push $target $dest")
        // 再推送config文件
        return execute("adb -s $device push \"${Config.getConfigFile().absolutePath}\" ${Config.getConfigRemotePath()}")
    }


    private fun checkRemoteFolder(device: String, folder: String) {
        execute("adb -s $device shell mkdir \"$folder\"")
    }
}