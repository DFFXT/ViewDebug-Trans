package com.example.viewdebugtrans

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil
import com.intellij.psi.util.PsiUtilBase
import com.intellij.psi.util.PsiUtilCore
import com.intellij.ui.awt.RelativePoint


@Deprecated("android studio上没有Bytecode Viewer这个插件 ")
class JavaByteCode {


    fun getByteCode(e: AnActionEvent) {

        val dataContext: DataContext = e.dataContext
        val project: Project = e.project ?: return
        val editor: Editor = e.getData(CommonDataKeys.EDITOR)!!

        val psiElement: PsiElement = getPsiElement(dataContext, project, editor) ?: return

        // Some PSI elements could be multiline. Try to be precise about the line we were invoked at.

        // Some PSI elements could be multiline. Try to be precise about the line we were invoked at.




        val virtualFile: VirtualFile = PsiUtilCore.getVirtualFile(psiElement) ?: return


        val element: SmartPsiElementPointer<*> =
            SmartPointerManager.getInstance(project).createSmartPsiElementPointer(psiElement)

        val myByteCode = ReadAction.compute<Any, Exception> {
            val targetElement: PsiElement? = element.element
            // todo
            //if (targetElement != null) ByteCodeViewerManager.getByteCode(targetElement) else null
            val f = 0
        }
    }

    private fun getPsiElement(dataContext: DataContext, project: Project, editor: Editor?): PsiElement? {
        var psiElement: PsiElement? = null
        if (editor == null) {
            psiElement = dataContext.getData(CommonDataKeys.PSI_ELEMENT);
        } else {
            val file = PsiUtilBase.getPsiFileInEditor(editor, project);
            val injectedEditor = InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, file);
            psiElement = findElementInFile(PsiUtilBase.getPsiFileInEditor(injectedEditor, project), injectedEditor);

            if (file != null && psiElement == null) {
                psiElement = findElementInFile(file, editor);
            }
        }

        return psiElement
    }

    private fun  findElementInFile(psiFile: PsiFile?, editor:Editor?): PsiElement?
    {
        psiFile ?: return null
        editor ?: return null
        return psiFile.findElementAt(editor.getCaretModel().getOffset())
    }
}
