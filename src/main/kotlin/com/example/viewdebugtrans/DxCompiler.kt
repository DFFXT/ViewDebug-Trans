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
            /**
             * 这里需要注意，dx工具默认是api 13，但是interface default 方法和static方法需要api>=24
             * 如果不设置版本号则编译失败
             * 这里设置版本号为26
             */
            show(null,execute(
                "$dxPath --dex --min-sdk-version=25 --output=\"$output\" \"$jarPath\"",
                /*File(dir)*/
            ))
            // 生成了dex文件
            if (!File(output).exists()) {
                show(null, "未生成dex")
            }
        } else {
            show(null, "没有找到dx路径")
        }
    }

    private fun tryGetDxPath() {
        val adbPath = Config.adbPath
        val recommendPath = if (!adbPath.isNullOrEmpty()) {
            adbPath
        } else {
            val adbPaths = execute("where adb").split("\n").map { it.trim() }
            adbPaths.find { it.contains("platform-tools") }
        }

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

    /*protected fun execute(cmd: String, dir: File? = null): String {
        println("执行：$cmd  on $dir")
        return String(Runtime.getRuntime().exec(cmd, null, dir).inputStream.readBytes())
    }*/

    protected fun getJavacPath(): String {
        return execute("where javac").split("\n").map { it.trim() }.getOrNull(0) ?: "javac"
    }
    protected fun getKtcPath(): String {
        return execute("where kotlinc").split("\n").map { it.trim() }.getOrNull(0) ?: "kotlinc"
    }
}