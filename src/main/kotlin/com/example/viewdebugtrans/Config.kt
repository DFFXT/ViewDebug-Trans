package com.example.viewdebugtrans

import com.google.gson.JsonObject
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import java.io.File

object Config {
    private var pkgName: String? = null
    private val projectPath: String
        get() {
            return ProjectManager.getInstance().openProjects.getOrNull(0)?.basePath ?: ""
        }

    fun savePackage(pkgName: String) {
        val f = File(getIdeaFolder(), "view-debug")
        println(f.absolutePath)
        f.writeBytes(pkgName.toByteArray())
    }

    fun getPackageName(): String? {
        if (pkgName == null) {
            val f = File(getIdeaFolder(), "view-debug")
            if (f.exists()) {
                pkgName = String(f.readBytes())

            }
        }
        return pkgName
    }

    /**
     * 输出配置文件，用于文件推送
     */
    fun saveConfig(path: String) {
        val config = getConfigFile()
        val json = JsonObject()
        json.addProperty("file", path)
        config.writeBytes(json.toString().toByteArray())
    }

    fun getConfigFile(): File {
        return File(getIdeaFolder(), "view-debug-config.json")
    }

    fun getConfigRemotePath(): String {
        return "sdcard/Android/data/$pkgName/cache/view-debug/view-debug-config.json"
    }

    fun getTargetFileDestPath(): String {
        return "sdcard/Android/data/$pkgName/cache/view-debug/receive/"
    }

    fun getIdeaFolder() : String{
        return projectPath + File.separator + Project.DIRECTORY_STORE_FOLDER
    }
}