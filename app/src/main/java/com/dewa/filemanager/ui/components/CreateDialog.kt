package com.dewa.filemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dewa.filemanager.ui.theme.MTOnSurface
import com.dewa.filemanager.ui.theme.MTPrimary
import com.dewa.filemanager.ui.theme.MTSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Boolean) -> Unit // name, isFolder
) {
    var name by remember { mutableStateOf("") }
    
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
                    text = "Membuat",
                    color = MTOnSurface,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = MTPrimary,
                        unfocusedIndicatorColor = Color.Gray,
                        cursorColor = MTPrimary,
                        focusedTextColor = MTOnSurface,
                        unfocusedTextColor = MTOnSurface
                    ),
                    textStyle = TextStyle(fontSize = 18.sp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("MEMBATALKAN", color = MTPrimary)
                    }
                    TextButton(onClick = { if (name.isNotEmpty()) onCreate(name, false) }) {
                        Text("FILE", color = MTPrimary)
                    }
                    TextButton(onClick = { if (name.isNotEmpty()) onCreate(name, true) }) {
                        Text("FOLDER", color = MTPrimary)
                    }
                }
            }
        }
    }
}
