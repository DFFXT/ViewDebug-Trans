package com.example.viewdebugtrans

import com.intellij.openapi.actionSystem.AnAction
import java.io.File

open abstract class DxCompiler() {


    fun dxCompileJar(jarPath: String, output: String) {
        val dxPath = Config.dxPath
        if (dxPath == null) {
            tryGetDxPath()
        }
        if (dxPath != null) {
            //val relativeClassPath = relativeJavaPath.replace(".$suffix", ".$toSuffix")
            //val outputDexPath = "${Config.getIdeaFolder()}/view-debug.dex"
            execute(
                "$dxPath --dex --output=\"$output\" \"$jarPath\"",
                /*File(dir)*/
            )
            // 生成了dex文件
            if (!File(output).exists()) {
                println("未生成dex")
            }
        } else {
            println("没有找到dx路径")
        }
    }

    private fun tryGetDxPath() {
        val adbPaths = execute("where adb").split("\n").map { it.trim() }
        val recommendPath = adbPaths.find { it.contains("platform-tools") }
        if (recommendPath != null) {
            val adbFile = File(recommendPath)
            val buildTools = File(adbFile.parentFile.parentFile, "build-tools")
            if (buildTools.exists()) {
                val dxPath = buildTools.listFiles().getOrNull(0)?.absolutePath + File.separator + "dx.bat"
                if (File(dxPath).exists()) {
                    Config.dxPath = dxPath
                }
            }
        }
    }

    protected fun execute(cmd: String, dir: File? = null): String {
        println("执行：$cmd  on $dir")
        return String(Runtime.getRuntime().exec(cmd, null, dir).inputStream.readBytes())
    }


    protected fun getJavacPath(): String {
        return execute("where javac").split("\n").map { it.trim() }.getOrNull(0) ?: "javac"
    }
    protected fun getKtcPath(): String {
        return execute("where kotlinc").split("\n").map { it.trim() }.getOrNull(0) ?: "kotlinc"
    }
}