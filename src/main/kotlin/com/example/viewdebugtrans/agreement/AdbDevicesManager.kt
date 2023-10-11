package com.example.viewdebugtrans.agreement

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
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
import org.jetbrains.kotlin.android.model.AndroidModuleInfoProvider
import java.io.File
import java.util.*
import kotlin.collections.set
import kotlin.concurrent.thread

/**
 * 设备管理
 */
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
    fun getAgreementFolder(project: Project): File {
        val file = File(project.getViewDebugDir(), "agreement")
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    /**
     * 保存设备的推送协议
     */
   /* fun saveDeviceAgreement(device: String, agreement: Map<String, String>) {
        //adb -s 127.0.0.1:5561 shell settings get secure android_id
        val id = AndroidDebugBridge.getBridge().devices.find { it.serialNumber == device }?.getProperty("net.hostname")
        val pkgName = agreement["pkgName"]?.replace('.', '_')
        val file = File(getAgreementFolder(), id + "_" + pkgName)
        file.writeText(Utils.mapToString(agreement))
    }*/

    /**
     * 获取设备对应的协议
     */
    /*fun getDeviceAgreement(device: Device) {
        val id = device.id
        val agreement =  getAgreementFolder().listFiles()?.find { it.isFile && it.name == id }?.let {
            AdbAgreement.parse(Utils.stringToMap(it.readText()))
        }
        device.addAgreement(agreement)
    }*/

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
        val agreementFile = File(getAgreementFolder(project).absolutePath + "/" + device.id)
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
                (getAgreementFolder(project).absolutePath + File.separator + device.id).replace('\\','/')
            )
        )
        execute(arrayOf(
            adbP,
            "-s", device.serialNumber,"shell", "rm", "/data/local/tmp/$pkgName-agreement"
        ))
        val id = device.id
        val agreement =  getAgreementFolder(project).listFiles()?.find { it.isFile && it.name == id }?.let {
            AdbAgreement.parse(Utils.stringToMap(it.readText()))
        }
        device.addAgreement(agreement)
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
    fun getDevice(device: IDevice): Device {
        return Device(getDeviceId(device.serialNumber), device.serialNumber, device.isOnline)
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

