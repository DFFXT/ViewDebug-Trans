package com.example.viewdebugtrans

import java.io.ByteArrayOutputStream
import java.io.File

@Deprecated("")
fun execute(cmd: String, dir: File? = null): String {
    var changedCmd = cmd
    val adbPath = Config.adbPath
    if (cmd.startsWith("adb") && !adbPath.isNullOrEmpty()) {
        // 兼容linux
        val file = File(adbPath)
        if (file.exists() && file.isFile) {
            changedCmd = adbPath + cmd.substring(3)
        } else if (file.isDirectory) {
            changedCmd = "$adbPath/adb.exe" + cmd.substring(3)
        }
    }
    show(null, "执行：$changedCmd")
    val p = Runtime.getRuntime().exec(changedCmd, null, dir)
    show(null, "执行错误信息："+String(p.errorStream.readBytes()))
    val result = String(p.inputStream.readBytes())
    show(null, "执行结果：$result")
    return result
}

fun execute(cmdArray: Array<String>, dir: File? = null): String {

    val adbPath = Config.adbPath
    if (cmdArray[0] == "adb" && !adbPath.isNullOrEmpty()) {
        // 如果有设置adb路径，则使用adb路径
        val file = File(adbPath)
        if (file.exists() && file.isFile) {
            cmdArray[0] = adbPath
        } else {
            show(null, "adb path is not a file")
        }
    }
    show(null, "执行：${cmdArray.joinToString(separator = " ")}")
    val p = Runtime.getRuntime().exec(cmdArray, null, dir)
    // 需要读取errorStream，否则缓冲区堆积，导致死锁
    val errorStream = p.errorStream.readBytes()
    show(null, "执行错误信息:"+ String(errorStream))
    val result = String(p.inputStream.readBytes())
    show(null, "执行结果：$result")
    return result
}