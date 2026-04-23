package com.dewa.filemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Breadcrumb(
    path: String,
    onPathClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val scrollState = rememberScrollState()
    val parts = path.split("/").filter { it.isNotEmpty() }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .background(if (isActive) Color.Gray.copy(alpha = 0.15f) else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        // Root icon or "/"
        Text(
            text = "/",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            modifier = Modifier.clickable { onPathClick("/") }
        )
        
        parts.forEachIndexed { index, part ->
            Text(
                text = "/",
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            val currentPath = "/" + parts.take(index + 1).joinToString("/")
            Text(
                text = part,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                modifier = Modifier.clickable { onPathClick(currentPath) },
                maxLines = 1
            )
        }
    }
}
