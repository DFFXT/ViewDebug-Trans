package com.example.viewdebugtrans.socket.biz

import com.example.viewdebugtrans.show
import com.example.viewdebugtrans.socket.core.ResponseWriter
import com.google.gson.Gson
import com.intellij.ide.actions.OpenFileAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.impl.JavaPsiFacadeEx
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

/**
 * content：
 * {
 *  type:"class|xml",
 *  name:"fileName|className"
 * }
 */
internal class BizRequestOpenFile(project: Project): BizRoute(project) {
    companion object {
        val gson = Gson()
    }
    override fun onRequest(routeId: String, content: String, response: ResponseWriter) {
        val ct = gson.fromJson(content, Content::class.java)
        show(project, content)
        ApplicationManager.getApplication().invokeLater {
            var vf:VirtualFile? = null
            if (ct.type == "xml") {
                val files = FilenameIndex.getVirtualFilesByName(ct.name, GlobalSearchScope.allScope(project))
                vf = files.firstOrNull()
            } else if(ct.type == "class") {
                vf = JavaPsiFacadeEx.getInstance(project).findClass(ct.name, GlobalSearchScope.allScope(project))?.containingFile?.virtualFile
            }
            if (vf != null) {
                OpenFileAction.openFile(vf, project)
            } else {
                show(project, "没有找到这个文件：${ct.name}")
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

    private class Content(val type: String, val name: String)
}