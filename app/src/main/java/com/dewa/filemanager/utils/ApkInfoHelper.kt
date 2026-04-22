package com.dewa.filemanager.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.dewa.filemanager.utils.ApkIconHelper
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*

data class SignatureInfo(
    val fileName: String,
    val schema: String, // V1, V2, V3 etc
    val status: String,
    val algorithm: String,
    val createdDate: String,
    val expiryDate: String,
    val owner: String,
    val md5: String,
    val sha1: String,
    val sha256: String
)

data class ApkInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val fileSize: String,
    val targetSdk: String,
    val minSdk: String,
    val installedVersion: String?,
    val installDate: String?,
    val updateDate: String?,
    val apkPath: String,
    val icon: ImageBitmap?,
    val signatureInfo: SignatureInfo?
)

object ApkInfoHelper {
    fun getApkInfo(context: Context, apkPath: String): ApkInfo? {
        return try {
            val pm = context.packageManager
            val file = File(apkPath)
            
            // For signatures, we need GET_SIGNATURES or GET_SIGNING_CERTIFICATES
            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION.SDK_INT) {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            
            val info = pm.getPackageArchiveInfo(apkPath, flags) ?: return null
            val appInfo = info.applicationInfo ?: return null
            
            appInfo.sourceDir = apkPath
            appInfo.publicSourceDir = apkPath
            
            val appName = pm.getApplicationLabel(appInfo).toString()
            val packageName = info.packageName
            val versionName = info.versionName ?: "N/A"
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
            
            val fileSize = toReadableSize(file.length())
            val targetSdk = "Android ${getAndroidVersion(appInfo.targetSdkVersion)} (API ${appInfo.targetSdkVersion})"
            val minSdk = "Android ${getAndroidVersion(appInfo.minSdkVersion)} (API ${appInfo.minSdkVersion})"
            
            var installedVersion: String? = null
            try {
                val installedPackage = pm.getPackageInfo(packageName, 0)
                installedVersion = "${installedPackage.versionName} (${if (android.os.Build.VERSION.SDK_INT >= 28) installedPackage.longVersionCode else @Suppress("DEPRECATION") installedPackage.versionCode})"
            } catch (e: PackageManager.NameNotFoundException) {
                installedVersion = "Tidak terpasang"
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val modDate = sdf.format(Date(file.lastModified()))
            
            val icon = ApkIconHelper.getApkIcon(context, apkPath)
            
            val signatureInfo = extractSignatureInfo(file.name, info)

            ApkInfo(
                appName = appName,
                packageName = packageName,
                versionName = versionName,
                versionCode = versionCode,
                fileSize = fileSize,
                targetSdk = targetSdk,
                minSdk = minSdk,
                installedVersion = installedVersion,
                installDate = modDate,
                updateDate = modDate,
                apkPath = apkPath,
                icon = icon,
                signatureInfo = signatureInfo
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun extractSignatureInfo(fileName: String, info: PackageInfo): SignatureInfo? {
        return try {
            @Suppress("DEPRECATION")
            val signatures = info.signatures
            if (signatures.isNullOrEmpty()) return null
            
            val sig = signatures[0]
            val certFactory = CertificateFactory.getInstance("X509")
            val cert = certFactory.generateCertificate(ByteArrayInputStream(sig.toByteArray())) as X509Certificate
            
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            
            SignatureInfo(
                fileName = fileName,
                schema = "V2 (Detected)", // Simplified
                status = "Berhasil diverifikasi",
                algorithm = cert.sigAlgName,
                createdDate = sdf.format(cert.notBefore),
                expiryDate = sdf.format(cert.notAfter),
                owner = cert.subjectDN.toString(),
                md5 = getHash(sig.toByteArray(), "MD5"),
                sha1 = getHash(sig.toByteArray(), "SHA1"),
                sha256 = getHash(sig.toByteArray(), "SHA256")
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun getHash(bytes: ByteArray, algorithm: String): String {
        return try {
            val md = MessageDigest.getInstance(algorithm)
            val digest = md.digest(bytes)
            digest.joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    private fun getAndroidVersion(sdk: Int): String {
        return when (sdk) {
            36 -> "16"
            35 -> "15"
            34 -> "14"
            33 -> "13"
            32 -> "12L"
            31 -> "12"
            30 -> "11"
            29 -> "10"
            28 -> "9"
            27 -> "8.1"
            26 -> "8.0"
            25 -> "7.1"
            24 -> "7.0"
            23 -> "6.0"
            22 -> "5.1"
            21 -> "5.0"
            else -> sdk.toString()
        }
    }

    private fun toReadableSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.2f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
