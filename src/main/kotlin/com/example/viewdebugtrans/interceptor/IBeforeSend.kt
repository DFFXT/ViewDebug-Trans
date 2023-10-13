package com.example.viewdebugtrans.interceptor

import com.example.viewdebugtrans.action.AdbSendAction
import com.example.viewdebugtrans.agreement.AdbAgreement
import com.example.viewdebugtrans.agreement.Device
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

interface IBeforeSend {

    /**
     * @return true 处理了，不分发给其它对象处理
     */
    fun beforeSend(project: Project, e: AnActionEvent, fileInfo: AdbSendAction.FileInfo, device: Device, agreement: AdbAgreement)
}

/**
 * 推送结束
 */
interface IAfterSend {
    fun afterSend(project: Project, e: AnActionEvent, fileInfo: AdbSendAction.FileInfo, device: Device, agreement: AdbAgreement)
}