package com.example.viewdebugtrans

import com.android.sdklib.devices.DeviceManager
import com.android.tools.idea.run.DeviceFutures
import com.example.viewdebugtrans.agreement.AdbDevicesManager
import com.example.viewdebugtrans.log.Logger
import com.example.viewdebugtrans.util.getPackageName
import com.example.viewdebugtrans.util.getViewDebugDir
import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.RunManagerListener
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.idea.refactoring.project
import java.io.File
import kotlin.collections.HashMap
import kotlin.collections.set

/**
 * 项目监听，设备监听，同时
 */
class ProjectListener : ProjectManagerListener {
    private val projectMap = HashMap<Project, ProjectTable>()
    override fun projectOpened(project: Project) {
        val table = ProjectTable()
        projectMap[project] = table
        AdbDevicesManager.projectOpened(project)
        // val disposable
        val connect = project.messageBus.connect()
        connect.subscribe(RunManagerListener.TOPIC, object : RunManagerListener {
            private fun addTsk(settings: RunnerAndConfigurationSettings?) {
                val c = settings?.configuration ?: return
                if (c.beforeRunTasks.find { it is SendRunSignalBeforeRunTask } == null) {
                    Logger.i("ProjectListener", "addTask")
                    c.beforeRunTasks = c.beforeRunTasks +  SendRunSignalBeforeRunTask()
                }
            }
            override fun runConfigurationSelected(settings: RunnerAndConfigurationSettings?) {
                addTsk(settings)
            }

            override fun runConfigurationAdded(settings: RunnerAndConfigurationSettings) {
                addTsk(settings)
            }

            override fun runConfigurationChanged(settings: RunnerAndConfigurationSettings) {
                addTsk(settings)
            }

        })
    }

    override fun projectClosed(project: Project) {
        AdbDevicesManager.projectClosed(project)
        projectMap.remove(project)
    }

    override fun projectClosing(project: Project) {
        AdbDevicesManager.projectClosing(project)
    }

    override fun projectClosingBeforeSave(project: Project) {
        AdbDevicesManager.projectClosingBeforeSave(project)
    }

    override fun canCloseProject(project: Project): Boolean {
        return AdbDevicesManager.canCloseProject(project)
    }

    private class ProjectTable {
        var runConfigurationDispose: Disposable? = null
    }

    class SendRunSignalBeforeRunTaskProvider : BeforeRunTaskProvider<SendRunSignalBeforeRunTask>() {
        companion object {
            val key = Key.create<SendRunSignalBeforeRunTask>("SendRunSignalBeforeRunTaskProvider.SendRunSignalBeforeRunTask")
        }

        override fun getId(): Key<SendRunSignalBeforeRunTask> = key

        override fun getName(): String = "clear push cache"

        override fun createTask(runConfiguration: RunConfiguration): SendRunSignalBeforeRunTask? {
            return SendRunSignalBeforeRunTask()
        }

        override fun executeTask(
            context: DataContext,
            configuration: RunConfiguration,
            environment: ExecutionEnvironment,
            task: SendRunSignalBeforeRunTask
        ): Boolean {
            // 通过断点查看源码，可以通过userData获取当前运行设备
            val devices = environment.getCopyableUserData(DeviceFutures.KEY).devices
            devices.map { AdbDevicesManager.getDevice(it.serial) }.forEach {
                if (it != null) {
                    Logger.i("ProjectListener", "executeTask start ${it.serialNumber}")
                    val project = context.project
                    val pkgName = project.getPackageName()
                    val agreement = it.getAgreement(pkgName)
                    val adbPath = AdbDevicesManager.getAdbPath(project)
                    if (agreement?.clearSignalFileName != null && !adbPath.isNullOrEmpty()) {
                        // 推送重新运行的信号文件
                        PushFileManager.init(project, it, agreement, adbPath)
                        PushFileManager.pushFile(target = getClearFile(project, agreement.clearSignalFileName),
                            dest = agreement.destDir, type = PushFileManager.TYPE_LAUNCH)
                        PushFileManager.pushApply(false)
                        Logger.i("ProjectListener", "executeTask end  ${it.serialNumber}")
                    } else {
                        Logger.i("ProjectListener", "executeTask skip ${it.serialNumber}")
                    }

                }

            }
            return true
        }
        private fun getClearFile(project: Project, clearFileName: String): String {
            val file = File(project.getViewDebugDir(), clearFileName)
            if (!file.exists()) {
                file.createNewFile()
            }
            return file.absolutePath
        }
    }

    class SendRunSignalBeforeRunTask :
        BeforeRunTask<SendRunSignalBeforeRunTask>(SendRunSignalBeforeRunTaskProvider.key) {

    }
}