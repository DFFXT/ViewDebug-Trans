package com.example.viewdebugtrans.interceptor


import com.example.viewdebugtrans.action.PushManager
import com.example.viewdebugtrans.agreement.AdbAgreement
import com.example.viewdebugtrans.agreement.Device
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.NewJavaToKotlinConverter.Companion.addImports
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.util.LinkedList

/**
 * java代码推送前期工作，将java转换成kotlin
 */
class JavaPushInterceptor : IPushInterceptor {
    override fun beforePush(
        project: Project,
        fileInfo: PushManager.FileInfo,
        device: Device,
        agreement: AdbAgreement
    ) {
        if (fileInfo.path.endsWith(".java")) {
            var file: PsiFile? = null
            ApplicationManager.getApplication().runReadAction {
                file = LocalFileSystem.getInstance().findFileByIoFile(File(fileInfo.path))?.toPsiFile(project)
            }

            val module = file?.module ?: return
            val f = file
            if (f is PsiJavaFile) {

                WriteCommandAction.runWriteCommandAction(project) {
                    // JavaToKotlinAction需要放到EDT线程执行
                    val result = JavaToKotlinAction.Handler.convertFiles(
                        listOf(f),
                        project,
                        module
                    )
                    val javaFile = f.virtualFile
                    var name = javaFile.nameWithoutExtension
                    // 防止name重复
                    while (javaFile.parent.findChild("$name.kt") != null) {
                        name = "A${name.reversed()}"
                    }
                    javaFile.parent.findChild(name)
                    val ktFile = javaFile.parent.createChildData(null, "$name.kt")
                    ktFile.setBinaryContent(result.first().toByteArray())

                    val newKtFile:KtFile =
                        PsiFileFactory.getInstance(project).createFileFromText(ktFile.path, KotlinLanguage.INSTANCE, result.first()) as KtFile


                   /* val imports = importList.filterNotNull().map { FqName(it) }
                    if (imports.isNotEmpty()) {
                        val ktpsi = PsiManager.getInstance(project).findFile(ktFile)!! as KtFile
                        ktpsi.addImports(imports)
                        val d = PsiDocumentManager.getInstance(project).getDocument(ktpsi)!!
                        FileDocumentManager.getInstance().saveDocument(d)
                    }*/
                    fileInfo.originPath = ktFile.path
                    fileInfo.path = ktFile.path
                    // todo 重要，这里需要进行一次analyze，将刚刚生成的kotlin文件导入编译系统，否则无法编译成字节码
                    // 进行一次空analyze就行
                    // idea中，这些大部分方法都是同步方法
                    analyze(newKtFile) {
                    }
                }

                tagJava(fileInfo)
            }
        }
    }

    override fun afterPush(project: Project, fileInfo: PushManager.FileInfo, device: Device, agreement: AdbAgreement) {
        tryDelete(fileInfo)
    }

    override fun onPushBreak(project: Project, fileInfo: PushManager.FileInfo, device: Device, agreement: AdbAgreement) {
        tryDelete(fileInfo)
    }

    private fun tryDelete(fileInfo: PushManager.FileInfo) {
        // 删除生成的kotlin代码
        if (isJava(fileInfo)) {
            val generatedKtFile = File(fileInfo.originPath)
            if (generatedKtFile.exists()) {
                WriteAction.runAndWait<Throwable> {
                    LocalFileSystem.getInstance().findFileByIoFile(generatedKtFile)?.delete(null)
                }
            }
        }
    }

    companion object {
        fun isJava(fileInfo: PushManager.FileInfo): Boolean {
            return fileInfo.tag.containsKey("isJava")
        }

        fun tagJava(fileInfo: PushManager.FileInfo) {
            fileInfo.tag["isJava"] = true
        }
    }
}