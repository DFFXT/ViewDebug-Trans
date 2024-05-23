package com.example.viewdebugtrans.socket.core

import com.example.viewdebug.server.ResponseWriter

internal interface BizRoute {
        fun onRequest(routeId: String, content: String, response: ResponseWriter)
    }