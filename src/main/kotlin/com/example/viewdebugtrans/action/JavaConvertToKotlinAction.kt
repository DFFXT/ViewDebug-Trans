package com.example.viewdebugtrans.action

import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ex.MessagesEx
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.actions.JavaToKotlinAction
import org.jetbrains.kotlin.idea.base.codeInsight.pathBeforeJavaToKotlinConversion
import org.jetbrains.kotlin.idea.base.resources.KotlinBundle
import org.jetbrains.kotlin.idea.configuration.ExperimentalFeatures
import org.jetbrains.kotlin.idea.j2k.IdeaJavaToKotlinServices
import org.jetbrains.kotlin.idea.j2k.J2kPostProcessor
import org.jetbrains.kotlin.idea.statistics.ConversionType
import org.jetbrains.kotlin.idea.statistics.J2KFusCollector
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.j2k.FilesResult
import org.jetbrains.kotlin.j2k.J2kConverterExtension
import org.jetbrains.kotlin.j2k.OldJavaToKotlinConverter
import org.jetbrains.kotlin.psi.KtFile
import java.io.IOException
import kotlin.io.path.notExists
import kotlin.system.measureTimeMillis

/**
 * 将java转换成kotlin代码,具体参见[JavaToKotlinAction]
 */
object JavaToKotlin {

    private fun uniqueKotlinFileName(javaFile: VirtualFile): String {
        val nioFile = javaFile.fileSystem.getNioPath(javaFile)

        var i = 0
        while (true) {
            val fileName = javaFile.nameWithoutExtension + (if (i > 0) i else "") + ".kt"
            if (nioFile == null || nioFile.resolveSibling(fileName).notExists()) return fileName
            i++
        }
    }

    val title = KotlinBundle.message("action.j2k.name")

    private fun saveResults(javaFiles: List<PsiJavaFile>, convertedTexts: List<String>): List<VirtualFile> {
        val result = ArrayList<VirtualFile>()
        for ((psiFile, text) in javaFiles.zip(convertedTexts)) {
            try {
                val document = PsiDocumentManager.getInstance(psiFile.project).getDocument(psiFile)
                val errorMessage = when {
                    document == null -> KotlinBundle.message("action.j2k.error.cant.find.document", psiFile.name)
                    !document.isWritable -> KotlinBundle.message("action.j2k.error.read.only", psiFile.name)
                    else -> null
                }
                if (errorMessage != null) {
                    val message = KotlinBundle.message("action.j2k.error.cant.save.result", errorMessage)
                    MessagesEx.error(psiFile.project, message).showLater()
                    continue
                }
                document!!.replaceString(0, document.textLength, text)
                FileDocumentManager.getInstance().saveDocument(document)

                val virtualFile = psiFile.virtualFile
                if (ScratchRootType.getInstance().containsFile(virtualFile)) {
                    val mapping = ScratchFileService.getInstance().scratchesMapping
                    mapping.setMapping(virtualFile, KotlinFileType.INSTANCE.language)
                } else {
                    val fileName = uniqueKotlinFileName(virtualFile)
                    virtualFile.pathBeforeJavaToKotlinConversion = virtualFile.path
                    virtualFile.rename(this, fileName)
                }
                result += virtualFile
            } catch (e: IOException) {
                MessagesEx.error(psiFile.project, e.message ?: "").showLater()
            }
        }
        return result
    }

    /**
     * For binary compatibility with third-party plugins.
     */
    fun convertFiles(
        files: List<PsiJavaFile>,
        project: Project,
        module: Module,
        enableExternalCodeProcessing: Boolean = true,
        askExternalCodeProcessing: Boolean = true,
        forceUsingOldJ2k: Boolean = false
    ): List<String> = convertFiles(
        files,
        project,
        module,
        enableExternalCodeProcessing,
        askExternalCodeProcessing,
        forceUsingOldJ2k,
        ConverterSettings.defaultSettings
    )

    fun convertFiles(
        files: List<PsiJavaFile>,
        project: Project,
        module: Module,
        enableExternalCodeProcessing: Boolean = true,
        askExternalCodeProcessing: Boolean = true,
        forceUsingOldJ2k: Boolean = false,
        settings: ConverterSettings = ConverterSettings.defaultSettings
    ): List<String> {
        val javaFiles = files.filter { it.virtualFile.isWritable }.ifEmpty { return emptyList() }
        var converterResult: FilesResult? = null
        fun convert() {
            val converter =
                if (forceUsingOldJ2k) OldJavaToKotlinConverter(
                    project,
                    settings,
                    IdeaJavaToKotlinServices
                ) else J2kConverterExtension.extension(useNewJ2k = ExperimentalFeatures.NewJ2k.isEnabled)
                    .createJavaToKotlinConverter(
                        project,
                        module,
                        settings,
                        IdeaJavaToKotlinServices
                    )
            converterResult = converter.filesToKotlin(
                javaFiles,
                if (forceUsingOldJ2k) J2kPostProcessor(formatCode = true)
                else J2kConverterExtension.extension(useNewJ2k = ExperimentalFeatures.NewJ2k.isEnabled)
                    .createPostProcessor(formatCode = true),
                progress = ProgressManager.getInstance().progressIndicator!!
            )
        }

        fun convertWithStatistics() {
            val conversionTime = measureTimeMillis {
                convert()
            }
            val linesCount = runReadAction {
                javaFiles.sumOf { StringUtil.getLineBreakCount(it.text) }
            }

            J2KFusCollector.log(
                ConversionType.FILES,
                ExperimentalFeatures.NewJ2k.isEnabled,
                conversionTime,
                linesCount,
                javaFiles.size
            )
        }


        if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(
                ::convertWithStatistics,
                title,
                true,
                project
            )
        ) return emptyList()


        var externalCodeUpdate: ((List<KtFile>) -> Unit)? = null

        val result = converterResult ?: return emptyList()
        val externalCodeProcessing = result.externalCodeProcessing
        if (enableExternalCodeProcessing && externalCodeProcessing != null) {
            val question = KotlinBundle.message("action.j2k.correction.required")
            if (!askExternalCodeProcessing /*|| (Messages.showYesNoDialog(
                    project,
                    question,
                    title,
                    Messages.getQuestionIcon()
                ) == Messages.YES)*/
            ) {
                ProgressManager.getInstance().runProcessWithProgressSynchronously(
                    {
                        runReadAction {
                            externalCodeUpdate = externalCodeProcessing.prepareWriteOperation(
                                ProgressManager.getInstance().progressIndicator!!
                            )
                        }
                    },
                    title,
                    true,
                    project
                )
            }
        }

        return result.results
        /*        return project.executeWriteCommand(KotlinBundle.message("action.j2k.task.name"), null) {
                    CommandProcessor.getInstance().markCurrentCommandAsGlobal(project)

                    val newFiles = saveResults(javaFiles, result.results)
                        .map { it.toPsiFile(project) as KtFile }
                        .onEach { it.commitAndUnblockDocument() }

                    externalCodeUpdate?.invoke(newFiles)

                    PsiDocumentManager.getInstance(project).commitAllDocuments()

                    newFiles.singleOrNull()?.let {
                        FileEditorManager.getInstance(project).openFile(it.virtualFile, true)
                    }

                    newFiles
                }*/
    }
}