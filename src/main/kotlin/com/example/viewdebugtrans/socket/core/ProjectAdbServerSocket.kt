package com.example.viewdebugtrans.socket.core

import com.example.viewdebugtrans.socket.biz.BizRequest404Route
import com.example.viewdebugtrans.socket.biz.BizRoute
import com.intellij.openapi.project.Project
import java.io.DataInputStream
import java.net.Socket
import kotlin.concurrent.thread

/**
 * 服务端，监听app发送的信息
 */
class ProjectAdbServerSocket(project: Project, port: Int) {
    private val socket: Socket = Socket("127.0.0.1", port)
    private val input by lazy { DataInputStream(socket.getInputStream()) }
    private var responseBuffer = ByteArray(1024)
    private val bizMap = HashMap<String, Class<BizRoute>>()
    private var disconnected: (() -> Unit)? = null
    init {
        thread {
            try {
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
                        routeClass.getConstructor(Project::class.java).newInstance(project).onRequest(routeId, content, ResponseWriterImpl(socket))
                    }


                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 连接断开了，需要重新连接
                // 1. 判断应用是否存活， 如果应用进程不存在，则需要定期
                // 2. 判断协议是否改变
                disconnected?.invoke()
            }

        }
    }
    fun <T : BizRoute> addBizRoute(routeId: String, routeClass: Class<T>) {
        bizMap[routeId] = routeClass as Class<BizRoute>
    }

    fun setDisconnectedListener(l: ()-> Unit) {
        disconnected = l
    }


    private fun getBuffer(size: Int): ByteArray {
        if (responseBuffer.size < size) {
            responseBuffer = ByteArray(size)
        }
        return responseBuffer
    }
}
