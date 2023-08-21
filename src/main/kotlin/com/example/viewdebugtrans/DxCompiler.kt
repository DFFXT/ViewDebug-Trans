package com.example.viewdebugtrans

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.io.File
import kotlin.concurrent.thread

abstract class DxCompiler(val project: Project) {


    fun dxCompileJar(jarPath: String, output: String) {
        var dxPath = Config.dxPath
        if (dxPath == null) {
            tryGetDxPath()
        }
        dxPath = Config.dxPath
        if (dxPath != null) {
            /**
             * 这里需要注意，dx工具默认是api 13，但是interface default 方法和static方法需要api>=24
             * 如果不设置版本号则编译失败
             * 这里设置版本号为26
             */
            if (dxPath.endsWith("dx.bat") || dxPath.endsWith("dx")) {
                execute(arrayOf(
                    dxPath,
                    "--dex",
                    "--min-sdk-version=25",
                    "--output=$output",
                    jarPath
                ))
            } else if (dxPath.endsWith("d8.bat") || dxPath.endsWith("d8")) {
                val outputDir = File(output).parentFile.absolutePath
                execute(arrayOf(
                    dxPath,
                    "--output",
                    outputDir,
                    jarPath
                ))
                val dexFile = File(outputDir, "classes.dex")
                if (dexFile.exists()) {
                    show(null, "R8生成")
                    dexFile.renameTo(File(output))
                } else {
                    show(null, "R8没有生成文件")
                }
            } else {
                show(null, "未知路径：$dxPath")
            }

            // 生成了dex文件
            if (!File(output).exists()) {
                show(null, "未生成dex")
            }
        } else {
            show(null, "没有找到dx、d8路径")
        }
    }

    private fun tryGetDxPath() {
        val adbPath = Config.adbPath
        val recommendPath = if (!adbPath.isNullOrEmpty()) {
            "$adbPath/adb.exe"
        } else {
            val adbPaths = execute("where adb").split("\n").map { it.trim() }
            adbPaths.find { it.contains("platform-tools") }
        }

        if (recommendPath != null) {
            val adbFile = File(recommendPath)
            val buildTools = File(adbFile.parentFile.parentFile, "build-tools")
            if (buildTools.exists()) {
                // 找d8
                val d8Path = buildTools.listFiles()?.firstOrNull()?.absolutePath + File.separator + "d8.bat"
                val result = trySetPath(d8Path)
                if (result) return
                // 找dx
                val dxPath = buildTools.listFiles()?.firstOrNull()?.absolutePath + File.separator + "dx.bat"
                trySetPath(dxPath)
            }
        } else {
            Messages.showDialog(project, "没有找到dx路径", "-", arrayOf("确定"), 0, null)
        }
    }

    private fun trySetPath(path: String?): Boolean {
        path?:return false
        val file = File(path)
        if (file.exists() && file.isFile) {
            Config.dxPath = path
            return true
        }
        return false
    }

    protected fun getJavacPath(): String {
        return execute("where javac").split("\n").map { it.trim() }.getOrNull(0) ?: "javac"
    }
    protected fun getKtcPath(): String {
        return execute("where kotlinc").split("\n").map { it.trim() }.getOrNull(0) ?: "kotlinc"
    }
}