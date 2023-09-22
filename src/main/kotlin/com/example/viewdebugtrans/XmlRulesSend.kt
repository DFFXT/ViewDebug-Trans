package com.example.viewdebugtrans

import com.example.viewdebugtrans.agreement.AdbAgreement
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.CompilerModuleExtension
import java.io.File

/**
 * xml 布局规则文件推送
 */
class XmlRulesSend {
    fun send(project: Project, agreement: AdbAgreement) {
        val rulePathsSet = HashSet<String>()
        val logSet = LinkedHashSet<String>()
        ModuleManager.getInstance(project).modules.forEachIndexed { index, it ->
            //val path = CompilerModuleExtension.getInstance(it)?.compilerOutputPath?.path
            val basePath = CompilerModuleExtension.getInstance(it)?.compilerOutputPath?.path?.replace('\\','/')
            if (basePath != null) {
                val index = basePath.indexOf("/build/")
                if (index > 0) {
                    // xml文件，需要xml规则文件
                    val rulesPath =  basePath.substring(0, index) + "/build/intermediates/incremental"
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
                                    PushFileManager.pushFile(ruleFile.absolutePath, agreement.destDir + "/" + "merger-${index}.xml", "rules")
                                }
                            } else {
                                logSet.add("没有规则文件：$ruleFile")
                            }
                        } else {
                            logSet.add("没有规则文件：$rulesPath")
                        }
                    }
                }

            }
        }
        logSet.forEach {
            show(project, it)
        }
    }
}