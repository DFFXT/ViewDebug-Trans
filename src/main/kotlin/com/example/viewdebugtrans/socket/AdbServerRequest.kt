package com.example.viewdebugtrans.socket

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.Client
import com.example.viewdebugtrans.ProjectListener
import com.example.viewdebugtrans.agreement.AdbAgreement
import com.example.viewdebugtrans.agreement.AdbDevicesManager
import com.example.viewdebugtrans.execute
import com.example.viewdebugtrans.show
import com.example.viewdebugtrans.socket.biz.BizRequestHeartRoute
import com.example.viewdebugtrans.socket.biz.BizRequestOpenFile
import com.example.viewdebugtrans.socket.core.Callback
import com.example.viewdebugtrans.socket.core.ProjectAdbClientSocket
import com.example.viewdebugtrans.socket.core.ProjectAdbServerSocket
import com.example.viewdebugtrans.util.getPackageName
import com.intellij.openapi.project.Project
import java.net.ServerSocket

/**
 * adb请求
 * 需要监听device中的进程情况，如果进程被杀死然后重新打开，需要重新建立连接
 */
class AdbServerRequest(private val project: Project) {
    private var localPort = 13345
    private val adbSockets = HashMap<ConnectPair, SocketPair>()
    init {
        AndroidDebugBridge.addClientChangeListener { client, changeMask ->
            val pkgName = client.clientData.packageName
            // 存在一个问题，Android studio中选中了其它运行配置，然后此时设备上进程重新打开了，此时无法连接设备，因为包名判断不通过
            AdbDevicesManager.getProjects().forEach {project ->
                if (project.getPackageName() == pkgName) {

                    AdbDevicesManager.getDevices().forEach {device ->
                        // 判断是否已经断开连接了
                        if (!adbSockets.containsKey(ConnectPair(pkgName, device.serialNumber))) {
                            // 不存在 project-devices->pkgName的连接
                            // 尝试重新连接
                            val remotePorts = getRemotePort(client.clientData.pid, device.serialNumber)
                            // 优先读取线程数据
                            if (remotePorts != null) {
                                create(pkgName, device.serialNumber, remotePorts[0], remotePorts[1])
                            } else {
                                // 再走老的方法
                                AdbDevicesManager.fetchRemoteAgreement(project, device, pkgName)
                                val agreement = device.getAgreement(pkgName)
                                if (agreement != null) {
                                    // 拿到了协议文档 而且端口号正常，可以连接
                                    if (agreement.serverPort != null && agreement.clientPort != null) {
                                        create(pkgName, device.serialNumber, agreement.serverPort, agreement.clientPort)
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    /**
     * 获取远程特殊线程
     * 这个线程包含了本地了服务器端口
     * 线程名称格式：vd*%789:4789
     */
    private fun getRemotePort(pid: Int, deviceSerialNumber: String): List<Int>? {
        val result = execute(arrayOf("adb", "-s", deviceSerialNumber, "ps", "-p", pid.toString(), "-T"))
        if (!result.error) {
            val threads = result.msg.split("\n").map { it.trim() }
            val regex = Regex("vd\\*%\\d+:\\d+")
            for (line in threads) {
                val value = regex.find(line)?.value?.replace("vd*%", "")
                if (value != null) {
                    val port = value.split(':').mapNotNull { it.toIntOrNull() }
                    return port
                }
            }
        }
        return null
    }

    /**
     * 连接断开
     * @param localTransport1 本地转发端口，需要关闭
     */
    private fun onDisconnected(pkgName: String, deviceId: String, localTransport1: Int, localTransport2: Int) {
        adbSockets.remove(ConnectPair(pkgName, deviceId))
        execute(arrayOf("adb", "-s", deviceId, "forward", "--remove", "tcp:$localTransport1"))
        execute(arrayOf("adb", "-s", deviceId, "forward", "--remove", "tcp:$localTransport2"))
    }

    /**
     * 连接成功
     */
    private fun onConnected(deviceId: String, serverSocket: ProjectAdbServerSocket, clientSocket: ProjectAdbClientSocket) {
        // 尝试请求协议
        val device = AdbDevicesManager.getDevice(deviceId)
        if (device != null) {
            clientSocket.request("request/requestPushAgreement", "") {
                show(null, it)
                val agr = AdbAgreement.parse(it)
                if (agr != null) {
                    AdbDevicesManager.getDevice(deviceId)?.addAgreement(agr)
                }
            }
        }
    }

    /**
     * @param remoteClientPort 远程客户端端口，用于本地服务端
     * @param remoteServerPort 远程服务端端口，用于本地客户端
     */
    @Synchronized
    fun create(pkgName: String, deviceId: String, remoteServerPort: Int, remoteClientPort: Int) {
        val pair = ConnectPair(pkgName, deviceId)
        if (!adbSockets.containsKey(pair)) {
            val portClient = getPort()
            val portServer = getPort()
            execute(arrayOf("adb", "-s", deviceId, "forward", "tcp:$portClient", "tcp:$remoteServerPort"))
            execute(arrayOf("adb", "-s", deviceId, "forward", "tcp:$portServer", "tcp:$remoteClientPort"))
            try {
                // 暂时只监听server
                val server = ProjectAdbServerSocket(project, portServer)
                server.addBizRoute("open", BizRequestOpenFile::class.java)
                server.addBizRoute("heart", BizRequestHeartRoute::class.java)
                server.setDisconnectedListener {
                    onDisconnected(pkgName, deviceId, portClient, portServer)
                }
                val client = ProjectAdbClientSocket(portClient)
                adbSockets[pair] = SocketPair(server, client)
                onConnected(deviceId, server, client)
            } catch (e: Exception) {
                e.printStackTrace()
                show(e)
                show(null, "无法创建socket连接:$pkgName $deviceId")
            }
        }
    }
    private fun getPort(): Int {
        localPort ++
        return try {
            val s = ServerSocket(localPort)
            val port = s.localPort
            s.close()
            localPort ++
            port
        } catch (e: Exception) {
            getPort()
        }
    }

    fun send(pkgName: String, deviceId: String, cmd: String, content: String, callback: Callback) {
        adbSockets[ConnectPair(pkgName, deviceId)]?.clientSocket?.request(cmd, content, callback)
    }

    private data class ConnectPair(val pkgName: String, val deiceId: String)

    class SocketPair(val server: ProjectAdbServerSocket, val clientSocket: ProjectAdbClientSocket)
}