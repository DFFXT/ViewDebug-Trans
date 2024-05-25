package com.example.viewdebugtrans.socket.biz

import com.example.viewdebugtrans.socket.core.ResponseWriter
import com.intellij.ide.actions.OpenFileAction
import com.intellij.ide.actions.RevealFileAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.io.File

internal class BizRequestOpenFile(project: Project): BizRoute(project) {
    override fun onRequest(routeId: String, content: String, response: ResponseWriter) {
        ApplicationManager.getApplication().invokeLater {
            val files = FilenameIndex.getVirtualFilesByName(content, GlobalSearchScope.allScope(project))
            if (files.isNotEmpty()) {
                OpenFileAction.openFile(files.first(), project)
            }
        }
        response.writeEmpty200Ok()
        /*EditorActionManager.getInstance()
        EditorFactory.getInstance().createDocument("")
        RevealFileAction.openFile(File(""))
        PsiDocumentManager.getInstance(project).
        OpenFileDescriptor(project, null, 0).navigateIn()
        FileDocumentManager.getInstance().*/

    }
}