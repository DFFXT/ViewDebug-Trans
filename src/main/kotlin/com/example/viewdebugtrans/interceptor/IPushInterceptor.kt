package com.example.viewdebugtrans.interceptor

import com.example.viewdebugtrans.action.PushManager
import com.example.viewdebugtrans.agreement.AdbAgreement
import com.example.viewdebugtrans.agreement.Device
import com.intellij.openapi.project.Project

interface IPushInterceptor {

    /**
     * 推送前逻辑
     */
    fun beforePush(project: Project, fileInfo: PushManager.FileInfo, device: Device, agreement: AdbAgreement) {}

    /**
     * 推送后逻辑
     * 未执行[beforePush]的实例将不会收到回调
     */
    fun afterPush(project: Project, fileInfo: PushManager.FileInfo, device: Device, agreement: AdbAgreement) {}

    /**
     * 终止执行回调
     * 未执行[beforePush]的实例将不会收到回调
     */
    fun onPushBreak(project: Project, fileInfo: PushManager.FileInfo, device: Device, agreement: AdbAgreement) {}
}