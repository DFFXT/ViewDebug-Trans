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
    private val sendAction = LinkedList<FileItem>()
    override fun actionPerformed(e: AnActionEvent) {
        try {
            sendAction.clear()
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
                    val rulePathsSet = HashSet<String>()
                    val logSet = LinkedHashSet<String>()
                    ModuleManager.getInstance(project).modules.forEachIndexed { index, it ->
                        //val path = CompilerModuleExtension.getInstance(it)?.compilerOutputPath?.path
                        val basePath = CompilerModuleExtension.getInstance(it)?.compilerOutputPath?.path?.replace('\\','/')
                        if (basePath != null) {
                            val index = basePath.indexOf("/build/")
                            if (index > 0) {
                                // xml文件，需要xml规则文件
                                val rulesPath =  basePath.substring(0, index) + "/build/intermediates/incremental"
                                val ruleFileDir = File(rulesPath)
                                if (ruleFileDir.exists()) {
                                    // 选择对应merge文件夹，过滤AndroidTestResources类型文件夹
                                    val folder = ruleFileDir.listFiles()?.find { it.name.startsWith("merge") && it.name.endsWith("Resources") && !it.name.endsWith("AndroidTestResources") }
                                    if (folder != null) {
                                        // 需要设置不同的名称
                                        val ruleFile = File(folder, "merger.xml")
                                        if (ruleFile.exists()) {
                                            // 过滤相同文件
                                            if (!rulePathsSet.contains(ruleFile.absolutePath)) {
                                                rulePathsSet.add(ruleFile.absolutePath)
                                                // 推送规则文件
                                                show(project, "找到规则文件-----：$ruleFile")
                                                pushFile(ruleFile.absolutePath, Config.getTargetFileDestPath() + "merger-${index}.xml", device, "rules")
                                            }
                                        } else {
                                            logSet.add("没有规则文件：$ruleFile")
                                        }
                                    } else {
                                        logSet.add("没有规则文件：$rulesPath")
                                    }
                                }
                            }

                        }
                    }
                    logSet.forEach {
                        show(project, it)
                    }

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
                        checkRemoteFolder(device, destFolder)
                        val result = pushFile("\"$path\"", destFolder + target.name, device)
                        show(e.project!!, result)
                        Messages.showDialog(e.project, "推送成功", "提示", arrayOf("确定"), 0, null)
                    }
                    pushApply()

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

    private fun pushFile(target: String, dest: String, device: String, type: String = "file"): String {
        // Config.saveConfig(dest, type)
        addFileItem(dest, type)
        // 先推送文件
        return execute("adb -s $device push $target $dest")
        // 再推送config文件

        // return execute("adb -s $device push \"${Config.getConfigFile().absolutePath}\" ${Config.getConfigRemotePath()}")
    }


    private fun checkRemoteFolder(device: String, folder: String) {
        execute("adb -s $device shell mkdir \"$folder\"")
    }

    /**
     * 推送配置文件
     */
    private fun pushApply() {
        if (sendAction.isNotEmpty()) {
            Config.saveConfig(sendAction)
            execute("adb -s $device push \"${Config.getConfigFile().absolutePath}\" ${Config.getConfigRemotePath()}")
        }
    }


    fun addFileItem(path: String, type: String) {
        sendAction.add(FileItem(path, type))
    }
    class FileItem(val path: String, val type: String)
}