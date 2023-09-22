package com.example.viewdebugtrans

import com.example.viewdebugtrans.agreement.AdbAgreement
import com.example.viewdebugtrans.agreement.AdbDevicesManager
import com.example.viewdebugtrans.agreement.Device
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import java.io.File
import java.util.*

/**
 * 文件推送
 */
object PushFileManager {

    private lateinit var agreement: AdbAgreement

    private val sendAction = LinkedList<FileItem>()

    private var device: Device? = null
    private lateinit var adbPath: String

    const val TYPE_LAYOUT = "layout"
    const val TYPE_DRAWABLE = "drawable"
    const val TYPE_COLOR = "color"
    const val TYPE_ANIM = "anim"
    const val TYPE_FILE = "file"
    const val TYPE_DEX = "dex"

    /**
     * 初始化
     */
    fun init(device: Device, agreement: AdbAgreement, adbPath: String) {
        reset()
        this.adbPath =adbPath
        this.agreement = agreement
        this.device = device
    }

   /* private fun execute(cmd: String): String {
        return String(Runtime.getRuntime().exec(cmd).inputStream.readBytes())
    }*/

    fun pushFile(target: String, dest: String, type: String = TYPE_FILE, originPath: String = target): String {
        // Config.saveConfig(dest, type)
        addFileItem(originPath, target, dest, type)
        // 先推送文件
        return execute(arrayOf(
            adbPath, "-s", device!!.serialNumber, "push" , target, dest
        ))
    }

    /**
     * 推送配置文件
     */
    fun pushApply() {
        if (sendAction.isNotEmpty()) {
            Config.saveConfig(sendAction)
            val cmd = arrayOf(
                adbPath,
                "-s",
                device!!.serialNumber,
                "push",
                Config.getConfigFile().absolutePath,
                agreement.listenFile
            )
            execute(cmd)
        }
    }


    /**
     * 添加要推送的文件
     */
    private fun addFileItem(originPath: String, target: String, path: String, type: String) {
        sendAction.add(FileItem(originPath, target, path, type))
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
    class FileItem(val originPath: String, val target: String, val path: String, val type: String)
}