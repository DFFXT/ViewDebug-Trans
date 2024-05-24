package com.example.viewdebugtrans.socket.biz

import com.example.viewdebugtrans.socket.core.ResponseWriter
import com.intellij.openapi.project.Project

internal abstract class BizRoute(protected val project: Project) {
    abstract fun onRequest(routeId: String, content: String, response: ResponseWriter)
}