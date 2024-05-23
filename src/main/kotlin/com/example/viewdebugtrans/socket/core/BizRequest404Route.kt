package com.example.viewdebugtrans.socket.core

import com.example.viewdebug.server.ResponseWriter
import com.example.viewdebugtrans.show

internal class BizRequest404Route: BizRoute {

    override fun onRequest(routeId: String, content: String, response: ResponseWriter) {
        show(null, "404 for $routeId")
        val c = "404 $routeId not found".toByteArray()
        response.writeContentLength(c.size)
        response.write(c)
        // response.finish()
    }
}