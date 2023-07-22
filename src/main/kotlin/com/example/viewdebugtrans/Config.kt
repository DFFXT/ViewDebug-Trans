package com.example.viewdebugtrans

import com.google.gson.JsonObject
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.PropertiesUtil
import java.io.File
import java.util.Properties

object Config {
    private var pkgName: String? = null
    private val projectPath: String
        get() {
            return ProjectManager.getInstance().openProjects.getOrNull(0)?.basePath ?: ""
        }

    var dxPath: String?
        set(value) {
            setProperties("dx", value)
        }
        get() {
            return getProperties("dx")
        }

    var javaPath: String?
        set(value) {
            setProperties("java", value)
        }
        get() {
            return getProperties("java")
        }

    fun savePackage(pkgName: String) {
        setProperties("packagePath", pkgName)
    }

    fun getPackageName(): String? {
        return getProperties("packagePath")
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
        return "$pkgName/cache/view-debug/view-debug-config.json"
    }

    fun getTargetFileDestPath(): String {
        return "$pkgName/cache/view-debug/receive/"
    }

    fun getIdeaFolder(): String {
        return projectPath + File.separator + Project.DIRECTORY_STORE_FOLDER
    }

    private fun getProperties(key: String): String? {
        val properties = Properties()
        File(getIdeaFolder(), "view-debug").inputStream().use {
            properties.load(it)
        }
        return properties.getProperty(key)
    }

    private fun setProperties(key: String, value: String?) {
        val properties = Properties()
        File(getIdeaFolder(), "view-debug").inputStream().use {
            properties.load(it)
        }
        properties.setProperty(key, value ?: "")
    }
}