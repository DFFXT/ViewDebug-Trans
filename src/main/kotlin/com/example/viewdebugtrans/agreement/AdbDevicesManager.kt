package com.example.viewdebugtrans.agreement

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import com.android.tools.idea.adb.wireless.AdbDevice
import com.example.viewdebugtrans.Config
import com.example.viewdebugtrans.execute
import com.example.viewdebugtrans.util.Utils
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.sdk.AndroidSdkUtils
import org.jetbrains.android.sdk.AndroidSdkUtils.AdbSearchResult
import org.jetbrains.kotlin.android.model.AndroidModuleInfoProvider
import java.io.File
import kotlin.concurrent.thread
object AdbDevicesManager : AndroidDebugBridge.IDeviceChangeListener, ProjectManagerListener {

    private val projects = HashMap<Project, AdbSearchResult>()
    private val devices = ArrayList<Device>()

    init {
        AndroidDebugBridge.addDeviceChangeListener(this)
    }
    /**
     * 获取设备列表
     */
    fun getDevices(): List<Device> {
        return devices
    }

    /**
     * 协议目录
     */
    fun getAgreementFolder(): File {
        val file = File(Config.getIdeaFolder(), "agreement")
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    /**
     * 保存设备的推送协议
     */
    fun saveDeviceAgreement(device: String, agreement: Map<String, String>) {
        //adb -s 127.0.0.1:5561 shell settings get secure android_id
        val id = AndroidDebugBridge.getBridge().devices.find { it.serialNumber == device }?.getProperty("net.hostname")
        val pkgName = agreement["pkgName"]?.replace('.', '_')
        val file = File(getAgreementFolder(), id + "_" + pkgName)
        file.writeText(Utils.mapToString(agreement))
    }

    /**
     * 获取设备对应的协议
     */
    fun getDeviceAgreement(device: Device) {
        val id = device.id
        val agreement =  getAgreementFolder().listFiles()?.find { it.isFile && it.name == id }?.let {
            AdbAgreement.parse(Utils.stringToMap(it.readText()))
        }
        device.addAgreement(agreement)
    }

    /**
     * 获取adb路径
     */
    fun getAdbPath(project: Project): String? {
        return projects[project]?.adbPath?.absolutePath
    }

    /**
     * 返回一个可用adb路径
     */
    fun getAnyAdbPath(): String? {
        return projects.asSequence().find { it.value.adbPath != null }?.value?.adbPath?.absolutePath
    }

    /**
     * 获取远程协议，远程协议默认在
     * data/data/pkgName/cache/viewDebug/agreement
     * 更新到：
     * .idea/viewDebug/agreement/deviceId
     */
    fun fetchRemoteAgreement(device: Device, pkgName: String) {
        val adbP = getAnyAdbPath() ?: return
        val agreementFile = File(getAgreementFolder().absolutePath + "/" + device.id)
        if (agreementFile.exists()) {
            agreementFile.delete()
        }
        execute(
            arrayOf(
                adbP,
                "-s",
                device.serialNumber,
                "pull",
                "/data/data/$pkgName/cache/viewDebug/agreement",
                getAgreementFolder().absolutePath + File.separator + device.id
            )
        )
        val id = device.id
        val agreement =  getAgreementFolder().listFiles()?.find { it.isFile && it.name == id }?.let {
            AdbAgreement.parse(Utils.stringToMap(it.readText()))
        }
        device.addAgreement(agreement)
    }

    /**
     * device转换
     * 这个方法必须在运行的线程中执行
     */
    fun getDevice(device: IDevice): Device {
        return Device(getDeviceId(device.serialNumber), device.serialNumber)
    }


    fun getDeviceId(device: String): String {
        val adbP = getAnyAdbPath() ?: return "--"
        return execute(arrayOf(adbP, "-s", device, "shell", "settings", "get", "secure", "android_id")).trim()
    }


    override fun deviceConnected(device: IDevice) {
        thread {
            devices.removeIf { it.serialNumber == device.serialNumber }
            val d = getDevice(device)
            devices.add(d)
            projects.forEach { project ->
                requestAgreement(project.key, d)
            }

        }
    }

    override fun deviceDisconnected(device: IDevice) {
        devices.removeIf { it.serialNumber == device.serialNumber }
    }

    override fun deviceChanged(device: IDevice, changeMask: Int) {
        // TODO("Not yet implemented")
    }

    override fun projectOpened(project: Project) {
        projects[project] = AndroidSdkUtils.findAdb(project)
        thread {
            getDevices().forEach {
                requestAgreement(project, it)
            }
        }


    }

    override fun projectClosed(project: Project) {
        projects.remove(project)
    }

    private fun requestAgreement(project: Project, device: Device) {
        // 获取app模块
        val appModules = ModuleManager.getInstance(project).sortedModules.filter {
            AndroidFacet.getInstance(it)?.configuration?.isAppProject == true
        }
        // 获取传输协议
        appModules.forEach {
            val pkgName = AndroidModuleInfoProvider.getInstance(it)?.getApplicationPackage()
            if (pkgName != null) {
                fetchRemoteAgreement(
                    device = device,
                    pkgName
                )
            }
        }
    }
}

