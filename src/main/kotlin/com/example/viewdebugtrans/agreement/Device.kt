package com.example.viewdebugtrans.agreement

data class Device(
    val id: String,
    val serialNumber: String
) {
    var agreement: ArrayList<AdbAgreement> = ArrayList()

    fun addAgreement(agreement: AdbAgreement?) {
        agreement ?: return
        if (getAgreement(agreement.pkgName) == null) {
            this.agreement.add(agreement)
        }
    }

    fun getAgreement(pkgName: String?): AdbAgreement? = this.agreement.find { it.pkgName == pkgName }

}