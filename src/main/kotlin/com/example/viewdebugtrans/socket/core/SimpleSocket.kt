package com.example.viewdebugtrans.socket.core

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder


/**
 * 短链接，使用后立即关闭
 * socket协议
 * 请求：
 * 4字节，路由id长度
 * 路由id内容
 * 4字节，body长度
 * body内容
 *
 * 返回：
 * 4字节 body长度
 * body内容
 */
object SimpleSocket {
    /**
     * @param data 要发送的数据
     * @param port 发送的端口
     */
    fun send(cmd: String, data: ByteArray, port: Int): ByteArray {
        val socket = Socket("127.0.0.1", port)
        val out = DataOutputStream(socket.getOutputStream())
        val input = DataInputStream(socket.getInputStream())

        // 写入路由信息
        val cmdByteArray = cmd.toByteArray()
        out.writeInt(cmdByteArray.size)
        out.write(cmdByteArray)

        // 写入实体数据
        out.writeInt(data.size)
        out.write(data)
        out.flush()
        val contentLength = input.readInt()
        val content = input.readNBytes(contentLength)

        // 关闭来连接
        socket.shutdownOutput()
        socket.shutdownInput()
        socket.close()
        return content
    }

    fun requestMultiContent(cmd: String, data: ByteArray, port: Int): List<ByteArray> {
        val content = send(cmd, data, port)
        val buffer = ByteBuffer.wrap(content).order(ByteOrder.BIG_ENDIAN)
        val list = ArrayList<ByteArray>()
        while (buffer.limit() != buffer.position()) {
            val len = buffer.getInt()
            val arr = ByteArray(len)
            buffer.get(arr)
            list.add(arr)
        }
        return list
    }
}