package com.example.viewdebugtrans.socket.core

import java.io.DataOutputStream
import java.net.Socket

/**
 * 数据返回操作对象
 */
class ResponseWriterImpl(private val socket: Socket) : ResponseWriter {
    companion object {
        val OK_RESP = "200 OK".toByteArray()
    }

    private val output = DataOutputStream(socket.getOutputStream())
    private var writeLength = false

    override fun writeContentLength(len: Int) {
        writeLength = true
        output.writeInt(len)
    }

    override fun writeInt(int: Int) {
        if (!writeLength) throw IllegalStateException("writeContentLength() should invoke before write")
        output.writeInt(int)
    }

    override fun write(byteArray: ByteArray) {
        if (!writeLength) throw IllegalStateException("writeContentLength() should invoke before write")
        output.write(byteArray)
    }

    override fun writeEmpty200Ok() {
        writeContentLength(OK_RESP.size)
        write(OK_RESP)
    }

    override fun finish() {
        if (!writeLength) {
            writeContentLength(0)
        }
        output.flush()
        socket.shutdownOutput()
        socket.shutdownInput()
        socket.close()
    }
}