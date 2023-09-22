package com.example.viewdebugtrans.R

import com.android.SdkConstants
import com.example.viewdebugtrans.show
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.*
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.android.util.AndroidBuildCommonUtils
import kotlin.concurrent.thread

object MyProjectManager {

    private val rModuleName = "module-R-tmp"

    private fun getRootModule(module: com.intellij.openapi.module.Module): com.intellij.openapi.module.Module {
        val modules = ModuleManager.getInstance(module.project).sortedModules
        val rootModule = modules.get(modules.size - 2)
        return rootModule
    }

    private fun getRDependencyUrl(module: com.intellij.openapi.module.Module): List<VirtualFile> {
        // getIn: 获取依赖的模块
        // getOut: 获取自己被哪些依赖
        val rootModule = getRootModule(module)

        val rJarList = ModuleRootManager.getInstance(rootModule).orderEntries().classesRoots.filter {

            // 名称必须是R.jar，排除AndroidTest类型的目录| 排除UnitTest类型目录
            it.name == "R.jar" &&
                    !it.path.endsWith("AndroidTest/R.jar!/") &&
                    !it.path.endsWith("UnitTest/R.jar!/") &&
                    it.path.contains("compile_and_runtime_not_namespaced_r_class_jar")
        }
        if (rJarList.isEmpty()) {
            show(null, "没有R依赖，或许需要一次完整构建：${rootModule.name}")
        }
        return rJarList
    }

    fun addRDependency(module: com.intellij.openapi.module.Module, runnable: Runnable) {
        /*if (!init) {
            val connection =  module.project.messageBus.connect()
            connection.subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
                override fun rootsChanged(event: ModuleRootEvent) {
                    connection.disconnect()
                    *//*thread {
                        DumbService.getInstance(module.project).runReadActionInSmartMode(runnable)
                    }*//*

                }
                override fun beforeRootsChange(event: ModuleRootEvent) {
                    val f = 0
                }
            })
        }*/
        /* ModuleRootManager.getInstance(module).modifiableModel.let {
             it.moduleLibraryTable.libraries[0].modifiableModel.let {
                 it.addJarDirectory("file://D:\\Android\\idea projet\\ideaTestAndorid\\testroot", false)
                 it.commit()
             }
         }*/

        // 添加依赖
        /*ModuleRootModificationUtil.addModuleLibrary(
            module,
            "module-R-tmp",
            getRDependencyUrl(module).map { it.url },
            emptyList()
        )*/
        // 依赖重排序
        ModuleRootManager.getInstance(module).modifiableModel.let {
            // 创建一个library对象
            val lib = it.moduleLibraryTable.createLibrary(rModuleName)
            // 获取library可变模型
            val libModifiableModel = lib.modifiableModel
            // 获取当前模块的R文件依赖
            val jars = getRDependencyUrl(module)
            jars.map { it.url }.forEach {
                show(null, "找到R依赖：$it")
                // 给创建的library添加root目录，类型为字节码
                libModifiableModel.addRoot(it, OrderRootType.CLASSES)
            }
            if (jars.isEmpty()) {
                show(null, "没有找到R依赖")
            }
            // 获取library对相应的orderEntry对象
            val orderEntry = it.findLibraryOrderEntry(lib)!!
            // 设置scope属性
            orderEntry.scope = DependencyScope.COMPILE
            // 设置exported属性
            orderEntry.isExported = false
            val allOrderEntry = ArrayList<OrderEntry>()
            // 遍历获取当前module所以依赖
            it.orderEntries().forEach { entry ->
                allOrderEntry.add(entry)
                true
            }
            // 当创建的library对应的orderEntry对象添加到第二个位置（source代码之前，这样R文件才会覆盖）
            allOrderEntry.add(1, orderEntry)
            // 重新对orderEntry进行排序
            it.rearrangeOrderEntries(allOrderEntry.toTypedArray())
            // 使用runWriteAction方法执行commit，对修改进行应用
            ApplicationManager.getApplication().runWriteAction {
                libModifiableModel.commit()
                it.commit()
            }
            thread {
                // 新开线程，确保不阻塞原线程（runReadActionInSmartMode会阻塞当前线程）
                DumbService.getInstance(module.project).runReadActionInSmartMode {
                    try {
                        runnable.run()
                    } catch (e: Exception) {
                        show(e)
                    } finally {
                        removeRDependency(module)
                    }
                }
            }
        }
    }

    /**
     * 移除依赖
     */
    fun removeRDependency(module: com.intellij.openapi.module.Module) {
        // IDEA有读线程和写线程，invokeLater会切换到写线程
        ApplicationManager.getApplication().invokeLater {
            ModuleRootModificationUtil.updateModel(module) {
                val lib = it.moduleLibraryTable.getLibraryByName(rModuleName)
                if (lib != null) {
                    it.moduleLibraryTable.removeLibrary(lib)
                }
            }
        }
    }

    fun getDxOrD8Path(module: com.intellij.openapi.module.Module): String {
        val root = getRootModule(module)
        // 获取adb路径
        AndroidBuildCommonUtils.platformToolPath(SdkConstants.FN_ADB)
        // dx
        //AndroidBuildCommonUtils.platformToolPath(SdkConstants.FN_DX)
        //AndroidFacet.getInstance(getRootModule(module)).configuration.isAppProject
        //AndroidDebugBridge.addClientChangeListener()
        return ""
    }
}