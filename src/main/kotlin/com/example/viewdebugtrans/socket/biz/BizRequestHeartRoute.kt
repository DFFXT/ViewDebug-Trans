package com.example.viewdebugtrans.socket.biz

import com.example.viewdebugtrans.socket.core.ResponseWriter
import com.example.viewdebugtrans.show
import com.intellij.openapi.project.Project

internal class BizRequestHeartRoute(project: Project): BizRoute(project) {

    override fun onRequest(routeId: String, content: String, response: ResponseWriter) {
        response.writeContentLength(0)
        // response.finish()
    }
}