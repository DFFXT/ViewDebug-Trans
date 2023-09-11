package com.example.viewdebugtrans.R

import java.io.File
import java.nio.ByteBuffer
import java.util.Base64

/**
 * 字符串转16进制，16进制转字符串
 */
object StringToHex {

    @JvmStatic
    fun main(vararg args: String) {
        println(revert(md5(File("").absolutePath)))
    }

    fun md5(string: String): String {
        val mm = "0123456789ABCDEF"
        val builder = StringBuilder()

        val buffer = string.toByteArray()
        for (byte in buffer) {
            builder.append(mm.get((byte.toInt() and 0xf0 shr 4)).toString())
            builder.append((mm.get(byte.toInt() and 0x0f)).toString())
        }

        return builder.toString()
    }

    fun revert(hex: String): String {
        println(hex)
        //Base64.getEncoder().encode("")
        val buffer = ByteBuffer.allocate(hex.length * 4)
        val mm = HashMap<Char, Int>()
        mm['0'] = 0
        mm['1'] = 1
        mm['2'] = 2
        mm['3'] = 3
        mm['4'] = 4
        mm['5'] = 5
        mm['6'] = 6
        mm['7'] = 7
        mm['8'] = 8
        mm['9'] = 9
        mm['A'] = 10
        mm['B'] = 11
        mm['C'] = 12
        mm['D'] = 13
        mm['E'] = 14
        mm['F'] = 15
        for (i in 0 until hex.length step 2) {
            val c1 = hex.get(i)
            val c2 = hex.get(i + 1)
            buffer.put((mm[c1]!! * 16 + mm[c2]!!.toInt()).toByte())
        }
        val bytes = ByteArray(buffer.position())
        buffer.position(0)
        buffer.get(bytes)
        return String(bytes)
    }
}