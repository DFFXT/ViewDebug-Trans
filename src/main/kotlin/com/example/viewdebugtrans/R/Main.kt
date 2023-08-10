package com.example.viewdebugtrans.R

import com.example.viewdebugtrans.Config

object Main {
    @JvmStatic
    fun main(vararg args: String) {
        println(String(Runtime.getRuntime().exec("adb2", arrayOf("Path=D:/C++;")).inputStream.readBytes()))
    }
}