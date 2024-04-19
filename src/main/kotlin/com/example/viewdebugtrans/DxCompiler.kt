package com.example.viewdebugtrans

import com.android.sdklib.BuildToolInfo
import com.android.tools.idea.sdk.IdeSdks
import com.example.viewdebugtrans.util.showTip
import org.jetbrains.android.facet.AndroidFacet
// import org.jetbrains.android.sdk.AndroidSdkData
import org.jetbrains.android.sdk.AndroidSdkUtils
import java.io.File

abstract class DxCompiler(val module: com.intellij.openapi.module.Module) {


    @Deprecated("不再使用了，使用android插件的D8工具转dex")
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
        if (File(Config.dxPath ?: "%~11://???").exists()) return
        // 获取Android sdk数据，找到dx路径

        val androidFacet = AndroidFacet.getInstance(module) ?: return showTip(module.project, "androidFacet null")

        // val dxPath = AndroidSdkData.getSdkData(IdeSdks.getInstance().getAndroidSdkPath()!!)?.targets?.lastOrNull()?.buildToolInfo?.getPath(BuildToolInfo.PathId.ANDROID_RS) ?: return showTip(module.project, "没有找到dx")
        // android studio 升级了，AndroidSdkData被AndroidSdkUtils代替
        val dxPath = AndroidSdkUtils.getProjectSdkData(module.project)?.targets?.lastOrNull()?.buildToolInfo?.getPath(BuildToolInfo.PathId.ANDROID_RS) ?: return showTip(module.project, "没有找到dx")
        val dx = File(dxPath)
        val dir = dx.parent
        val d8 = File(dir, "SdkConstants.FN_DX.replace(,)")
        // AndroidSdks.getInstance().tryToChooseSdkHandler().getLatestBuildTool(StudioLoggerProgressIndicator(javaClass), true)

        if (d8.exists()) {
            Config.dxPath = d8.absolutePath
        } else {
            if (dx.exists()) {
                Config.dxPath = dxPath
            } else {
                showTip(module.project, "没有找到d8、dx位置")
            }
        }
        /*val adbPath = Config.adbPath
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
            Messages.showDialog(module.project, "没有找到dx路径", "-", arrayOf("确定"), 0, null)
        }*/
    }


    protected fun getJavacPath(): String {
        return execute("where javac").split("\n").map { it.trim() }.getOrNull(0) ?: "javac"
    }
    protected fun getKtcPath(): String {
        return execute("where kotlinc").split("\n").map { it.trim() }.getOrNull(0) ?: "kotlinc"
    }
}