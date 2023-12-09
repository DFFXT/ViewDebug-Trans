package com.example.viewdebugtrans

import com.android.tools.idea.projectsystem.AndroidModuleSystem
import com.android.tools.idea.projectsystem.androidProjectType
import com.android.tools.idea.projectsystem.getHolderModule
import com.example.viewdebugtrans.util.getPackageName
import com.intellij.execution.RunManagerEx
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessModuleDir
import com.intellij.openapi.project.modules
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

/**
 * xml 布局规则文件推送
 */
class XmlRulesFetch {
    fun getXmlRules(project: Project):HashSet<String> {
        val rulePathsSet = HashSet<String>()
        val logSet = LinkedHashSet<String>()
        val applicationPkg = project.getPackageName()
        val configuration = RunManagerEx.getInstanceEx(project).selectedConfiguration?.configuration
        var appModule: Module? = null
        if (configuration  is ModuleBasedConfiguration<*,*>) {
            appModule = configuration.configurationModule.module?.getHolderModule()
        }
        // 找到Android项目的根模块
        /*val appModule = project.getProjectSystem().submodules.find {
            val androidModule = AndroidModuleInfoProvider.getInstance(it) ?: return@find false
            return@find androidModule.isAndroidModule() && androidModule.getApplicationPackage() == applicationPkg
        }*/
        if (appModule == null) {
            show(project, "没有找到android主模块")
            return rulePathsSet
        } else if (appModule.androidProjectType() != AndroidModuleSystem.Type.TYPE_APP) {
            val autoSelect = project.modules.find { it.androidProjectType() == AndroidModuleSystem.Type.TYPE_APP }?.getHolderModule()
            show(project, "当前选择的不是app模块，请选择正确的运行模块")
            if (autoSelect != null) {
                appModule = autoSelect
                show(project, "当前选择的不是app模块，自动选择：${autoSelect.name}")
            }
        }
        /**
         *  这里使用这个方法，ProjectUtil里面的[Module.guessModuleDir]方法由于版本问题，无法返回正确的目录
         */
        fun Module.guessModuleDir(): VirtualFile? {
            val contentRoots = rootManager.contentRoots.filter { it.isDirectory }
            show(project, "select -> $name")
            show(project, "contentRoots ${contentRoots.joinToString(";") { it.name }}")
            show(project, "moduleFile?.parent ${moduleFile?.parent}")
            return contentRoots.find { it.name == name } ?: contentRoots.firstOrNull() ?: moduleFile?.parent
        }

        // 猜测模块的base dir；
        var basePath = appModule.guessModuleDir()?.path


        if (basePath != null) {
            if (basePath.endsWith("/src/main")) {
                basePath = basePath.substring(0, basePath.length - 9)
            }
            // xml文件，需要xml规则文件
            val rulesPath = "$basePath/build/intermediates/incremental"
            val ruleFileDir = File(rulesPath)

            /**
             * 所属规则文件并添加到集合
             */
            fun search(dir: File): Boolean {
                // 选择对应merge文件夹，过滤AndroidTestResources类型文件夹
                val folder = dir.listFiles()?.find { it.name.startsWith("merge") && it.name.endsWith("Resources") && !it.name.endsWith("AndroidTestResources") }
                if (folder != null) {
                    // 需要设置不同的名称
                    val ruleFile = File(folder, "merger.xml")
                    if (ruleFile.exists()) {
                        // 过滤相同文件
                        if (!rulePathsSet.contains(ruleFile.absolutePath)) {
                            rulePathsSet.add(ruleFile.absolutePath)
                            // 推送规则文件
                            show(project, "找到规则文件-----：$ruleFile")
                            return true
                            // PushFileManager.pushFile(ruleFile.absolutePath, agreement.destDir + "/" + "merger-${index}.xml", "rules")
                        }
                    } else {
                        logSet.add("没有规则文件：$ruleFile")
                    }
                } else {
                    logSet.add("不存在规则文件 $dir")
                }
                return false
            }

            if (ruleFileDir.exists()) {
                // incremental/mergeDebugResource
                if (!search(ruleFileDir)) {
                    // incremental/debug/mergeDebugResource
                    if (!search(File(rulesPath, "debug"))) {
                        // incremental/release/mergeReleaseResource
                        search(File(rulesPath, "release"))
                    }
                }
            } else {
                show(project, "null rulesPath（$basePath）不存在")
            }

        } else {
            show(project, "${appModule.name}模块目录没找到")
        }
        logSet.forEach {
            show(project, it)
        }
        show(project, "getXmlRules size:${rulePathsSet.size}")
        return rulePathsSet
    }
}