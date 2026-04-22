package com.dewa.filemanager.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dewa.filemanager.data.model.FileEntity
import com.dewa.filemanager.ui.theme.MTOnSurface
import com.dewa.filemanager.ui.theme.MTTextSecondary
import com.dewa.filemanager.utils.toReadableDate
import com.dewa.filemanager.utils.toReadableSize

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRow(
    file: FileEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        var apkIcon by remember(file.path) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
        
        LaunchedEffect(file.path) {
            if (file.name.endsWith(".apk", ignoreCase = true)) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    apkIcon = com.dewa.filemanager.utils.ApkIconHelper.getApkIcon(context, file.path)
                }
            }
        }

        val specs = com.dewa.filemanager.utils.FileIconHelper.getIconSpecs(file)
        
        if (apkIcon != null) {
            androidx.compose.foundation.Image(
                bitmap = apkIcon!!,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        } else {
            Icon(
                imageVector = specs.icon,
                contentDescription = null,
                tint = specs.color,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(10.dp))
        
        Column {
            Text(
                text = file.name,
                color = MTOnSurface,
                fontSize = 13.sp,
                maxLines = 1
            )
            Row {
                Text(
                    text = file.lastModified.toReadableDate(),
                    color = MTTextSecondary,
                    fontSize = 10.sp
                )
                if (!file.isDirectory) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = file.size.toReadableSize(),
                        color = MTTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
