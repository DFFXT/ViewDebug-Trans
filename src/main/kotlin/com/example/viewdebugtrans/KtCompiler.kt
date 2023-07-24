package com.example.viewdebugtrans

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class KtCompiler: DxCompiler() {

    fun compile(e: AnActionEvent): String  {
        val KtFile = e.getData(CommonDataKeys.PSI_FILE)
        val KotlinCompilerIde =
            KtFile!!.javaClass.classLoader.loadClass("org.jetbrains.kotlin.idea.core.KotlinCompilerIde")
        val CompilerConfiguration =
            KtFile!!.javaClass.classLoader.loadClass("org.jetbrains.kotlin.config.CompilerConfiguration")
        val ClassBuilderFactory =
            KtFile!!.javaClass.classLoader.loadClass("org.jetbrains.kotlin.codegen.ClassBuilderFactory")
        val Function1 = KtFile!!.javaClass.classLoader.loadClass("kotlin.jvm.functions.Function1")
        val constructor = KotlinCompilerIde.getConstructor(
            KtFile::class.java,
            CompilerConfiguration,
            ClassBuilderFactory,
            Function1,
            Boolean::class.java
        )
        val com =
            KtFile!!.javaClass.classLoader.loadClass("org.jetbrains.kotlin.idea.core.KotlinCompilerIde\$Companion")
        com.declaredMethods
        val getDefaultCompilerConfiguration =
            com.getDeclaredMethod("getDefaultCompilerConfiguration", KtFile::class.java)
        getDefaultCompilerConfiguration.isAccessible = true

//val c = getDefaultCompilerConfiguration.invoke(null, KtFile)

        val ClassBuilderFactories =
            KtFile!!.javaClass.classLoader.loadClass("org.jetbrains.kotlin.codegen.ClassBuilderFactories")
        val BINARIES = ClassBuilderFactories.getDeclaredField("BINARIES").get(null)
        val resolutionFacadeProvider = object : kotlin.jvm.functions.Function1<Any, Any> {
            override fun invoke(p1: Any): Any {
                val r =
                    KtFile::class.java.classLoader.loadClass("org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils")
                val ktElement = KtFile::class.java.classLoader.loadClass("org.jetbrains.kotlin.psi.KtElement")
                return r.getDeclaredMethod("getResolutionFacade", ktElement).invoke(null, KtFile)

            }
        }

        val ccc = constructor.newInstance(
            KtFile,
            CompilerConfiguration.newInstance(),
            BINARIES,
            resolutionFacadeProvider,
            false
        )

        val ficompileToDirectoryFiled = KotlinCompilerIde.getDeclaredMethod("compileToBytecode")
        val g = ficompileToDirectoryFiled.invoke(ccc)
        val f = g
        val compiledFiles = g as List<Any>
        val result = compiledFiles.map {
            val pathFiled = it::class.java.getDeclaredField("path")
            pathFiled.isAccessible = true
            val path = pathFiled.get(it) as String
            val byteCodeFiled = it::class.java.getDeclaredField("bytecode")
            byteCodeFiled.isAccessible = true
            val byteCode = byteCodeFiled.get(it) as ByteArray
            Pair(path, byteCode)
        }
        show(e.project!!, result.size.toString())
        val jarPath = Config.getIdeaFolder() + File.separator + "view-debug.jar"
        output(result, jarPath)
        dxCompileJar(jarPath, Config.getIdeaFolder() + File.separator + "view-debug.dex")
        show(e.project!!, Config.getIdeaFolder() + File.separator + "view-debug.dex")
        return Config.getIdeaFolder() + File.separator + "view-debug.dex"
    }
    private fun output(compiledResult: List<Pair<String, ByteArray>>, jarPath: String) {
        ZipOutputStream(FileOutputStream(jarPath)).use {os ->
            compiledResult.forEach { item ->
                os.putNextEntry(ZipEntry(item.first))
                os.write(item.second)
            }
        }
    }
}