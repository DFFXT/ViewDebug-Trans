package com.example.viewdebugtrans.log

/**
 * 日志输出，和Android 的log一致
 */
object Logger {
    fun i(tag: String, msg: String?) {
        println("Trans-${Thread.currentThread().id}  $tag- $msg")
    }
}