package com.example.viewdebugtrans

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 使用kotlin插件的【Show Kotlin Bytecode】功能编译代码
 */
class KtCompiler : DxCompiler() {

    /**
     * @return 编译后的dex路径
     */
    fun compile(e: AnActionEvent): String {
        // 得到KtFile对象，这个对象是kotlin插件中声明的，其类加载器能够加载kotlin插件中的其它类
        val KtFile = e.getData(CommonDataKeys.PSI_FILE)
        // 通过kotlin插件的类加载器加载KotlinCompilerIde对象
        val KotlinCompilerIde =
            KtFile!!.javaClass.classLoader.loadClass("org.jetbrains.kotlin.idea.core.KotlinCompilerIde")
        // 加载kotlin编译配置
        val CompilerConfiguration =
            KtFile.javaClass.classLoader.loadClass("org.jetbrains.kotlin.config.CompilerConfiguration")
        // 加载kotlin类生成工厂
        val ClassBuilderFactory =
            KtFile.javaClass.classLoader.loadClass("org.jetbrains.kotlin.codegen.ClassBuilderFactory")
        val Function1 = KtFile.javaClass.classLoader.loadClass("kotlin.jvm.functions.Function1")
        // KotlinCompilerIde构造器
        val constructor = KotlinCompilerIde.getConstructor(
            KtFile::class.java,
            CompilerConfiguration,
            ClassBuilderFactory,
            Function1,
            Boolean::class.java
        )
        // 加载KotlinCompilerIde的伴生类
        val com =
            KtFile.javaClass.classLoader.loadClass("org.jetbrains.kotlin.idea.core.KotlinCompilerIde\$Companion")
        com.declaredMethods
        // 加载伴生类中的getDefaultCompilerConfiguration，用于获取默认的编译配置
        val getDefaultCompilerConfiguration =
            com.getDeclaredMethod("getDefaultCompilerConfiguration", KtFile::class.java)
        getDefaultCompilerConfiguration.isAccessible = true


        // 加载kotlin类生成工厂
        val ClassBuilderFactories =
            KtFile.javaClass.classLoader.loadClass("org.jetbrains.kotlin.codegen.ClassBuilderFactories")
        // 获取默认的BINARIES类生成器
        val BINARIES = ClassBuilderFactories.getDeclaredField("BINARIES").get(null)
        // 生成KotlinCompilerIde的Function1参数
        val resolutionFacadeProvider = object : kotlin.jvm.functions.Function1<Any, Any> {
            override fun invoke(p1: Any): Any {
                // 这个方法需要返回settingLanguageVersion对象
                // 经过分析kotlin插件源码，可通过KtFile的扩展方法来获取，扩展方法位于ResolutionUtils.getResolutionFacade
                val r =
                    KtFile::class.java.classLoader.loadClass("org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils")
                val ktElement = KtFile::class.java.classLoader.loadClass("org.jetbrains.kotlin.psi.KtElement")
                return r.getDeclaredMethod("getResolutionFacade", ktElement).invoke(null, KtFile)

            }
        }

        // 实例化KotlinCompilerIde对象
        val ccc = constructor.newInstance(
            KtFile,
            CompilerConfiguration.newInstance(),
            BINARIES,
            resolutionFacadeProvider,
            false
        )

        // 调用KotlinCompilerIde的compileToBytecode方法将kotlin文件编译成字节码文件，该方法返回List对象，元素类型包含path、bytecode字段
        val ficompileToDirectoryFiled = KotlinCompilerIde.getDeclaredMethod("compileToBytecode")
        val g = ficompileToDirectoryFiled.invoke(ccc)
        val compiledFiles = g as List<Any>
        val result = compiledFiles.map {
            // 取元素的path、bytecode字段
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
        // 输出jar文件
        output(result, jarPath)
        // 将jar转换为dex文件
        dxCompileJar(jarPath, Config.getIdeaFolder() + File.separator + "view-debug.dex")
        show(e.project!!, Config.getIdeaFolder() + File.separator + "view-debug.dex")
        return Config.getIdeaFolder() + File.separator + "view-debug.dex"
    }

    private fun output(compiledResult: List<Pair<String, ByteArray>>, jarPath: String) {
        ZipOutputStream(FileOutputStream(jarPath)).use { os ->
            compiledResult.forEach { item ->
                os.putNextEntry(ZipEntry(item.first))
                if (item.first.endsWith(".class")) {
                    os.write(insertFunction(item.second))
                } else {
                    os.write(item.second)
                }
            }
        }
    }

    //  根据生成文件分析，通过这种方式生成的kotlin会在Fragment的onDestroyView方法末尾生成 _$_clearFindViewByIdCache方法的调用，但实际又没有生成这个方法
    //  所以需要删除这个调用或者生成一个空对应的空方法避免方法找不到的异常
    private fun insertFunction(byteArray: ByteArray): ByteArray {

        val reader = ClassReader(byteArray)
        val writer = ClassWriter(0)
        val visitor = object : ClassVisitor(Opcodes.ASM4, writer) {

            override fun visit(
                version: Int,
                access: Int,
                name: String?,
                signature: String?,
                superName: String?,
                interfaces: Array<out String>?
            ) {
                super.visit(version, access, name, signature, superName, interfaces)
            }

            override fun visitEnd() {

                val methodVisitor = writer.visitMethod(
                    ACC_PUBLIC or Opcodes.ACC_FINAL,
                    "_\$_clearFindViewByIdCache",
                    "()V",
                    null,
                    null
                );
                methodVisitor.visitCode();
                methodVisitor.visitInsn(Opcodes.RETURN);
                methodVisitor.visitMaxs(0, 1);
                methodVisitor.visitEnd();
                super.visitEnd()
            }
        }
        reader.accept(visitor, 0)
        return writer.toByteArray()
    }
}