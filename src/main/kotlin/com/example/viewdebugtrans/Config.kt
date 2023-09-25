package com.example.viewdebugtrans

import com.example.viewdebugtrans.util.Utils
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.openapi.project.Project
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.*

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


    fun updateProject(project: Project) {
        projectPath = project.basePath!!
    }

    fun savePackage(pkgName: String) {
        setProperties("packagePath", pkgName)
    }

    /*fun getPackageName(): String? {
        return getProperties("packagePath")
    }*/

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
            // 原始文件路径，具有唯一性
            item.addProperty("originPath", it.originPath)
            arr.add(item)
        }
        json.add("config", arr)
        config.writeBytes(json.toString().toByteArray())
    }

    fun getConfigFile(): File {
        return File(getIdeaFolder(), "view-debug-config.json")
    }

  /*  fun getConfigRemotePath(): String {
        return "${getPackageName()}/cache/view-debug/view-debug-config.json"
    }

    fun getTargetFileDestPath(): String {
        return "${getPackageName()}/cache/view-debug/receive/"
    }*/

    @Deprecated("project.getViewDebugDir()")
    fun getIdeaFolder(): String {
        val file = File(projectPath + File.separator + Project.DIRECTORY_STORE_FOLDER + File.separator + "viewDebug")
        if (!file.exists()) {
            file.mkdirs()
        }
        return file.absolutePath
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

    /**
     * 复制脚本到外部
     */
    fun copyCmdIfNotExists(cmd: String, fileName: String) {
        val file = File(getIdeaFolder(), fileName)
        file.createNewFile()
        file.writeText(cmd)
    }

    private val md5 by lazy {
        MessageDigest.getInstance("MD5")
    }
    fun md5(string: String): String {
        md5.reset()
        val md5Byte = md5.digest(string.toByteArray())
        val builder = StringBuilder()

        for (aByte in md5Byte) {
            builder.append(Integer.toHexString(0x000000FF and aByte.toInt() or -0x100).substring(6))
        }

        return builder.toString()
    }


}