package com.dewa.filemanager.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
        var customIcon by remember(file.path) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
        
        LaunchedEffect(file.path) {
            val lowerName = file.name.lowercase()
            val isApk = lowerName.endsWith(".apk") || lowerName.endsWith(".apks")
            val isImage = lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp") || lowerName.endsWith(".bmp")
            val isVideo = lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") || lowerName.endsWith(".avi") || lowerName.endsWith(".webm") || lowerName.endsWith(".mov")
            
            if (isApk || isImage || isVideo) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    customIcon = if (isApk) {
                        com.dewa.filemanager.utils.ApkIconHelper.getApkIcon(context, file.path)
                    } else if (isVideo) {
                        com.dewa.filemanager.utils.ImageThumbnailHelper.getVideoThumbnail(file.path)
                    } else {
                        com.dewa.filemanager.utils.ImageThumbnailHelper.getThumbnail(file.path)
                    }
                }
            }
        }

        val specs = com.dewa.filemanager.utils.FileIconHelper.getIconSpecs(file)
        
        if (customIcon != null) {
            androidx.compose.foundation.Image(
                bitmap = customIcon!!,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        } else if (file.isDirectory) {
            Icon(
                imageVector = specs.icon,
                contentDescription = null,
                tint = specs.color,
                modifier = Modifier.size(28.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = specs.color,
                        shape = RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (specs.text != null) {
                    androidx.compose.material3.Text(
                        text = specs.text,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = specs.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(10.dp))
        
        Column {
            Text(
                text = file.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                maxLines = 1
            )
            Row {
                Text(
                    text = file.lastModified.toReadableDate(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
                if (!file.isDirectory) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = file.size.toReadableSize(),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
