package com.dewa.filemanager.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dewa.filemanager.utils.SignatureInfo

@Composable
fun SignatureInfoDialog(
    info: SignatureInfo,
    onDismiss: () -> Unit
) {
    var useUppercase by remember { mutableStateOf(true) }
    var useColons by remember { mutableStateOf(true) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val copyAction: (String, String) -> Unit = { label, value ->
        clipboardManager.setText(AnnotatedString(value))
        Toast.makeText(context, "$label disalin", Toast.LENGTH_SHORT).show()
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
                Text(
                    text = "Informasi tanda tangan",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                SigDetailRow("File", info.fileName) { copyAction("Nama file", info.fileName) }
                SigDetailRow("Skema", info.schema) { copyAction("Skema", info.schema) }
                SigDetailRow("Status", info.status) { copyAction("Status", info.status) }
                SigDetailRow("algoritma", info.algorithm) { copyAction("Algoritma", info.algorithm) }
                SigDetailRow("Dibuat", info.createdDate) { copyAction("Tanggal dibuat", info.createdDate) }
                SigDetailRow("Kedaluwarsa", info.expiryDate) { copyAction("Tanggal kedaluwarsa", info.expiryDate) }
                SigDetailRow("Pemilik", info.owner) { copyAction("Pemilik", info.owner) }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val md5 = formatHash(info.md5, useUppercase, useColons)
                val sha1 = formatHash(info.sha1, useUppercase, useColons)
                val sha256 = formatHash(info.sha256, useUppercase, useColons)

                SigDetailRow("MD5", md5) { copyAction("MD5", md5) }
                SigDetailRow("SHA1", sha1) { copyAction("SHA1", sha1) }
                SigDetailRow("SHA256", sha256) { copyAction("SHA256", sha256) }

                Spacer(modifier = Modifier.height(16.dp))

                // Formatting Settings
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Format", color = Color.White, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Tambahkan titik dua", color = Color.Gray, fontSize = 12.sp)
                        Switch(
                            checked = useColons,
                            onCheckedChange = { useColons = it },
                            modifier = Modifier.scale(0.7f)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Huruf besar", color = Color.Gray, fontSize = 12.sp)
                    Switch(
                        checked = useUppercase,
                        onCheckedChange = { useUppercase = it },
                        modifier = Modifier.scale(0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        val allInfo = """
                            File: ${info.fileName}
                            Skema: ${info.schema}
                            Status: ${info.status}
                            Algoritma: ${info.algorithm}
                            Dibuat: ${info.createdDate}
                            Kedaluwarsa: ${info.expiryDate}
                            Pemilik: ${info.owner}
                            MD5: $md5
                            SHA1: $sha1
                            SHA256: $sha256
                        """.trimIndent()
                        clipboardManager.setText(AnnotatedString(allInfo))
                        Toast.makeText(context, "Semua informasi disalin", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("SALIN SEMUA", color = Color(0xFF64B5F6))
                    }
                    TextButton(onClick = onDismiss) {
                        Text("TUTUP", color = Color(0xFF64B5F6))
                    }
                    TextButton(onClick = { /* Compare placeholder */ }) {
                        Text("MEMBANDINGKAN", color = Color(0xFF64B5F6))
                    }
                }
            }
        }
    }
}

@Composable
fun SigDetailRow(label: String, value: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.width(100.dp)
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun formatHash(hash: String, uppercase: Boolean, colons: Boolean): String {
    var result = hash
    if (!colons) result = result.replace(":", "")
    if (!uppercase) result = result.lowercase()
    return result
}
