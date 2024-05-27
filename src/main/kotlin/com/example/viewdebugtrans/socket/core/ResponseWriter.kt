package com.example.viewdebugtrans.socket.core

/**
 * 返回数据操作接口
 */
internal interface ResponseWriter {

    /**
     * 写入返回数据长度
     */
    fun writeContentLength(len: Int)

    /**
     * 写入int
     */
    fun writeInt(int: Int)

    /**
     * 写入返回内容
     */
    fun write(byteArray: ByteArray)

    /**
     * 写入200 OK，不能包含内容
     */
    fun writeEmpty200Ok()

    /**
     * 写入结束，关闭连接
     */
    fun finish()
}