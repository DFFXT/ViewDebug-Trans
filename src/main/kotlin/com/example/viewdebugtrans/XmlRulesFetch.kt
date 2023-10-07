package com.example.viewdebugtrans

import com.example.viewdebugtrans.util.getPackageName
import com.intellij.execution.RunManagerEx
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessModuleDir
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
            appModule = configuration.configurationModule.module
        }
        // 找到Android项目的根模块
        /*val appModule = project.getProjectSystem().submodules.find {
            val androidModule = AndroidModuleInfoProvider.getInstance(it) ?: return@find false
            return@find androidModule.isAndroidModule() && androidModule.getApplicationPackage() == applicationPkg
        }*/
        if (appModule == null) {
            show(project, "没有找到android主模块")
            return rulePathsSet
        }
        /**
         *  这里使用这个方法，ProjectUtil里面的[Module.guessModuleDir]方法由于版本问题，无法返回正确的目录
         */
        fun Module.guessModuleDir(): VirtualFile? {
            val contentRoots = rootManager.contentRoots.filter { it.isDirectory }
            return contentRoots.find { it.name == name } ?: contentRoots.firstOrNull() ?: moduleFile?.parent
        }

        // 猜测模块的base dir；
        val basePath = appModule.guessModuleDir()?.path


        if (basePath != null) {
            // xml文件，需要xml规则文件
            val rulesPath = "$basePath/build/intermediates/incremental"
            val ruleFileDir = File(rulesPath)
            if (ruleFileDir.exists()) {
                // 选择对应merge文件夹，过滤AndroidTestResources类型文件夹
                val folder = ruleFileDir.listFiles()?.find { it.name.startsWith("merge") && it.name.endsWith("Resources") && !it.name.endsWith("AndroidTestResources") }
                if (folder != null) {
                    // 需要设置不同的名称
                    val ruleFile = File(folder, "merger.xml")
                    if (ruleFile.exists()) {
                        // 过滤相同文件
                        if (!rulePathsSet.contains(ruleFile.absolutePath)) {
                            rulePathsSet.add(ruleFile.absolutePath)
                            // 推送规则文件
                            show(project, "找到规则文件-----：$ruleFile")
                            // PushFileManager.pushFile(ruleFile.absolutePath, agreement.destDir + "/" + "merger-${index}.xml", "rules")
                        }
                    } else {
                        logSet.add("没有规则文件：$ruleFile")
                    }
                } else {
                    logSet.add("没有规则文件：$rulesPath")
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