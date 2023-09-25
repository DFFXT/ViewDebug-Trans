package com.example.viewdebugtrans.util

import com.android.tools.idea.projectsystem.getProjectSystem
import com.example.viewdebugtrans.Config
import com.intellij.execution.RunManagerEx
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

object Utils {

    /**
     * 字符串转map
     */
    fun stringToMap(text: String, pairSeparator: String = "\n", valueSeparator: String = "="): Map<String, String> {
        val map = HashMap<String, String>()
        text.trim().split(pairSeparator).forEach {
            val index = it.indexOf(valueSeparator)
            if (index > 0) {
                map[it.substring(0, index)] = it.substring(index + valueSeparator.length)
            }
        }
        return map
    }

    /**
     * map转字符串
     */
    fun mapToString(map: Map<String, String>, pairSeparator: String = "\n", valueSeparator: String = "="): String {
        val builder = StringBuilder()
        map.forEach { t, u ->
            builder.append(t)
            builder.append(valueSeparator)
            builder.append(u)
            builder.append(pairSeparator)
        }
        return builder.toString().trim()
    }
}

fun Project.getPackageName(): String? {
    /*val ms = ModuleManager.getInstance(this).sortedModules
    if (ms.size >= 2) {
        return AndroidFacet.getInstance(ms[ms.size - 2])?.getPackageForApplication()
    }*/
    val r = RunManagerEx.getInstanceEx(this).selectedConfiguration?.configuration
    /*if (r is AndroidRunConfiguration) {
        r.appId
    }*/
    return this.getProjectSystem().getApplicationIdProvider(r!!)?.packageName
    // getProjectSystem().getModuleSystem(ms[1]).applicationRClassConstantIds
    //return null
}

fun Project.getViewDebugDir(): File {
    val file = File(this.basePath!! + File.separator + Project.DIRECTORY_STORE_FOLDER + File.separator + "viewDebug")
    if (!file.exists()) {
        file.mkdirs()
    }
    return file
}

fun AnActionEvent.getModule(): com.intellij.openapi.module.Module? {
    val project = this.project ?: return null
    val editor = this.getData(PlatformDataKeys.EDITOR) ?: return null
    val path = FileDocumentManager.getInstance().getFile(editor.document)?.path ?: return null
    val vf = LocalFileSystem.getInstance().findFileByIoFile(File(path)) ?: return null
    return ModuleUtil.findModuleForFile(vf, project)
}

fun showDialog(project: Project?, msg: String, title: String, options: Array<String>, defaultOption: Int) {

    ApplicationManager.getApplication().invokeLater {
        Messages.showDialog(project, msg, title, options, defaultOption, null)
    }
}

fun showTip(project: Project, msg: String) {
    ApplicationManager.getApplication().invokeLater {
        Messages.showDialog(project, msg, "提示", arrayOf("确认"), 0, null)
    }
}
