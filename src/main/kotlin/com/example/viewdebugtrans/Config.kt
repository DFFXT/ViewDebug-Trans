package com.example.viewdebugtrans

import com.google.gson.JsonObject
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.PropertiesUtil
import java.io.File
import java.io.FileOutputStream
import java.util.Properties

object Config {
    var projectPath: String = ""
    private set

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

    var RFilePath: String?
        set(value) {
            setProperties("RPath", value)
        }
        get() = getProperties("RPath")

    fun updateProject(project: Project) {
        projectPath = project.basePath!!
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
        return "${getPackageName()}/cache/view-debug/view-debug-config.json"
    }

    fun getTargetFileDestPath(): String {
        return "${getPackageName()}/cache/view-debug/receive/"
    }

    fun getIdeaFolder(): String {
        return projectPath + File.separator + Project.DIRECTORY_STORE_FOLDER
    }

    private fun getProperties(key: String): String? {
        val properties = Properties()
        val file = File(getIdeaFolder(), "view-debug")
        if (file.exists()) {
            file.inputStream().use {
                properties.load(it)
            }
            return properties.getProperty(key)
        } else {
            return null
        }
    }

    private fun setProperties(key: String, value: String?) {
        val properties = Properties()
        val file = File(getIdeaFolder(), "view-debug")
        if (file.exists()) {
            file.inputStream().use {
                properties.load(it)
            }
        }
        properties.setProperty(key, value ?: "")
        FileOutputStream(File(getIdeaFolder(), "view-debug")).use {
            properties.store(it, null)
        }
    }
}