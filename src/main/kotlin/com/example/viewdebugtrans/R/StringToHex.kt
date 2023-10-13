package com.example.viewdebugtrans.R

import com.android.utils.forEach
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 字符串转16进制，16进制转字符串
 */
object StringToHex {

    @JvmStatic
    fun main(vararg args: String) {
        transformXml(
            "<resources>\n" +
                    "\n" +
                    "    <style name=\"user_item\">\n" +
                    "        <item name=\"android:layout_height\">@dimen/user_fragment_item_width_full</item>\n" +
                    "        <item name=\"android:layout_width\">@dimen/user_fragment_item_width_full</item>\n" +
                    "    </style>\n" +
                    "\n" +
                    "</resources>"
        )
        println(builder.toString())
    }

    private val builder = StringBuilder()

    private fun serverTransform(xml: String): String? {
        if (xml.isEmpty()) return null
        val text = if (xml.startsWith("<resources") || xml.startsWith("<?xml")) {
            xml
        } else {
            "<?xml version=\"1.0\" encoding=\"utf-8\" ?><resources>$xml</resources>"
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
    private fun transformXml(xml: String) {
        val text = if (xml.startsWith("<resources") || xml.startsWith("<?xml")) {
            xml
        } else {
            "<?xml version=\"1.0\" encoding=\"utf-8\" ?><resources>$xml</resources>"
        }
        val builder = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder()
        val doc = builder.parse(text.byteInputStream())
        doc.childNodes.forEach {
            parse(it)
        }
    }

    private fun parse(node: Node) {
        val name = node.nodeName
        when (name) {
            "string", "color", "integer" -> {
                addItem(node)
            }

            "resources" -> {
                node.childNodes.forEach {
                    parse(it)
                }
            }
        }
    }

    private fun addItem(node: Node) {
        if (node.nodeType == Node.ELEMENT_NODE) {
            builder.append("<${node.nodeName}")
            node.attributes.forEach {
                builder.append(" ${it.nodeName}=")
                builder.append("\"${it.nodeValue}\"")
            }
            builder.append(">")
            node.childNodes.forEach {
                addItem(it)
            }
            builder.append("</${node.nodeName}>\n")
        } else if (node.nodeType == Node.TEXT_NODE) {
            builder.append(node.nodeValue)
        }
    }
}