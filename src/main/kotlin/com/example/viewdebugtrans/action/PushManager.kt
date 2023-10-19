package com.example.viewdebugtrans.action

import com.example.viewdebugtrans.PushFileManager
import com.example.viewdebugtrans.ShowLogAction
import com.example.viewdebugtrans.agreement.AdbAgreement
import com.example.viewdebugtrans.agreement.AdbDevicesManager
import com.example.viewdebugtrans.agreement.Device
import com.example.viewdebugtrans.interceptor.*
import com.example.viewdebugtrans.show
import com.example.viewdebugtrans.util.showDialog
import com.example.viewdebugtrans.util.showTip
import com.google.gson.JsonElement
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import java.io.File
import java.util.*
import kotlin.concurrent.thread

private val beforeSendMap = LinkedList<IPushInterceptor>().apply {
    add(JavaPushInterceptor())
    add(KotlinPushInterceptor())
    add(XmlPushInterceptor())
    add(XmlValuePushInterceptor())
}

class PushManager(protected val device: Device, private val agreement: AdbAgreement, private val reboot: Boolean) {

    fun actionPerformed(project: Project, file: String) {
        try {
            // return
            ShowLogAction.clear()

            val path = file
            val originPath = path
            val fileType: String = getFileType(originPath)
            val fileInfo = FileInfo(path, path, fileType)
            val adbP = AdbDevicesManager.getAdbPath(project)
            if (adbP == null) {
                showTip(project, "没有adb可用")
                return
            }
            PushFileManager.init(project, device, agreement, adbP)

            thread {
                ProgressManager.getInstance().run(object : Task.Backgroundable(project, "推送中") {
                    override fun run(indicator: ProgressIndicator) {
                        beforeSend(project, fileInfo)
                        if (!indicator.isCanceled) {
                            if (fileInfo.isPushBreak()) {
                                showTip(project, fileInfo.breakReason!!)
                            } else {
                                send(fileInfo)
                                afterSend(project, fileInfo)
                            }
                        }
                    }

                    override fun onCancel() {
                        super.onCancel()
                    }
                })
            }
        } catch (exception: Exception) {
            show(
                project = project, exception.message + "\n" +
                        exception.stackTraceToString()
            )
        }

    }

    /**
     * 推送之前，可以对文件进行加工和处理
     */
    private fun beforeSend(project: Project, fileInfo: FileInfo) {
        val executed = LinkedList<IPushInterceptor>()
        for (interceptor in beforeSendMap) {
            if (!fileInfo.isPushBreak()) {
                executed.add(interceptor)
                interceptor.beforePush(project, fileInfo, device, agreement)
            } else {
                executed.forEach {
                    it.onPushBreak(project, fileInfo, device, agreement)
                }
                break
            }
        }
    }

    /**
     * 推送
     */
    private fun send(fileInfo: FileInfo) {
        val target = File(fileInfo.path)
        if (target.exists()) {
            val destFolder = agreement.destDir
            PushFileManager.pushFile(
                fileInfo.path,
                destFolder + "/" + target.name,
                fileInfo.type,
                fileInfo.originPath,
                fileInfo.extra
            )
            PushFileManager.pushApply(reboot)
        }
    }

    /**
     * 推送结束
     */
    private fun afterSend(project: Project, fileInfo: FileInfo) {
        val target = File(fileInfo.path)
        if (fileInfo.isPushBreak()) {
            showTip(project, fileInfo.breakReason!!)
        } else if (target.exists()) {
            showDialog(project, "推送成功", "提示", arrayOf("确定"), 0)
        } else {
            showDialog(project, "推送失败，产物文件不存在: ${fileInfo.path}", "提示", arrayOf("确定"), 0)
            show(project, "不存在${fileInfo.path}")
        }
        PushFileManager.reset()
        for (interceptor in beforeSendMap) {
            if (!fileInfo.isPushBreak()) {
                interceptor.afterPush(project, fileInfo, device, agreement)
            } else {
                beforeSendMap.forEach {
                    it.onPushBreak(project, fileInfo, device, agreement)
                }
                break
            }
        }
    }


    private fun getFileType(path: String): String {
        val file = File(path)
        val parent = file.parentFile.name
        if (parent.startsWith("drawable")) {
            return PushFileManager.TYPE_DRAWABLE
        }
        if (parent.startsWith("layout")) {
            return PushFileManager.TYPE_LAYOUT
        }
        if (parent.startsWith("anim")) {
            return PushFileManager.TYPE_ANIM
        }
        if (parent.startsWith("color")) {
            return PushFileManager.TYPE_COLOR
        }
        if (parent.startsWith("values")) {
            return PushFileManager.TYPE_XML_VALUES
        }
        return PushFileManager.TYPE_FILE
    }

    /**
     * @param originPath 原始文件路径
     * @param path 要处理或者推送的路径
     * @param type 文件类型
     */
    class FileInfo(var originPath: String, var path: String, var type: String, var extra: JsonElement? = null) {
        // 标记，比如再before中设置一个tag，然后再在after中读取
        val tag = HashMap<String, Any>()
        private var isBreak = false
        var breakReason: String? = null
            private set

        /**
         * 打断执行流程
         * @param reason 原因
         */
        fun breakPush(reason: String) {
            isBreak = true
            breakReason = reason
        }

        /**
         * 流程是否被打断
         */
        fun isPushBreak(): Boolean {
            return isBreak
        }
    }
}