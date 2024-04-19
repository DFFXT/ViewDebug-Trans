package com.example.viewdebugtrans.R

import java.io.File
import java.nio.charset.Charset
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

object Main {
    @JvmStatic
    fun main(vararg args: String) {
        var start = 0
        var end = 255
        var mac = "6a-54-62-d0-fe-c8"
        if (args.size >= 3) {
            start = args[0].toInt()
            end = args[1].toInt()
            mac = args[2]
        }
        val ip = arpGetIp(mac)
        val count = end - start + 1
        if (ip == null) {
            val pingList = CopyOnWriteArrayList<String>()
            val size = AtomicInteger(0)
            for (i in IntRange(start, end)) {
                thread {
                    val tryIp = "192.168.133.${i}"
                    val result = execute(arrayOf("ping", tryIp))
                    if (result.contains("的回复: 字节=32")) {
                        pingList.add(tryIp)
                    }
                    if (size.incrementAndGet() == count) {
                        pingList.forEach {
                            println("--> $it")
                        }
                        arpGetIp(mac)?.let { pingGetIp ->
                            println(pingGetIp)
                            println(execute(arrayOf("adb", "connect", pingGetIp)))
                        }
                    }
                }
            }
        } else {
            println(ip)
        }
    }

    private fun arpGetIp(macAddress: String): String? {
        val lines = execute(arrayOf("arp", "-a")).split('\n')
        val line = lines.find { it.contains(macAddress) }
        return line?.substring(2, line.indexOf("   "))
    }

    fun execute(cmdArray: Array<String>, dir: File? = null): String {
        val p = Runtime.getRuntime().exec(cmdArray)
        // 需要读取errorStream，否则缓冲区堆积，导致死锁
        thread {
            val errorStream = p.errorStream.readBytes()
        }
        val result = String(p.inputStream.readBytes(), Charset.forName("gbk"))
        // println(result)
        return result
    }
}