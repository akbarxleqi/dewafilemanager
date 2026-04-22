package com.dewa.filemanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dewa.filemanager.ui.theme.MTOnSurface
import com.dewa.filemanager.ui.theme.MTPrimary
import com.dewa.filemanager.ui.theme.MTSurface

@Composable
fun ConfirmDeleteDialog(
    fileName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MTSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Konfirmasi Hapus",
                    color = MTOnSurface,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "Apakah Anda yakin ingin menghapus \"$fileName\"? Tindakan ini tidak dapat dibatalkan.",
                    color = MTOnSurface,
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("BATAL", color = MTPrimary)
                    }
                    TextButton(onClick = onConfirm) {
                        Text("HAPUS", color = MTPrimary)
                    }
                }
            }
        }
    }
}
