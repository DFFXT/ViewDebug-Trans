package com.example.viewdebugtrans

import com.example.viewdebugtrans.action.AdbSendAction
import com.example.viewdebugtrans.util.getViewDebugDir
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import java.io.File
import java.util.regex.Pattern


/**
 * val KtFile = e.getData(CommonDataKeys.PSI_FILE)
val KotlinCompilerIde = KtFile!!.javaClass.classLoader.loadClass("org.jetbrains.kotlin.idea.core.KotlinCompilerIde")
val CompilerConfiguration = KtFile!!.javaClass.classLoader.loadClass("org.jetbrains.kotlin.config.CompilerConfiguration")
val ClassBuilderFactory = KtFile!!.javaClass.classLoader.loadClass("org.jetbrains.kotlin.codegen.ClassBuilderFactory")
val Function1 = KtFile!!.javaClass.classLoader.loadClass("kotlin.jvm.functions.Function1")
val constructor = KotlinCompilerIde.getConstructor(KtFile::class.java, CompilerConfiguration, ClassBuilderFactory, Function1, Boolean::class.java )
val com = KtFile!!.javaClass.classLoader.loadClass("org.jetbrains.kotlin.idea.core.KotlinCompilerIde\$Companion")
com.declaredMethods
val getDefaultCompilerConfiguration = com.getDeclaredMethod("getDefaultCompilerConfiguration", KtFile::class.java)
getDefaultCompilerConfiguration.isAccessible = true

//val c = getDefaultCompilerConfiguration.invoke(null, KtFile)

val ClassBuilderFactories = KtFile!!.javaClass.classLoader.loadClass("org.jetbrains.kotlin.codegen.ClassBuilderFactories")
val BINARIES = ClassBuilderFactories.getDeclaredField("BINARIES").get(null)
val resolutionFacadeProvider = object : kotlin.jvm.functions.Function1<Any,Any> {
override fun invoke(p1: Any): Any {
val f =  KtFile::class.java.getDeclaredField("languageVersionSettings")
return f.get(KtFile)
}
}

val  ccc= constructor.newInstance(KtFile, CompilerConfiguration.newInstance(), BINARIES, resolutionFacadeProvider, false)

val ficompileToDirectoryFiled = KotlinCompilerIde.getDeclaredMethod("compileToDirectory", File::class.java)
//ficompileToDirectoryFiled.invoke(ccc, File(Config.getIdeaFolder()))

var c:Class<*> = KtFile::class.java
while (c.methods.find { it.name.contains("uageVersionSet") } == null) {
c=c.superclass
if (c == null) break
}

c


 */
class CompileFileAndSend(module: com.intellij.openapi.module.Module): DxCompiler(module) {

    companion object {
        private var dxPath: String? = null
    }

    fun compile(fileInfo: AdbSendAction.FileInfo, e: AnActionEvent): String {
        val path = fileInfo.path
        if (path.endsWith(".java")) {
            //JavaByteCode().getByteCode(e)
            return path
                //return compileJava(path, "java", "class", getJavacPath())
        } else if (path.endsWith(".kt")) {
            return KtCompiler(module).compile(fileInfo, e)
        }
        return path
    }

    /**
     * 使用javac编译java文件
     */
    @Deprecated("这个方法只能编译无三方依赖的java类，如果能得到java所有依赖路径，就可以编译")
    private fun compileJava(path: String, suffix: String, toSuffix: String, compilerPath: String): String {
        val tag = "package "
        val line = File(path).readLines().find { it.startsWith(tag) }
        val index = line?.indexOf(tag) ?: -1
        val pkgName = if (index >= 0) {
            // 包名上不能有注释
            line!!.substring(index + tag.length, line.length - 1).replace(".", "/")
        } else {
            ""
        }
        val base = path.replace(pkgName, "")
        val file = File(base)
        val dir = file.parent
        val relativeJavaPath = if (pkgName.isEmpty()) {
            file.name
            //execute("javac ${file.name}", File(dir))
        } else {
            pkgName + File.separator + file.name
            //execute("javac $pkgName/${file.name}", File(dir))
        }
        execute("$compilerPath $relativeJavaPath", File(dir))
        val classFilePath = path.replace(".$suffix", ".class")
        val classFile = File(classFilePath)
        if (classFile.exists()) {
            // 判断是否有dx工具
            if (dxPath == null) {
                val adbPaths = execute("where adb").split("\n").map { it.trim() }
                val recommendPath = adbPaths.find { it.contains("platform-tools") }
                if (recommendPath != null) {
                    val adbFile = File(recommendPath)
                    val buildTools = File(adbFile.parentFile.parentFile, "build-tools")
                    if (buildTools.exists()) {
                        val dxPath = buildTools.listFiles()?.getOrNull(0)?.absolutePath + File.separator + "dx.bat"
                        if (File(dxPath).exists()) {
                            CompileFileAndSend.dxPath = dxPath
                        }
                    }
                }
            }
            if (dxPath != null) {
                val relativeClassPath = relativeJavaPath.replace(".$suffix", ".$toSuffix")
                val outputDexPath = "${module.project.getViewDebugDir().absolutePath}/view-debug.dex"
                execute(arrayOf(dxPath!!, "--dex", "--output=$outputDexPath", relativeClassPath), File(dir))
                /*execute(
                    "$dxPath --dex --output=\"$outputDexPath\" \"$relativeClassPath\"",
                    File(dir)
                )*/
                // 生成了dex文件
                if (File(outputDexPath).exists()) {
                    return outputDexPath
                }
            } else {
                println("没有找到dx路径")
            }
        } else {
            println("未生成class文件")
        }
        return path
    }
}