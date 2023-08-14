package com.example.viewdebugtrans

import com.intellij.openapi.project.ProjectManager
import java.io.File
import java.util.*

/**
 * 文件推送
 */
object PushFileManager {

    private val sendAction = LinkedList<FileItem>()

    private var device: String? = null

    const val TYPE_LAYOUT = "layout"
    const val TYPE_DRAWABLE = "drawable"
    const val TYPE_COLOR = "color"
    const val TYPE_ANIM = "anim"
    const val TYPE_FILE = "file"
    const val TYPE_DEX = "dex"

    /**
     * 初始化
     */
    fun init(device: String) {
        this.device = device
    }

   /* private fun execute(cmd: String): String {
        return String(Runtime.getRuntime().exec(cmd).inputStream.readBytes())
    }*/

    fun pushFile(target: String, dest: String, type: String = TYPE_FILE): String {
        // Config.saveConfig(dest, type)
        addFileItem(target, dest, type)
        // 先推送文件
        return execute(arrayOf(
            "adb", "-s", device!!, "push" , target, dest
        ))
    }


    fun checkRemoteFolder(device: String, folder: String) {
        execute(arrayOf("adb", "-s", device, "shell", "mkdir", folder))
        //execute("adb -s $device shell mkdir \"$folder\"")
    }

    /**
     * 推送配置文件
     */
    fun pushApply() {
        if (sendAction.isNotEmpty()) {
            Config.saveConfig(sendAction)
            val cmd = arrayOf(
                "adb",
                "-s",
                device!!,
                "push",
                Config.getConfigFile().absolutePath,
                Config.getConfigRemotePath()
            )
            execute(cmd)
        }
    }


    /**
     * 添加要推送的文件
     */
    private fun addFileItem(target: String, path: String, type: String) {
        sendAction.add(FileItem(target, path, type))
    }

    fun reset() {
        device = null
        sendAction.clear()
    }

    /**
     * @param target 要推送的文件
     * @param path 要推送到的远程地址
     * @param type 文件类型
     */
    class FileItem(val target: String, val path: String, val type: String)
}