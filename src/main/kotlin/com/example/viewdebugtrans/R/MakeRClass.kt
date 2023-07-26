package com.example.viewdebugtrans.R

import com.example.viewdebugtrans.Config
import com.example.viewdebugtrans.DestRAction
import com.example.viewdebugtrans.show
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File
import java.io.FileWriter
import java.util.*

/**
 * com.example.skinswitch:style/Widget.AppCompat.ActionBar.TabBar = 0x7f100207
 * com.example.skinswitch:layout/abc_activity_chooser_view_list_item = 0x7f0b0007
 */
class MakeRClass {
    private var rClass: String? = null
    fun make(e: AnActionEvent, kotlinSrcPath: String) {
        val rPath = Config.RFilePath ?: return
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val rFile = "${project.basePath}/$rPath"
        if (!File(rFile).exists()) {
            show(project, "没有指定R文件位置")
            return
        }
        val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(File(kotlinSrcPath))
        if (virtualFile != null) {
            val module = ModuleUtil.findModuleForFile(virtualFile, project)
            if (module != null) {
                val virtualFiles = ModuleRootManager.getInstance(module).contentRoots
                if (virtualFiles.isNotEmpty()) {
                    val rootPath = virtualFiles[0].path
                    var manifest: File? = null
                    // 从目录下找AndroidManifest文件
                    for (f in virtualFiles) {
                        manifest = File(f.path, "AndroidManifest.xml")
                        if (manifest.exists()) {
                            break
                        }
                    }
                    if (manifest?.exists() == true) {
                        val str = String(manifest.readBytes())
                        val tag = "package=\""
                        val start = str.indexOf(tag)
                        val end = str.indexOf("\"", start + tag.length)
                        val pkgName = str.substring(start+ tag.length, end)
                        val pkgPath = pkgName.replace('.', '/')
                        show(project, "读取包名：${manifest.absolutePath}")
                        makeR(e, rFile, "${manifest.parentFile!!.absolutePath}/java/$pkgPath", pkgName)
                    } else {
                        for (f in virtualFiles) {
                            show(project, "该目录下没有AndroidManifest.xml文件：${f.path}    ${manifest?.absolutePath}")
                        }
                    }
                } else {
                    show(project, "virtualFiles empty")
                }
            } else {
                show(project, "module null")
            }
        } else {
            show(project, "virtualFile null")
        }

    }

    private fun makeR(e:AnActionEvent, rowRPath: String, toRPath: String, pkg: String) {
        val rFile = File(rowRPath)

        //val pkgPath = configPkgPath.substring(index + "Android/data/".length)
        val toPath = File(toRPath)
        if (rFile.exists()) {
            val items = LinkedList<Item>()
            rFile.readLines().forEach {
                if (it.startsWith("int" )) {
                    val arr = it.split(' ').toMutableList()
                    val type = arr[1]

                    if (arr[3] == "{") {
                        arr[3] = "={"
                    } else {
                        arr.add(3, "=")
                    }
                    arr.add(";")
                    arr.removeAt(1)
                    val value = arr.joinToString(separator = " ") { it }
                    items.add(Item(type, value))
                } else {
                    show(e.project!!, "R文件格式错误$rowRPath")
                    return
                    var index1 = it.indexOf(":")
                    val index2 = it.indexOf("/")
                    if (index1 in 0 until index2) {
                        val type = it.substring(index1 + 1, index2)
                        val index3 = it.indexOf(" = ")
                        /*if (index3 > index2) {
                            val name = it.substring(index2 + 1, index3)
                            val value = it.substring(index3 )//.toUInt(16)
                            items.add(Item(type, name, value))
                        }*/
                        items.add(Item(type, it.substring(index2 + 1).replace('.', '_')))
                    }
                }

            }
            if (items.isNotEmpty()) {
                val types = HashMap<String, StringBuilder>()
                if (!toPath.exists()) {
                    toPath.mkdirs()
                }
                val rClass = File(toPath, "R.java")
                this.rClass = rClass.absolutePath

                FileWriter(rClass).use { os ->
                    val breakLine = "\n"
                    items.forEach {
                        var typeBuilder = types[it.type]
                        if (typeBuilder == null) {
                            typeBuilder = StringBuilder()
                            types[it.type] = typeBuilder
                        }
                        typeBuilder.append("public final static ")
                        typeBuilder.append(it.value)
                        typeBuilder.append(breakLine)
                    }
                    os.write("package $pkg;\n")
                    os.write("public class R {\n")
                    types.forEach {
                        if (it.value.isNotEmpty()) {
                            os.write("public static class ${it.key} {")
                            os.write(breakLine)
                            os.write(it.value.toString())
                            os.write(breakLine)
                            os.write("}")
                            os.write(breakLine)
                        }
                    }
                    os.write("}")
                    show(project = e.project!!, "R文件：${rClass.absolutePath}")
                }
            }
        } else {
            DestRAction().actionPerformed(e)
        }
    }
    fun delete() {
        rClass?.let {
            File(it).delete()
        }
    }

    private class Item(val type: String, val value: String)
}