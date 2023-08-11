package com.example.viewdebugtrans

import com.google.gson.JsonArray
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
            // windows上反斜杠替换
            setProperties("dx", value?.replace('\\', '/'))
        }
        get() {
            return getProperties("dx")?.replace('\\', '/')
        }
    var dxVersion: String = ""

    var adbPath: String?
        set(value) {
            setProperties("adbPath", value?.replace('\\', '/'))
        }
        get() {
            return getProperties("adbPath")?.replace('\\', '/')
        }

    var javaPath: String?
        set(value) {
            setProperties("java", value?.replace('\\', '/'))
        }
        get() {
            return getProperties("java")?.replace('\\', '/')
        }

    var RFilePath: String?
        set(value) {
            setProperties("RPath", value?.replace('\\', '/'))
        }
        get() = getProperties("RPath")?.replace('\\', '/')

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
    fun saveConfig(fileItems: List<PushFileManager.FileItem>) {
        val config = getConfigFile()
        val json = JsonObject()
        val arr = JsonArray()
        fileItems.forEach {
            val item = JsonObject()
            item.addProperty("file", it.path)
            item.addProperty("type", it.type)
            arr.add(item)
        }
        json.add("config", arr)
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