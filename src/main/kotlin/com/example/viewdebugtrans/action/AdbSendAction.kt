package com.example.viewdebugtrans.action

import com.example.viewdebugtrans.agreement.AdbAgreement
import com.example.viewdebugtrans.agreement.Device
import com.example.viewdebugtrans.util.showTip
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager


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


open class AdbSendAction(
    protected val device: Device,
    private val agreement: AdbAgreement,
    private val reboot: Boolean
) : AnAction(
    if (reboot) "推送并重启应用" else "推送"
) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        if (reboot) {
            try {
                if (!agreement.versionSupport("0.20.5")) {
                    showTip(project, "客户端插件版本过低，需0.20.5及以上，当前版本${agreement.version}")
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showTip(project, e.message ?: "版本异常${agreement.version}")
                return
            }
        }
        // 保存当前内容到磁盘，否则有可能推送的不是最新内容，文件内容变更后会不定时的刷新到磁盘，刷新前推送就会导致内容不是最新
        FileDocumentManager.getInstance().saveDocument(editor.document)
        val path = e.getData(PlatformDataKeys.VIRTUAL_FILE)?.path ?: return
        PushManager(device, agreement, reboot).actionPerformed(project, path)
    }

}