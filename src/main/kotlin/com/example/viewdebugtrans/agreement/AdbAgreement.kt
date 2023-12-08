package com.example.viewdebugtrans.agreement

data class AdbAgreement(
    // 格式 0.20.4-1-g87125dd 或者不带后续 0.20.4
    val version: String,
    val pkgName: String,
    val listenFile: String,
    val destDir: String
) {

    /**
     * 版本判断
     * 判断版本是否等于高于
     * @param version 衡量版本, 格式必须为 xx.xx.xx 不能带更详细的版本号
     * @return true, 客户端版本高于等于指定版本
     */
    fun atLeast(version: String): Boolean {
        val versionArray = version.split('.').map { it.toIntOrNull() ?: 0 }
        val thisVersion = this.version.replace('-', '.').split('.').map { it.toIntOrNull() ?: 0 }
        if (versionArray.size >= 3 && thisVersion.size >= 3) {
            val versionValue = versionArray[0] * 10000_0000 + versionArray[1] * 10000 + versionArray[2]
            val thisValue = thisVersion[0] * 10000_0000 + thisVersion[1] * 10000 + thisVersion[2]
            return thisValue >= versionValue
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