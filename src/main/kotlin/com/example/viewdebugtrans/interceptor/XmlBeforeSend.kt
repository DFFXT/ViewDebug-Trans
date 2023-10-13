package com.example.viewdebugtrans.interceptor

import com.example.viewdebugtrans.PushFileManager
import com.example.viewdebugtrans.XmlRulesFetch
import com.example.viewdebugtrans.action.AdbSendAction
import com.example.viewdebugtrans.agreement.AdbAgreement
import com.example.viewdebugtrans.agreement.Device
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

/**
 * 推动普通xml文件
 */
class XmlBeforeSend:IBeforeSend {
    override fun beforeSend(project: Project, e: AnActionEvent, fileInfo: AdbSendAction.FileInfo, device: Device, agreement: AdbAgreement) {
        val originPath = fileInfo.originPath
        if (originPath.endsWith(".xml") && fileInfo.type == PushFileManager.TYPE_LAYOUT) {
            XmlRulesFetch().getXmlRules(project).forEachIndexed { index, it ->
                PushFileManager.pushFile(it, agreement.destDir + "/" + "merger-${index}.xml", PushFileManager.TYPE_XML_RULE, extra = null)
            }
            // send(fileInfo, e)

        }
    }
}