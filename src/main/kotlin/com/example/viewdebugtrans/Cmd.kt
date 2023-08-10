package com.example.viewdebugtrans

import java.io.File

fun execute(cmd: String, dir: File? = null): String {
    var changedCmd = cmd
    val adbPath = Config.adbPath
    if (cmd.startsWith("adb") && !adbPath.isNullOrEmpty()) {
        changedCmd = "$adbPath/adb.exe" + cmd.substring(3)
    }
    show(null, "执行：$changedCmd")
    val result = String(Runtime.getRuntime().exec(changedCmd, null, dir).inputStream.readBytes())
    show(null, "执行结果：$result")
    return result
}