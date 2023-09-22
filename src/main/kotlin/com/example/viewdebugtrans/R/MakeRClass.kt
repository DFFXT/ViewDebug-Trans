package com.example.viewdebugtrans.R

import com.example.viewdebugtrans.show
import com.example.viewdebugtrans.socket.core.SimpleSocket
import com.intellij.openapi.roots.ModuleRootManager
import java.io.File

/**
 * com.example.skinswitch:style/Widget.AppCompat.ActionBar.TabBar = 0x7f100207
 * com.example.skinswitch:layout/abc_activity_chooser_view_list_item = 0x7f0b0007
 */
/**
 * 为了兼容非传递R，需要在当前模块下生成所有模块的R文件
 */
class MakeRClass {
    private var rClass: String? = null
    private var rFile = ArrayList<String>()
    fun make(module: com.intellij.openapi.module.Module, kotlinSrcPath: String, runnable: Runnable) {

        /*val dependencyModules = ModuleRootManager.getInstance(module).moduleDependencies
        val virtualFiles = ModuleRootManager.getInstance(module).contentRoots
        val dependModulePkgName = dependencyModules.map {
            getModulePackageName(it)
        }.filterNotNull().toMutableList()*/


        /*ApplicationManager.getApplication().invokeAndWait {
            val m = ModuleRootManager.getInstance(module).modifiableModel
            m.addContentEntry("file://D:\\Android\\idea projet\\ideaTestAndorid\\testroot").let {
                it.addSourceFolder("file://D:\\Android\\idea projet\\ideaTestAndorid\\testroot\\src", false)
            }
            m.commit()
        }*/


        // todo 通过这个api看看内部内添加 jar作为依赖，

        // MyProjectManager.getDxOrD8Path(module)
        //VirtualFileManager.getInstance().findFileById()
        /*JarFileSystem.getInstance()
        VirtualFileManager.getInstance()*/
        /*val p = "D:\\Android\\Project\\SkinSwitch\\app\\build\\intermediates\\compile_and_runtime_not_namespaced_r_class_jar\\debug\\R.jar!/"
        val url = "jar://$p"
        ModuleRootModificationUtil.addModuleLibrary(module, url)*/
        /*module.moduleWithDependenciesScope.accept()
        OrderEntryUtil.addLibraryToRoots()*/
        MyProjectManager.addRDependency(module, runnable)
        /*getModulePackageName(module)?.let {
            dependModulePkgName.add(it)
        }


        val rContent = try {
            SimpleSocket.requestMultiContent("request/R", dependModulePkgName.joinToString("\n").toByteArray(), 12348)
        } catch (e: Exception) {
            emptyList()
        }
        dependModulePkgName.forEachIndexed { index, s ->
            val rpath = virtualFiles[0].path + File.separatorChar + "java" + File.separatorChar + s.replace('.', File.separatorChar) + File.separatorChar + "R.java"
            show(null, "生成文件：$rpath")
            makeRFormString(String(rContent[index]), virtualFiles[0].path + File.separatorChar + "java" + File.separatorChar + s.replace('.', File.separatorChar) + File.separatorChar + "R.java", s)
        }*/

    }

    private fun getModulePackageName(module: com.intellij.openapi.module.Module): String? {
        val virtualFiles = ModuleRootManager.getInstance(module).contentRoots
        if (virtualFiles.isNotEmpty()) {
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
                val pkgName = str.substring(start + tag.length, end)
                return pkgName
                // val pkgPath = pkgName.replace('.', '/')
            }
        }
        return null
    }

    private fun makeRFormString(rString: String, toRPath: String, pkg: String) {
        val file = File(toRPath)
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        if (!file.exists()) {
            file.createNewFile()
        }
        file.writeText(rString)
        rFile.add(toRPath)
    }

    /*private fun makeR(e:AnActionEvent, rowRPath: String, toRPath: String, pkg: String) {
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
                        *//*if (index3 > index2) {
                            val name = it.substring(index2 + 1, index3)
                            val value = it.substring(index3 )//.toUInt(16)
                            items.add(Item(type, name, value))
                        }*//*
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
            DeviceConnect().actionPerformed(e)
        }
    }*/
    fun delete() {
        rClass?.let {
            File(it).delete()
            rClass = null
        }
    }

    private class Item(val type: String, val value: String)
}