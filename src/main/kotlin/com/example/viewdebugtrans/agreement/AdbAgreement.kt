package com.example.viewdebugtrans.agreement

import com.example.viewdebugtrans.util.showTip
import org.gradle.internal.impldep.org.apache.ivy.osgi.util.VersionComparator

data class AdbAgreement (
    val version: String,
    val pkgName: String,
    val listenFile: String,
    val destDir: String
) {

    /**
     * 版本判断
     * 判断版本是否等于高于
     * @param version 衡量版本
     */
    fun versionSupport(version: String): Boolean{
        val versionArray = version.split('.').map { it.toIntOrNull() ?: 0 }
        val thisVersion = this.version.split('.').map { it.toIntOrNull() ?: 0 }
        if (versionArray.size >= 3 && thisVersion.size >= 3) {
            return !(versionArray[0] > thisVersion[0] || versionArray[1] > thisVersion[1] || versionArray[2] > thisVersion[2])
        } else {
            throw RuntimeException("不规范的版本号:${this.version}")
        }
    }
    companion object {
        fun parse(map: Map<String, String>): AdbAgreement {
            return AdbAgreement(
                version = map["version"] ?: "",
                pkgName = map["pkgName"] ?: "",
                listenFile = map["listenFile"] ?: "",
                destDir = map["destDir"] ?: ""
            )
        }
    }
}