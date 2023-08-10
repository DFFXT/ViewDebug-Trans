package com.example.viewdebugtrans

import com.intellij.openapi.project.ProjectManager
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
        addFileItem(dest, type)
        // 先推送文件
        return execute("adb -s $device push $target $dest")
        // 再推送config文件

        // return execute("adb -s $device push \"${Config.getConfigFile().absolutePath}\" ${Config.getConfigRemotePath()}")
    }


    fun checkRemoteFolder(device: String, folder: String) {
        execute("adb -s $device shell mkdir \"$folder\"")
    }

    /**
     * 推送配置文件
     */
    fun pushApply() {
        if (sendAction.isNotEmpty()) {
            Config.saveConfig(sendAction)
            execute("adb -s $device push \"${Config.getConfigFile().absolutePath}\" ${Config.getConfigRemotePath()}")
        }
    }


    /**
     * 添加要推送的文件
     */
    private fun addFileItem(path: String, type: String) {
        sendAction.add(FileItem(path, type))
    }

    fun reset() {
        device = null
        sendAction.clear()
    }

    class FileItem(val path: String, val type: String)
}