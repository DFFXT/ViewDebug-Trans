package com.example.viewdebugtrans

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import com.example.viewdebugtrans.agreement.AdbDevicesManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.android.model.AndroidModuleInfoProvider
import kotlin.concurrent.thread

/**
 * 项目监听，设备监听，同时
 */
class ProjectListener : ProjectManagerListener {

    override fun projectOpened(project: Project) {
        AdbDevicesManager.projectOpened(project)
    }

    override fun projectClosed(project: Project) {
        AdbDevicesManager.projectClosed(project)
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
}