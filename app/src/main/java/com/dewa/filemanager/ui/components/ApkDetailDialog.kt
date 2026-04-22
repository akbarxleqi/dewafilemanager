package com.dewa.filemanager.ui.components

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dewa.filemanager.utils.ApkInfo
import com.dewa.filemanager.ui.theme.MTOnSurface
import com.dewa.filemanager.ui.theme.MTPrimary
import com.dewa.filemanager.ui.theme.MTSurface
import com.dewa.filemanager.ui.theme.MTTextSecondary

@Composable
fun ApkDetailDialog(
    info: ApkInfo,
    onDismiss: () -> Unit,
    onInstall: () -> Unit
) {
    var showSignatureInfo by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    if (showSignatureInfo && info.signatureInfo != null) {
        SignatureInfoDialog(
            info = info.signatureInfo,
            onDismiss = { showSignatureInfo = false }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF2D2D30),
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    if (info.icon != null) {
                        Image(
                            bitmap = info.icon,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp).padding(end = 16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = info.appName,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = info.versionName,
                            color = MTTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }

                // Info Rows
                InfoRow("Nama paket", info.packageName) {
                    clipboardManager.setText(AnnotatedString(info.packageName))
                    Toast.makeText(context, "Nama paket disalin", Toast.LENGTH_SHORT).show()
                }
                InfoRow("Kode versi", info.versionCode.toString()) {
                    clipboardManager.setText(AnnotatedString(info.versionCode.toString()))
                    Toast.makeText(context, "Kode versi disalin", Toast.LENGTH_SHORT).show()
                }
                InfoRow("Ukuran file", info.fileSize) {
                    clipboardManager.setText(AnnotatedString(info.fileSize))
                    Toast.makeText(context, "Ukuran file disalin", Toast.LENGTH_SHORT).show()
                }
                InfoRow("Tanda tangan", info.signatureInfo?.schema ?: "V2 (Detected)") {
                    showSignatureInfo = true
                }
                InfoRow("Perlindungan", "Tidak terdeteksi") {
                    clipboardManager.setText(AnnotatedString("Tidak terdeteksi"))
                    Toast.makeText(context, "Keterangan disalin", Toast.LENGTH_SHORT).show()
                }
                InfoRow("SDK Target", info.targetSdk) {
                    clipboardManager.setText(AnnotatedString(info.targetSdk))
                    Toast.makeText(context, "SDK Target disalin", Toast.LENGTH_SHORT).show()
                }
                InfoRow("SDK Minimum", info.minSdk) {
                    clipboardManager.setText(AnnotatedString(info.minSdk))
                    Toast.makeText(context, "SDK Minimum disalin", Toast.LENGTH_SHORT).show()
                }
                InfoRow("Terpasang", info.installedVersion ?: "Tidak terpasang") {
                    clipboardManager.setText(AnnotatedString(info.installedVersion ?: "Tidak terpasang"))
                    Toast.makeText(context, "Status terpasang disalin", Toast.LENGTH_SHORT).show()
                }
                InfoRow("jalur APK", info.apkPath) {
                    clipboardManager.setText(AnnotatedString(info.apkPath))
                    Toast.makeText(context, "Jalur APK disalin", Toast.LENGTH_SHORT).show()
                }
                InfoRow("Dipasang", info.installDate ?: "-") {
                    clipboardManager.setText(AnnotatedString(info.installDate ?: "-"))
                    Toast.makeText(context, "Tanggal dipasang disalin", Toast.LENGTH_SHORT).show()
                }
                InfoRow("Diperbarui", info.updateDate ?: "-") {
                    clipboardManager.setText(AnnotatedString(info.updateDate ?: "-"))
                    Toast.makeText(context, "Tanggal diperbarui disalin", Toast.LENGTH_SHORT).show()
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("BATAL", color = Color(0xFF64B5F6))
                    }
                    TextButton(onClick = onInstall) {
                        Text("INSTAL", color = Color(0xFF64B5F6))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = MTTextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            modifier = Modifier.weight(0.6f)
        )
    }
}
