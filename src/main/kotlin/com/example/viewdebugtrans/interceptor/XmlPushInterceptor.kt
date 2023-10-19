package com.example.viewdebugtrans.interceptor

import com.example.viewdebugtrans.PushFileManager
import com.example.viewdebugtrans.XmlRulesFetch
import com.example.viewdebugtrans.action.PushManager
import com.example.viewdebugtrans.agreement.AdbAgreement
import com.example.viewdebugtrans.agreement.Device
import com.intellij.openapi.project.Project

/**
 * 推动普通xml文件
 */
class XmlPushInterceptor:IPushInterceptor {
    override fun beforePush(project: Project, fileInfo: PushManager.FileInfo, device: Device, agreement: AdbAgreement) {
        val originPath = fileInfo.originPath
        if (originPath.endsWith(".xml") && fileInfo.type == PushFileManager.TYPE_LAYOUT) {
            XmlRulesFetch().getXmlRules(project).forEachIndexed { index, it ->
                PushFileManager.pushFile(it, agreement.destDir + "/" + "merger-${index}.xml", PushFileManager.TYPE_XML_RULE, extra = null)
            }
            // send(fileInfo, e)

        }
    }
}