package com.example.viewdebugtrans

import java.io.File

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
        /*if (adbPath.endsWith("adb.exe") || adbPath.endsWith("adb")) {
            changedCmd = adbPath + cmd.substring(3)
        } else {
            changedCmd = "$adbPath/adb.exe" + cmd.substring(3)
        }*/
    }
    show(null, "执行：$changedCmd")
    val result = String(Runtime.getRuntime().exec(changedCmd, null, dir).inputStream.readBytes())
    show(null, "执行结果：$result")
    return result
}

fun execute(cmdArray: Array<String>, dir: File? = null): String {
    val cmd = cmdArray[0]
    val adbPath = Config.adbPath
    //var changedCmd = cmd
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
    val result = String(Runtime.getRuntime().exec(cmdArray, null, dir).inputStream.readBytes())
    show(null, "执行结果：$result")
    return result
}