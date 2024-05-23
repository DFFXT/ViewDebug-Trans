package com.example.viewdebugtrans.socket.core

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * 服务端，监听app发送的信息
 */
class ProjectAdbServerSocket(port: Int) {
    private val socket: Socket = Socket("127.0.0.1", port)
    private val input by lazy { DataInputStream(socket.getInputStream()) }
    private var responseBuffer = ByteArray(1024)
    private val bizMap = HashMap<String, Class<BizRoute>>()
    init {
        thread {
            while (true) {
                // 指令长度
                val cmdLength = input.readInt()
                if (cmdLength > 0) {
                    // 接收内容长度
                    val contentLength = input.readInt()
                    // 路由id
                    var buffer = getBuffer(cmdLength)
                    input.readBulk(buffer, 0, cmdLength)
                    val routeId = String(buffer, 0, cmdLength)
                    buffer = getBuffer(contentLength)
                    input.readBulk(buffer, 0, contentLength)
                    val content = String(buffer, 0, contentLength)

                    // 构建路由处理器
                    val routeClass = bizMap.getOrDefault(routeId, BizRequest404Route::class.java)
                    // 处理器处理对应请求
                    routeClass.newInstance().onRequest(routeId, content, ResponseWriterImpl(socket))
                }


            }
        }
    }
    fun <T : BizRoute> addBizRoute(routeId: String, routeClass: Class<T>) {
        bizMap[routeId] = routeClass as Class<BizRoute>
    }


    private fun getBuffer(size: Int): ByteArray {
        if (responseBuffer.size < size) {
            responseBuffer = ByteArray(size)
        }
        return responseBuffer
    }
}
