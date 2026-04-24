package com.dewa.filemanager.data.model

enum class SignatureStoreType {
    JKS,
    BKS
}

data class SignatureKeyEntry(
    val name: String,
    val path: String,
    val type: SignatureStoreType,
    val size: Long,
    val lastModified: Long
)

data class SignatureKeyCreateRequest(
    val storeType: SignatureStoreType,
    val storePassword: String,
    val alias: String,
    val aliasPassword: String,
    val validityYears: Int,
    val commonName: String,
    val organizationalUnit: String,
    val organization: String,
    val locality: String,
    val stateOrProvince: String,
    val countryCode: String
)
