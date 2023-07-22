package com.example.viewdebugtrans

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiFile
import org.codehaus.groovy.control.CompilerConfiguration
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
        val editor = e.getData(PlatformDataKeys.EDITOR)
        if (editor is EditorImpl) {
            var path = FileDocumentManager.getInstance().getFile(editor.document)?.path ?: return
            // 经过编译的产物路径
           path = CompileFileAndSend().compile(path, e)
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
                    pushFile("\"$path\"", destFolder + target.name, device)
                }

                //showDialog(editor.component)
            } else {
                show(e.project!!, "不存在$path")
            }
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

    private fun com(e: AnActionEvent) {

        val window = ToolWindowManager.getInstance(e.project!!).getToolWindow("Kotlin Bytecode")

        val KtFile = e.getData(CommonDataKeys.PSI_FILE)
        val KotlinCompilerIde = KtFile!!.javaClass.classLoader.loadClass("org.jetbrains.kotlin.idea.core.KotlinCompilerIde")
        val CompilerConfiguration = KtFile!!.javaClass.classLoader.loadClass("org.jetbrains.kotlin.config.CompilerConfiguration")
        val ClassBuilderFactory = KtFile!!.javaClass.classLoader.loadClass("org.jetbrains.kotlin.codegen.ClassBuilderFactory")
        val Function1 = KtFile!!.javaClass.classLoader.loadClass("kotlin.jvm.functions.Function1")
        val constructor = KotlinCompilerIde.getConstructor(KtFile::class.java, CompilerConfiguration, ClassBuilderFactory, Function1, Boolean::class.java )
        val com = KtFile!!.javaClass.classLoader.loadClass("org.jetbrains.kotlin.idea.core.KotlinCompilerIde\$Companion")
        com.declaredMethods
        val getDefaultCompilerConfiguration = com.getDeclaredMethod("getDefaultCompilerConfiguration", KtFile::class.java)
        getDefaultCompilerConfiguration.isAccessible = true

//val c = getDefaultCompilerConfiguration.invoke(null, KtFile)

        val ClassBuilderFactories = KtFile!!.javaClass.classLoader.loadClass("org.jetbrains.kotlin.codegen.ClassBuilderFactories")
        val BINARIES = ClassBuilderFactories.getDeclaredField("BINARIES").get(null)
        val resolutionFacadeProvider = object : kotlin.jvm.functions.Function1<Any,Any?> {
            override fun invoke(p1: Any): Any? {
                //val f =  KtFile::class.java.getDeclaredField("languageVersionSettings")
                return null
            }
        }

        val  ccc= constructor.newInstance(KtFile, CompilerConfiguration.newInstance(), BINARIES, resolutionFacadeProvider, false)

/*val ficompileToDirectoryFiled = KotlinCompilerIde.getDeclaredMethod("compileToDirectory", File::class.java)
ficompileToDirectoryFiled.invoke(ccc, File(Config.getIdeaFolder()))*/

        KotlinCompilerIde.getDeclaredMethod("compile").invoke(ccc)



    }
}