package com.example.viewdebugtrans.socket

import com.example.viewdebugtrans.execute
import com.example.viewdebugtrans.show
import com.example.viewdebugtrans.socket.core.Callback
import com.example.viewdebugtrans.socket.core.ProjectAdbClientSocket
import com.example.viewdebugtrans.socket.core.ProjectAdbServerSocket
import java.net.ServerSocket

/**
 * adb请求
 */
object AdbServerRequest {
    private var localPort = 13345
    private val adbSockets = HashMap<ConnectPair, SocketPair>()

    /**
     * @param remoteClientPort 远程客户端端口，用于本地服务端
     * @param remoteServerPort 远程服务端端口，用于本地客户端
     */
    fun create(pkgName: String, deviceId: String, remoteServerPort: Int, remoteClientPort: Int) {
        val pair = ConnectPair(pkgName, deviceId)
        if (!adbSockets.containsKey(pair)) {
            val portClient = getPort()
            val portServer = getPort()
            execute(arrayOf("adb", "-s", deviceId, "forward", "tcp:$portClient", "tcp:$remoteServerPort"))
            execute(arrayOf("adb", "-s", deviceId, "forward", "tcp:$portServer", "tcp:$remoteClientPort"))
            try {
                adbSockets[pair] = SocketPair(ProjectAdbServerSocket(portServer), ProjectAdbClientSocket(portClient))
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