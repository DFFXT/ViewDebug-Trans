package com.example.viewdebugtrans.socket.core

object SocketClient {


    @JvmStatic
    fun main(vararg args: String) {
        //println(String(SimpleSocket.send("request/R","com.example.viewdebug".toByteArray(), 12348)))
        println((SimpleSocket.requestMultiContent(
            "request/R",
            "com.example.viewdebug\ncom.example.skinswitch".toByteArray(),
            12348
        ).map { String(it) }.joinToString("\n\n")))
       // HttpSocket.send("", byteArrayOf(), 1)
    }
}