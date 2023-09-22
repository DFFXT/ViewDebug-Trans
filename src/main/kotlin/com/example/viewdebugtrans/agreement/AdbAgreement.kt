package com.example.viewdebugtrans.agreement

data class AdbAgreement (
    val version: String,
    val pkgName: String,
    val listenFile: String,
    val destDir: String
) {
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