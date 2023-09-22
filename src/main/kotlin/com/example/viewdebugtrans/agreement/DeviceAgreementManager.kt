package com.example.viewdebugtrans.agreement

object DeviceAgreementManager {

    fun toAgreementMap(text: String): Map<String,String> {
        val map = HashMap<String, String>()
        text.split('\n').forEach {
            val index = it.indexOf('=')
            if (index > 0) {
                map[it.substring(0, index)] = it.substring(index + 1)
            }
        }
        return map
    }
}