package com.example.viewdebugtrans.agreement

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import com.android.tools.idea.layoutinspector.pipeline.adb.AdbUtils
import com.example.viewdebugtrans.execute
import com.example.viewdebugtrans.show
import com.example.viewdebugtrans.util.Utils
import com.example.viewdebugtrans.util.getPackageName
import com.example.viewdebugtrans.util.getViewDebugDir
import com.example.viewdebugtrans.util.launch
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.project.VetoableProjectManagerListener
import kotlinx.coroutines.Dispatchers
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.sdk.AndroidSdkUtils
import org.jetbrains.android.sdk.AndroidSdkUtils.AdbSearchResult
import org.jetbrains.kotlin.android.model.AndroidModuleInfoProvider
import java.io.File
import java.util.*
import kotlin.collections.set
import kotlin.concurrent.thread

/**
 * 设备管理
 */
object AdbDevicesManager : AndroidDebugBridge.IDeviceChangeListener, VetoableProjectManagerListener {

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
     * 获取所有打开的项目
     */
    fun getProjects(): Set<Project> {
        return projects.keys
    }

    /**
     * 协议目录
     */
    private fun getAgreementFolder(project: Project): File {
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
        return projects[project]?.adbPath?.absolutePath ?: AndroidSdkUtils.findAdb(project).adbPath?.absolutePath
    }

    /**
     * 返回一个可用adb路径
     */
    fun getAnyAdbPath(): String? {
        return projects.asSequence().find { (it.value.adbPath ?: AndroidSdkUtils.findAdb(it.key).adbPath) != null }?.value?.adbPath?.absolutePath
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
     */
    fun fetchRemoteAgreement(project: Project, device: Device, pkgName: String) {
        val adbP = getAnyAdbPath() ?: return
        val agreementFile = File(createAgreementFilePath(project, device, pkgName))
        if (agreementFile.exists()) {
            agreementFile.delete()
        }

        // 创建临时文件（不能run-as，run-as没权限）
        val tmp = "/data/local/tmp/$pkgName-agreement"
        execute(
            arrayOf(
                adbP,
                "-s",
                device.serialNumber,
                "shell",
                "touch",
                tmp
            )
        )
       /* if (device.isSuRoot()) {
            execute(arrayOf(adbP, "-s", device.serialNumber, "shell", "su", "0", "sh", "-c", "cp /data/data/$pkgName/cache/viewDebug/agreement /data/local/tmp/$pkgName-agreement"))
        }
        // 复制文件到data/local/tmp，需要run-as，不然没权限
        execute(
            arrayOf(
                adbP,
                "-s",
                device.serialNumber,
                "shell",
                "run-as",
                pkgName,
                "cp",
                "/data/data/$pkgName/cache/viewDebug/agreement",
                "/data/local/tmp/$pkgName-agreement"
            )
        )*/

        copyRemoteAgreement(project, device, tmp)
        // pull文件到电脑
        execute(
            arrayOf(
                adbP,
                "-s",
                device.serialNumber,
                "pull",
                "/data/local/tmp/$pkgName-agreement",
                agreementFile.absolutePath
            )
        )
        execute(arrayOf(
            adbP,
            "-s", device.serialNumber,"shell", "rm", "/data/local/tmp/$pkgName-agreement"
        ))
        if (agreementFile.exists()) {
            val agreement = AdbAgreement.parse(Utils.stringToMap(agreementFile.readText()))
            device.addAgreement(agreement)
            agreementFile.delete()
        }
    }

    private fun createAgreementFilePath(project: Project, device: Device, pkgName: String): String {
        return getAgreementFolder(project).absolutePath + File.separator + "agreement_" + device.serialNumber + "_" + pkgName
    }

    /**
     * 复制文件到dest
     * 由于不同设备的cache目录不一致，所以需要猜测
     * ？？是否可以在设备上安装一个临时apk来进行通信
     * @param dest 设备上的临时文件地址
     */
    private fun copyRemoteAgreement(project: Project, device: Device, dest: String) {
        val adb = getAdbPath(project) ?: getAnyAdbPath() ?: return show(null, "没有adb环境")
        AdbUtils.getAdbFuture(project).get()?.devices
        val cmdResult = execute(arrayOf(adb, "-s", device.serialNumber, "shell", "am", " get-current-user"))
        val userId = cmdResult.msg.trim()
        show(null, "userId = $userId")
        val result = LinkedList<String>()
        val pkgName = project.getPackageName() ?: return show(null, "包名null")
        if (userId == "0") {
            result.add("/data/data/${pkgName}/cache/viewDebug/agreement")
        }
        result.add("/data/user_de/$userId/${pkgName}/cache/viewDebug/agreement")
        // 目录都读取一遍
        if (device.isSuRoot()) {
            for (p in result) {
                execute(arrayOf(adb, "-s", device.serialNumber, "shell", "su", "0", "sh", "-c", "'cp $p $dest'"))
            }
        } else {
            // 复制文件到data/local/tmp，需要run-as，不然没权限
            for (p in result) {
                execute(
                    arrayOf(
                        adb,
                        "-s",
                        device.serialNumber,
                        "shell",
                        "run-as",
                        pkgName,
                        "cp",
                        p,
                        dest
                    )
                )
            }
        }
    }

    /**
     * device转换
     * 这个方法必须在运行的线程中执行
     */
    private fun createDevice(serialNumber: String, online: Boolean): Device {
        return Device(serialNumber, serialNumber, online)
    }

    /**
     * 根据序列号获取存在的设备
     */
    fun getDevice(serialNumber: String): Device? {
        return devices.find { it.serialNumber == serialNumber }
    }




    fun getDeviceId(device: String): String {
        val adbP = getAnyAdbPath() ?: return "--"
        val result = execute(arrayOf(adbP, "-s", device, "shell", "settings", "get", "secure", "android_id"))
        return if (!result.error) {
            result.msg
        } else {
            ""
        }
    }


    override fun deviceConnected(device: IDevice) {
        deviceConnected(device.serialNumber, device.isOnline)
    }

    private fun deviceConnected(serialNumber: String, online: Boolean) {
        val d = createDevice(serialNumber, online)
        devices.removeIf { it.serialNumber == serialNumber}
        devices.add(d)
        launch(Dispatchers.IO) {
            projects.forEach { project ->
                requestAgreement(project.key, d)
            }
        }

    }

    override fun deviceDisconnected(device: IDevice) {
        synchronized(devices) {
            devices.removeIf { it.serialNumber == device.serialNumber }
        }
    }

    override fun deviceChanged(device: IDevice, changeMask: Int) {
        if (changeMask or IDevice.CHANGE_STATE == IDevice.CHANGE_STATE) {
            deviceConnected(device)
        }
    }


    override fun projectOpened(project: Project) {
        projects[project] = AndroidSdkUtils.findAdb(project)
        launch(Dispatchers.IO) {
            synchronized(devices) {
                getDevices().forEach {
                    requestAgreement(project, it)
                }
            }
        }
    }

    override fun projectClosed(project: Project) {
        projects.remove(project)
    }

    override fun canClose(project: Project): Boolean {
        return true
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
                    project,
                    device = device,
                    pkgName
                )
            }
        }
    }
}

