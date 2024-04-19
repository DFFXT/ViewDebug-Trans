package com.example.viewdebugtrans

import com.android.tools.r8.*
import com.android.tools.r8.origin.Origin
import com.example.viewdebugtrans.action.PushManager
import com.example.viewdebugtrans.util.getViewDebugDir
import com.example.viewdebugtrans.util.showTip
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.idea.base.projectStructure.languageVersionSettings
import org.jetbrains.kotlin.idea.core.KotlinCompilerIde
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.commons.AdviceAdapter
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 使用kotlin插件的【Show Kotlin Bytecode】功能编译代码
 */
class KtCompiler(module: com.intellij.openapi.module.Module) : DxCompiler(module) {
    private lateinit var fileInfo: PushManager.FileInfo

    /**
     * @return 编译后的dex路径
     */
    fun compile(project: Project, fileInfo: PushManager.FileInfo, ktFile: KtFile) {
        // 得到KtFile对象，这个对象是kotlin插件中声明的，其类加载器能够加载kotlin插件中的其它类
        this.fileInfo = fileInfo
        /*// 通过kotlin插件的类加载器加载KotlinCompilerIde对象
        val kotlinCompilerIde =
            ktFile!!.javaClass.classLoader.loadClass("org.jetbrains.kotlin.idea.core.KotlinCompilerIde")
        // 加载kotlin编译配置
        val compilerConfiguration =
            ktFile.javaClass.classLoader.loadClass("org.jetbrains.kotlin.config.CompilerConfiguration")
        // 加载kotlin类生成工厂
        val classBuilderFactory =
            ktFile.javaClass.classLoader.loadClass("org.jetbrains.kotlin.codegen.ClassBuilderFactory")
        val Function1 = ktFile.javaClass.classLoader.loadClass("kotlin.jvm.functions.Function1")
        // KotlinCompilerIde构造器
        val constructor = kotlinCompilerIde.getConstructor(
            ktFile::class.java,
            compilerConfiguration,
            classBuilderFactory,
            Function1,
            Boolean::class.java
        )
        // 加载KotlinCompilerIde的伴生类
        val com =
            ktFile.javaClass.classLoader.loadClass("org.jetbrains.kotlin.idea.core.KotlinCompilerIde\$Companion")
        com.declaredMethods
        // 加载伴生类中的getDefaultCompilerConfiguration，用于获取默认的编译配置
        val getDefaultCompilerConfiguration =
            com.getDeclaredMethod("getDefaultCompilerConfiguration", ktFile::class.java)
        getDefaultCompilerConfiguration.isAccessible = true


        // 加载kotlin类生成工厂
        val classBuilderFactories =
            ktFile.javaClass.classLoader.loadClass("org.jetbrains.kotlin.codegen.ClassBuilderFactories")
        // 获取默认的BINARIES类生成器
        val BINARIES = classBuilderFactories.getDeclaredField("BINARIES").get(null)
        // 生成KotlinCompilerIde的Function1参数
        val resolutionFacadeProvider = object : kotlin.jvm.functions.Function1<Any, Any> {
            override fun invoke(p1: Any): Any {
                // 这个方法需要返回settingLanguageVersion对象
                // 经过分析kotlin插件源码，可通过KtFile的扩展方法来获取，扩展方法位于ResolutionUtils.getResolutionFacade
                val r =
                    ktFile::class.java.classLoader.loadClass("org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils")
                val ktElement = ktFile::class.java.classLoader.loadClass("org.jetbrains.kotlin.psi.KtElement")
                return r.getDeclaredMethod("getResolutionFacade", ktElement).invoke(null, ktFile)

            }
        }*/

        // 实例化KotlinCompilerIde对象
        val configuration = CompilerConfiguration()
        configuration.put(JVMConfigurationKeys.DISABLE_OPTIMIZATION, true)
        //configuration.put(JVMConfigurationKeys.IR, true)
        val jvmTargets = ComboBox(JvmTarget.supportedValues().map { it.description }.toTypedArray())
        configuration.put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.fromString(jvmTargets.selectedItem as String)!!)
        configuration.languageVersionSettings = ktFile.languageVersionSettings
        val acc = KotlinCompilerIde(ktFile, CompilerConfiguration(), ClassBuilderFactories.BINARIES)
        /*val ccc = constructor.newInstance(
            ktFile,
            compilerConfiguration.newInstance(),
            BINARIES,
            resolutionFacadeProvider,
            false
        )*/

        // 调用KotlinCompilerIde的compileToBytecode方法将kotlin文件编译成字节码文件，该方法返回List对象，元素类型包含path、bytecode字段
        // val ficompileToDirectoryFiled = kotlinCompilerIde.getDeclaredMethod("compileToBytecode")
        //val g = ficompileToDirectoryFiled.invoke(ccc)
        val g = acc.compileToBytecode()
        val result = g.map {
            // 取元素的path、bytecode字段
            Pair(it.path, it.bytecode)
        }
        show(project, result.size.toString())
        val jarPath = module.project.getViewDebugDir().absolutePath + File.separator + "view-debug.jar"

        // 输出文本
        val generatedDex = getOutputFileName(File(fileInfo.path))

        // 输出jar文件
        output(result, jarPath, generatedDex)

        // dxCompileJar(jarPath, generatedDex)
        File(jarPath).let {
            // 重命名jar产物文件
            val renameToFile = File(it.parent, "view-debug-delete.jar")
            renameToFile.delete()
            it.renameTo(renameToFile)
        }
        fileInfo.path = generatedDex
        // return generatedDex
    }


    /**
     * 获取转换后的文件名称，具有唯一性
     * 格式：md5_原始名称_kt.dex
     */
    private fun getOutputFileName(ktPath: File): String {
        // 原始文件名称
        val originKtFileName = ktPath.nameWithoutExtension + "_kt"
        // 将jar转换为dex文件
        return module.project.getViewDebugDir().absolutePath + File.separator + Config.md5(ktPath.absolutePath) + "_"+originKtFileName+".dex"
    }

    /**
     * @param compiledResult 字节码数据，first，文件路径名称（jar中的路径）
     * @param jarPath 生成jar路径
     * @param dexPath 生成dex路径
     */
    private fun output(compiledResult: List<Pair<String, ByteArray>>, jarPath: String, dexPath: String) {
        val bytes = ArrayList<ByteArray>()
        val extra = JsonObject()
        val classArr = JsonArray()
        extra.add("class", classArr)
        // 记录额外信息
        /**
         * {
         * class:[]
         * }
         */
        fileInfo.extra = extra
        ZipOutputStream(FileOutputStream(jarPath)).use { os ->
            compiledResult.forEach { item ->
                os.putNextEntry(ZipEntry(item.first))
                if (item.first.endsWith(".class")) {
                    val byteArray = insertFunction(item.second)
                    os.write(byteArray)
                    bytes.add(byteArray)
                    var name = item.first.replace('/', '.')
                    classArr.add(name.substring(0, name.length-6))
                } else {
                    os.write(item.second)
                }
            }
        }
        val file = File(dexPath)
        val dexByteArray = dex(bytes)
        if (dexByteArray != null) {
            file.writeBytes(dexByteArray)
        } else {
            showTip(module.project, "生成dex文件失败")
        }

    }

    private fun dex(classes: List<ByteArray>): ByteArray? {
        try {
            val builder: D8Command.Builder = D8Command.builder()
            val consumer = DexConsumer()
            for (bytes in classes) {
                builder.addClassProgramData(bytes, Origin.unknown())
            }
            builder.mode = CompilationMode.DEBUG
            builder.programConsumer = consumer
            builder.minApiLevel = 13
            builder.disableDesugaring = true
            D8.run(builder.build())
            return consumer.bytes
        } catch (e: Exception) {
            show(e)
            return null
        }
    }

    /**
     * 复制于[org.jetbrains.kotlin.android.debugger.AndroidDexerImpl]
     */
    private class DexConsumer : DexIndexedConsumer {
        var bytes: ByteArray? = null

        @Synchronized
        override fun accept(
            fileIndex: Int, data: ByteDataView, descriptors: Set<String>, handler: DiagnosticsHandler
        ) {
            if (bytes != null) throw IllegalStateException("Multidex not supported")
            bytes = data.copyByteData()
        }

        override fun finished(handler: DiagnosticsHandler) {
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

            override fun visitMethod(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?
            ): MethodVisitor {
                // 由于sdk内部会添加’_$_clearFindViewByIdCache‘方法，需要删除
                if (name == "onDestroyView") {
                    return object : AdviceAdapter(Opcodes.ASM4, super.visitMethod(access, name, descriptor, signature, exceptions), access, name, descriptor) {

                        override fun visitMethodInsn(
                            opcodeAndSource: Int,
                            owner: String?,
                            name: String?,
                            descriptor: String?,
                            isInterface: Boolean
                        ) {
                            println("visitMethodInsn: $name")
                            if (name == "_\$_clearFindViewByIdCache") {
                                return
                            }
                            super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
                        }
                    }
                }

                return super.visitMethod(access, name, descriptor, signature, exceptions)
            }
        }
        reader.accept(visitor, ClassReader.EXPAND_FRAMES)
        return writer.toByteArray()
    }
}