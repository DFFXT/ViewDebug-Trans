package com.example.viewdebugtrans.socket

import com.example.viewdebugtrans.socket.core.SimpleSocket

/**
 * adb请求
 */
object AdbServerRequest {
    // pc本地端口，会被adb映射到远程端口
    val remotePort = 12348

    /**
     * 请求远推送协议
     */
    fun requestRemotePushAgreement(): Map<String, String> {
        val result = String(SimpleSocket.send("request/requestPushAgreement", byteArrayOf(), remotePort))
        val map = HashMap<String, String>()
        result.split('\n').forEach {
            val index = it.indexOf('=')
            if (index > 0) {
                map[it.substring(0, index)] = it.substring(index + 1)
            }
        }
        return map
    }
}