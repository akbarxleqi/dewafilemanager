package com.dewa.filemanager.ui.apkextractor

import androidx.compose.ui.graphics.ImageBitmap

data class InstalledAppItem(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val baseApkPath: String,
    val splitApkPaths: List<String>,
    val baseApkSizeBytes: Long,
    val splitApkSizeBytes: Long,
    val targetSdk: Int,
    val minSdk: Int,
    val dataDir: String,
    val deviceProtectedDataDir: String,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val uid: Int,
    val signatureScheme: String,
    val icon: ImageBitmap?
) {
    val totalSizeBytes: Long
        get() = baseApkSizeBytes + splitApkSizeBytes
}

data class ExtractApkResult(
    val outputDir: String,
    val copiedFiles: List<String>
)
