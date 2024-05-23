package com.example.viewdebugtrans.socket.core

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * 每个项目对应的socket
 * 客户端
 */
typealias Callback = ((String)-> Unit)
class ProjectAdbClientSocket(port: Int) {
    private val socket: Socket = Socket("127.0.0.1", port)
    private val output by lazy { DataOutputStream(socket.getOutputStream()) }
    private val input by lazy { DataInputStream(socket.getInputStream()) }
    private var responseBuffer = ByteArray(1024)

    @Synchronized
    fun request(cmd: String, content: String, onResponse: (String) -> Unit) {
        val cmdBytes = cmd.toByteArray()
        val contentBytes = content.toByteArray()
        // 标识符
        output.writeInt(cmd.length)
        output.writeInt(content.length)
        // 请求命令，类似于url
        output.write(cmdBytes)
        // 请求体
        output.write(contentBytes)


        val responseLength = input.readInt()
        val buffer = getBuffer(responseLength)
        input.readBulk(buffer, 0, responseLength)
        val response = String(buffer, 0, responseLength)
        onResponse(response)
    }

    private fun getBuffer(size: Int): ByteArray {
        if (responseBuffer.size < size) {
            responseBuffer = ByteArray(size)
        }
        return responseBuffer
    }
}

fun InputStream.readBulk(buffer: ByteArray, offset: Int, length: Int) {
    var readSize = 0
    while (readSize < length) {
        val read = this.read(buffer, offset, length)
        if (read > 0) {
            readSize += read
        }
    }
}