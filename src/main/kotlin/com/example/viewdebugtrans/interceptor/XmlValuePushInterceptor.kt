package com.example.viewdebugtrans.interceptor

import com.android.utils.forEach
import com.example.viewdebugtrans.PushFileManager
import com.example.viewdebugtrans.action.PushManager
import com.example.viewdebugtrans.agreement.AdbAgreement
import com.example.viewdebugtrans.agreement.Device
import com.intellij.openapi.project.Project
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 推送value裂隙xml文件
 */
class XmlValuePushInterceptor : IPushInterceptor {

    override fun beforePush(project: Project, fileInfo: PushManager.FileInfo, device: Device, agreement: AdbAgreement) {
        if (fileInfo.type == PushFileManager.TYPE_XML_VALUES) {

            return
            /* val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
             var selection = editor.selectionModel.selectedText?.trim() ?: return
             val xml = valueXmlTransform(selection) ?: return showTip(project, "请选中正确的values-xml内容")
             val localFile = File(project.getViewDebugDir(), "values-selected.xml")
             localFile.writeText(xml)
             // 将临时文件作为可推送目标
             fileInfo.path = localFile.absolutePath*/
            /*XmlRulesFetch().getXmlRules(project).forEachIndexed { index, it ->
                PushFileManager.pushFile(it, agreement.destDir + "/" + "merger-${index}.xml", PushFileManager.TYPE_XML_RULE, extra = null)
            }*/
            // send(fileInfo, e)

        }
    }

    private fun valueXmlTransform(xml: String): String? {
        if (xml.isEmpty()) return null
        val text = if (xml.startsWith("<resources") || xml.startsWith("<?xml")) {
            xml
        } else {
            "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n<resources>$xml</resources>"
        }
        val builder = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder()
        try {
            builder.parse(text.byteInputStream())
            return text
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private val builder = StringBuilder()
    private fun transformXml(xml: String) {
        val selection = "<resources>$xml</resources>"
        val builder = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder()
        val doc = builder.parse(selection)
        doc.childNodes.forEach {
            parse(it)
        }
    }

    private fun parse(node: Node) {
        val name = node.nodeName
        when (name) {
            "string", "color" -> {
                addItem(node)
            }
        }
    }

    private fun addItem(node: Node) {
        if (node.nodeType == Node.DOCUMENT_NODE) {
            builder.append("<${node.nodeName}")
            node.attributes.forEach {
                builder.append(" ${it.nodeName}=")
                builder.append("\"${it.nodeValue}\"")
            }
            builder.append(">")
            node.childNodes.forEach {
                addItem(it)
            }
            builder.append("</${node.nodeName}")
        } else if (node.nodeType == Node.TEXT_NODE) {
            builder.append(node.nodeValue)
        }
    }
}