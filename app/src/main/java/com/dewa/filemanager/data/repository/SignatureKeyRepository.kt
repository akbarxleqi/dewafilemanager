package com.dewa.filemanager.data.repository

import com.dewa.filemanager.data.model.SignatureKeyCreateRequest
import com.dewa.filemanager.data.model.SignatureKeyEntry
import com.dewa.filemanager.data.model.SignatureStoreType
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Date
import java.util.Locale

class SignatureKeyRepository(
    private val fileRepo: FileManagerRepository = FileManagerRepository()
) {

    companion object {
        private const val APK_SIGNING_STORE_TYPE = "PKCS12"
    }

    data class CreateResult(
        val path: String
    )

    fun listKeys(): List<SignatureKeyEntry> {
        val dir = getKeyDir()
        return dir.listFiles()
            ?.filter { it.isFile }
            ?.mapNotNull { file ->
                val type = when (file.extension.lowercase(Locale.ROOT)) {
                    "jks" -> SignatureStoreType.JKS
                    "bks" -> SignatureStoreType.BKS
                    else -> null
                } ?: return@mapNotNull null

                SignatureKeyEntry(
                    name = file.name,
                    path = file.absolutePath,
                    type = type,
                    size = file.length(),
                    lastModified = file.lastModified()
                )
            }
            ?.sortedByDescending { it.lastModified }
            ?: emptyList()
    }

    fun createKey(request: SignatureKeyCreateRequest): Result<CreateResult> {
        return runCatching {
            require(request.alias.isNotBlank()) { "Alias wajib diisi" }
            require(request.storePassword.isNotBlank()) { "Kata sandi wajib diisi" }
            require(request.validityYears > 0) { "Validitas harus lebih dari 0 tahun" }

            val bcProvider = BouncyCastleProvider()

            val keyDir = getKeyDir()
            val outputFile = buildOutputFile(
                dir = keyDir,
                baseName = request.alias,
                storeType = request.storeType
            )

            val keyPair = KeyPairGenerator.getInstance("RSA").apply {
                initialize(2048, SecureRandom())
            }.generateKeyPair()

            val subject = buildSubject(request)
            val now = Date()
            val notAfter = Date(now.time + request.validityYears * 365L * 24L * 60L * 60L * 1000L)

            val certBuilder = JcaX509v3CertificateBuilder(
                subject,
                BigInteger(64, SecureRandom()),
                now,
                notAfter,
                subject,
                keyPair.public
            )

            val signer = JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(bcProvider)
                .build(keyPair.private)

            val certificate = JcaX509CertificateConverter()
                .setProvider(bcProvider)
                .getCertificate(certBuilder.build(signer))

            val storePassword = request.storePassword.toCharArray()
            val aliasPassword = request.aliasPassword.ifBlank { request.storePassword }.toCharArray()

            val keyStore = when (request.storeType) {
                // Android runtimes often do not ship a JKS provider. We generate a PKCS12
                // keystore for APK signing compatibility and keep the .jks extension the user selected.
                SignatureStoreType.JKS -> KeyStore.getInstance(APK_SIGNING_STORE_TYPE)
                SignatureStoreType.BKS -> KeyStore.getInstance("BKS", bcProvider)
            }

            keyStore.load(null, storePassword)
            keyStore.setKeyEntry(
                request.alias,
                keyPair.private,
                aliasPassword,
                arrayOf(certificate)
            )

            outputFile.outputStream().use { output ->
                keyStore.store(output, storePassword)
            }

            CreateResult(path = outputFile.absolutePath)
        }
    }

    fun deleteKey(path: String): Boolean {
        val file = File(path)
        val root = getKeyDir().absolutePath
        if (!file.absolutePath.startsWith(root)) return false
        return file.exists() && file.isFile && file.delete()
    }

    private fun getKeyDir(): File {
        fileRepo.ensureAppDirectories()
        return File(fileRepo.getSignatureKeyPath()).apply { mkdirs() }
    }

    private fun buildOutputFile(
        dir: File,
        baseName: String,
        storeType: SignatureStoreType
    ): File {
        val safeBase = baseName
            .trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .ifBlank { "signkey" }

        val ext = when (storeType) {
            SignatureStoreType.JKS -> "jks"
            SignatureStoreType.BKS -> "bks"
        }

        var file = File(dir, "$safeBase.$ext")
        var index = 2
        while (file.exists()) {
            file = File(dir, "$safeBase ($index).$ext")
            index++
        }
        return file
    }

    private fun buildSubject(request: SignatureKeyCreateRequest): X500Name {
        fun esc(value: String): String = value.replace(",", "\\,")

        val attrs = buildList {
            if (request.commonName.isNotBlank()) add("CN=${esc(request.commonName)}")
            if (request.organizationalUnit.isNotBlank()) add("OU=${esc(request.organizationalUnit)}")
            if (request.organization.isNotBlank()) add("O=${esc(request.organization)}")
            if (request.locality.isNotBlank()) add("L=${esc(request.locality)}")
            if (request.stateOrProvince.isNotBlank()) add("ST=${esc(request.stateOrProvince)}")
            if (request.countryCode.isNotBlank()) add("C=${request.countryCode.uppercase(Locale.ROOT)}")
        }

        val dn = if (attrs.isEmpty()) {
            "CN=${esc(request.alias)}"
        } else {
            attrs.joinToString(",")
        }
        return X500Name(dn)
    }
}
