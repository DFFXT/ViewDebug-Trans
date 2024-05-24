package com.example.viewdebugtrans.socket.biz

import com.example.viewdebugtrans.socket.core.ResponseWriter
import com.example.viewdebugtrans.show
import com.intellij.openapi.project.Project

internal class BizRequest404Route(project: Project): BizRoute(project) {

    override fun onRequest(routeId: String, content: String, response: ResponseWriter) {
        show(null, "404 for $routeId")
        val c = "404 $routeId not found".toByteArray()
        response.writeContentLength(c.size)
        response.write(c)
        // response.finish()
    }
}