package com.example.viewdebugtrans.agreement

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import com.android.tools.idea.explorer.fs.FileTransferProgress
import com.android.tools.idea.layoutinspector.pipeline.adb.AdbUtils
import com.example.viewdebugtrans.execute
import com.example.viewdebugtrans.show
import com.example.viewdebugtrans.util.Utils
import com.example.viewdebugtrans.util.getPackageName
import com.example.viewdebugtrans.util.getViewDebugDir
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.sdk.AndroidSdkUtils
import org.jetbrains.android.sdk.AndroidSdkUtils.AdbSearchResult
import java.io.File
import java.util.*
import kotlin.collections.set
import kotlin.concurrent.thread

/**
 * 设备管理
 */
object AdbDevicesManager : AndroidDebugBridge.IDeviceChangeListener, ProjectManagerListener {

    private val projects = HashMap<Project, AdbSearchResult>()

    // AdbUtils.getAdbFuture(project).get()?.devices
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
    fun getAgreementFolder(project: Project): File {
        val file = File(project.getViewDebugDir(), "agreement")
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
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
     * 注意：如果设备没有root权限，则无法直接使用adb pull命令来复制data/data/pkg下的文件
     * 替代方案：通过adb run-as pkg 的方式来操作对应目录下的文件
     *  1. 先将文件复制到data/local/tmp目录下
     *  2. 再通过adb pull拉取文件
     *  3. 删除临时文件
     *  data/local/tmp 文件夹的特殊性
     *  1. 专门供adb使用的目录，adb可以直接读写
     *  2. 应用无法在该目录写入
     *
     *  为啥这么复杂，不在可读写的目录写入协议，因为不想依赖要外部存储权限，而且这个数据如果储存到外部，卸载后有残留
     *
     *
     *  借鉴了android插件内设备文件管理器的思路（直接用adbFileTransfer封装的逻辑将文件拷贝到电脑）
     */
    fun fetchRemoteAgreement(project: Project, device: Device) {
        val agreementFile = File(getAgreementFolder(project).absolutePath + "/" + device.id)
        if (agreementFile.exists()) {
            agreementFile.delete()
        }

        device.fileSystem.taskExecutor.executeAndAwait {
            getRemoteAgreement(project, device, getAgreementFolder(project).absolutePath + File.separator + device.id)
            val id = device.id
            val agreement = getAgreementFolder(project).listFiles()?.find { it.isFile && it.name == id }?.let {
                AdbAgreement.parse(Utils.stringToMap(it.readText()))
            }
            device.addAgreement(agreement)
        }
    }

    /**
     * 获取远程协议路径
     * 由于系统的多样性，大多数情况下，内部存储在data/data/pkg下，
     * 但是仍然有些系统用户数据不在这个目录下：
     * 比如data/user_de/uid/pkg
     */
    private fun getRemoteAgreement(project: Project, device: Device, dest: String) {
        val adb = getAdbPath(project) ?: getAnyAdbPath() ?: return show(null, "没有adb环境")
        AdbUtils.getAdbFuture(project).get()?.devices
        val userId = execute(arrayOf(adb, "-s", device.device.serialNumber, "shell", "am", " get-current-user")).trim()
        show(null, "userId = $userId")
        val result = LinkedList<String>()
        val pkgName = project.getPackageName() ?: return show(null, "包名null")
        if (userId == "0") {
            result.add("/data/data/${pkgName}/cache/viewDebug/agreement")
        }
        result.add("/data/user_de/$userId/${pkgName}/cache/viewDebug/agreement")
        // 目录都读取一遍
        for (p in result) {
            try {
                device.fileSystem.adbFileTransfer.downloadFileViaTempLocation(
                    p,
                    0L,
                    File(dest).toPath(),
                    object : FileTransferProgress {
                        override fun progress(currentBytes: Long, totalBytes: Long) {

                        }

                        override fun isCancelled(): Boolean {
                            return false
                        }
                    },
                    pkgName
                ).get()


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * device转换
     * 这个方法必须在运行的线程中执行
     */
    fun getDevice(device: IDevice): Device {
        return Device(getDeviceId(device.serialNumber), device.serialNumber, device.isOnline, device)
    }


    fun getDeviceId(device: String): String {
        val adbP = getAnyAdbPath() ?: return "--"
        return execute(arrayOf(adbP, "-s", device, "shell", "settings", "get", "secure", "android_id")).trim()
    }


    override fun deviceConnected(device: IDevice) {
        getDevice(device)
        thread {
            show(null, device.serialNumber +"-remove")
            devices.removeIf { it.serialNumber == device.serialNumber }
            try {
                val d = getDevice(device)
                show(null, d.serialNumber)
                devices.add(d)
                show(null, d.serialNumber +"-add")
                projects.forEach { project ->
                    requestAgreement(project.key, d)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                show(e)
            }


        }
    }

    override fun deviceDisconnected(device: IDevice) {
        devices.removeIf { it.serialNumber == device.serialNumber }
        show(null, device.serialNumber +"-remove")
    }

    override fun deviceChanged(device: IDevice, changeMask: Int) {
        if (changeMask or IDevice.CHANGE_STATE == IDevice.CHANGE_STATE) {
            thread {
                updateDevice(device)
            }
        }
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

    private fun updateDevice(iDevice: IDevice) {
        val id = getDeviceId(iDevice.serialNumber)
        devices.find { it.id == id }?.online = iDevice.isOnline
    }

    private fun requestAgreement(project: Project, device: Device) {
        // 获取app模块
        val appModules = ModuleManager.getInstance(project).sortedModules.filter {
            AndroidFacet.getInstance(it)?.configuration?.isAppProject == true
        }
        // 获取传输协议
        appModules.forEach { _ ->
            fetchRemoteAgreement(
                project,
                device = device
            )
        }
    }
}

