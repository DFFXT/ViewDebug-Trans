package com.example.viewdebugtrans

import com.example.viewdebugtrans.agreement.AdbAgreement
import com.example.viewdebugtrans.agreement.Device
import com.example.viewdebugtrans.util.getViewDebugDir
import com.google.gson.JsonElement
import com.intellij.openapi.project.Project
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
    private var project: Project? = null

    const val TYPE_LAYOUT = "layout"
    const val TYPE_DRAWABLE = "drawable"
    const val TYPE_COLOR = "color"
    const val TYPE_ANIM = "anim"
    const val TYPE_FILE = "file"
    const val TYPE_DEX = "dex"
    // 规则文件
    const val TYPE_XML_RULE = "rules"
    // 重新运行信号
    const val TYPE_LAUNCH = "launch"
    // xml value 文件
    const val TYPE_XML_VALUES = "values"

    /**
     * 初始化
     */
    fun init(project: Project, device: Device, agreement: AdbAgreement, adbPath: String) {
        reset()
        this.project = project
        this.adbPath =adbPath
        this.agreement = agreement
        this.device = device
    }

   /* private fun execute(cmd: String): String {
        return String(Runtime.getRuntime().exec(cmd).inputStream.readBytes())
    }*/

    fun pushFile(target: String, dest: String, type: String = TYPE_FILE, originPath: String = target, extra: JsonElement? = null) {
        // Config.saveConfig(dest, type)
        addFileItem(originPath, target, dest, type, extra)
        if (extra != null) {
            // 推送extra文件
            val extraFile = File(project!!.getViewDebugDir(), "extra")
            extraFile.writeText(extra.toString())
            execute(arrayOf(
                adbPath, "-s", device!!.serialNumber, "push" , extraFile.absolutePath, "$dest.extra"
            ))
        }

        // 先推送文件
        execute(arrayOf(
            adbPath, "-s", device!!.serialNumber, "push" , target, dest
        ))
    }

    /**
     * 推送配置文件
     */
    fun pushApply(reboot: Boolean) {
        if (sendAction.isNotEmpty()) {
            Config.saveConfig(sendAction, reboot)
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
    private fun addFileItem(originPath: String, target: String, path: String, type: String, extra: JsonElement?) {
        sendAction.add(FileItem(originPath, target, path, type, extra))
    }

    fun reset() {
        device = null
        sendAction.clear()
        project = null
    }

    /**
     * @param target 要推送的文件
     * @param path 要推送到的远程地址
     * @param type 文件类型
     */
    class FileItem(val originPath: String, val target: String, val path: String, val type: String, val extra: JsonElement?)
}