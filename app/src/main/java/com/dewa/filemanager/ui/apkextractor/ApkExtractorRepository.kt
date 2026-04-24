package com.dewa.filemanager.ui.apkextractor

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.dewa.filemanager.data.repository.FileManagerRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ApkExtractorRepository {

    data class InstalledAppsResult(
        val userApps: List<InstalledAppItem>,
        val systemApps: List<InstalledAppItem>
    )

    fun loadInstalledApps(context: Context): InstalledAppsResult {
        val pm = context.packageManager
        val installed = pm.getInstalledPackages(PackageManager.GET_SIGNING_CERTIFICATES)

        val mapped = installed.mapNotNull { pkg ->
            runCatching { pkg.toInstalledAppItem(pm) }.getOrNull()
        }.sortedBy { it.appName.lowercase(Locale.ROOT) }

        val userApps = mapped.filterNot { it.isSystemApp }
        val systemApps = mapped.filter { it.isSystemApp }

        return InstalledAppsResult(userApps = userApps, systemApps = systemApps)
    }

    fun extractApkFiles(item: InstalledAppItem): ExtractApkResult {
        val managerRoot = FileManagerRepository().getManagerRootPath()
        val outputRoot = File(managerRoot, "apk-extract")
        if (!outputRoot.exists()) outputRoot.mkdirs()

        val safeFolderName = sanitizeFileName("${item.appName}_${item.packageName}")
        var appOutDir = File(outputRoot, safeFolderName)
        if (appOutDir.exists()) {
            val suffix = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            appOutDir = File(outputRoot, "${safeFolderName}_$suffix")
        }
        appOutDir.mkdirs()

        val copied = mutableListOf<String>()
        val allPaths = buildList {
            add(item.baseApkPath)
            addAll(item.splitApkPaths)
        }

        allPaths.forEachIndexed { index, srcPath ->
            val srcFile = File(srcPath)
            if (!srcFile.exists() || !srcFile.isFile) return@forEachIndexed

            val fileName = if (srcFile.name.endsWith(".apk", ignoreCase = true)) {
                srcFile.name
            } else {
                "${item.packageName}_${index + 1}.apk"
            }

            val outFile = File(appOutDir, fileName)
            srcFile.copyTo(outFile, overwrite = true)
            copied += outFile.absolutePath
        }

        if (copied.isEmpty()) {
            throw IllegalStateException("APK sumber tidak bisa dibaca")
        }

        return ExtractApkResult(
            outputDir = appOutDir.absolutePath,
            copiedFiles = copied
        )
    }

    fun formatSdkLabel(sdk: Int): String {
        return "Android ${androidVersionForSdk(sdk)} (API $sdk)"
    }

    fun formatDateTime(timeMillis: Long): String {
        if (timeMillis <= 0L) return "-"
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timeMillis))
    }

    fun formatMtSize(size: Long): String {
        if (size <= 0L) return "0B"
        val units = arrayOf("B", "K", "M", "G", "T")
        val digitGroups = (kotlin.math.log10(size.toDouble()) / kotlin.math.log10(1024.0)).toInt()
        val number = size / Math.pow(1024.0, digitGroups.toDouble())
        return String.format(Locale.US, "%.2f%s", number, units[digitGroups])
    }

    private fun PackageInfo.toInstalledAppItem(pm: PackageManager): InstalledAppItem {
        val appInfo = applicationInfo ?: return throw IllegalStateException("No application info")
        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

        val basePath = appInfo.sourceDir.orEmpty()
        val splitPaths = appInfo.splitSourceDirs?.toList().orEmpty()

        val baseSize = File(basePath).let { if (it.exists()) it.length() else 0L }
        val splitSize = splitPaths.sumOf { path -> File(path).let { if (it.exists()) it.length() else 0L } }

        val icon = runCatching { appInfo.loadIcon(pm).toImageBitmap() }.getOrNull()

        val versionCodeLong = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            longVersionCode
        } else {
            @Suppress("DEPRECATION")
            versionCode.toLong()
        }

        return InstalledAppItem(
            appName = appInfo.loadLabel(pm).toString(),
            packageName = packageName,
            versionName = versionName ?: "-",
            versionCode = versionCodeLong,
            isSystemApp = isSystem,
            baseApkPath = basePath,
            splitApkPaths = splitPaths,
            baseApkSizeBytes = baseSize,
            splitApkSizeBytes = splitSize,
            targetSdk = appInfo.targetSdkVersion,
            minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) appInfo.minSdkVersion else 1,
            dataDir = appInfo.dataDir ?: "-",
            deviceProtectedDataDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) appInfo.deviceProtectedDataDir ?: "-" else "-",
            firstInstallTime = firstInstallTime,
            lastUpdateTime = lastUpdateTime,
            uid = appInfo.uid,
            signatureScheme = detectSignatureScheme(this),
            icon = icon
        )
    }

    private fun detectSignatureScheme(pkg: PackageInfo): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && pkg.signingInfo != null) {
            return "V2/V3"
        }
        @Suppress("DEPRECATION")
        val hasLegacySign = !pkg.signatures.isNullOrEmpty()
        return if (hasLegacySign) "V1" else "Tidak terdeteksi"
    }

    private fun androidVersionForSdk(sdk: Int): String {
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

    private fun sanitizeFileName(input: String): String {
        return input.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "app" }
    }

    private fun android.graphics.drawable.Drawable.toImageBitmap(): ImageBitmap {
        return when (this) {
            is BitmapDrawable -> bitmap.asImageBitmap()
            else -> {
                val width = intrinsicWidth.takeIf { it > 0 } ?: 128
                val height = intrinsicHeight.takeIf { it > 0 } ?: 128
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                setBounds(0, 0, canvas.width, canvas.height)
                draw(canvas)
                bitmap.asImageBitmap()
            }
        }
    }
}
