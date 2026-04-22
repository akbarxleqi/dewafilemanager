package com.dewa.filemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dewa.filemanager.ui.theme.MTOnSurface

@Composable
fun Breadcrumb(
    path: String,
    onPathClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val parts = path.split("/").filter { it.isNotEmpty() }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        // Root icon or "/"
        Text(
            text = "/",
            color = MTOnSurface,
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
                color = MTOnSurface,
                fontSize = 13.sp,
                modifier = Modifier.clickable { onPathClick(currentPath) },
                maxLines = 1
            )
        }
    }
}
