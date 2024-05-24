package com.example.viewdebugtrans.socket

interface OnDisconnected {
    fun onDisconnected(pkgName:String, device: String)
}