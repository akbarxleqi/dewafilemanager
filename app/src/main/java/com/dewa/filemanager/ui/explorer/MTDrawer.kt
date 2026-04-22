package com.dewa.filemanager.ui.explorer

import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dewa.filemanager.data.repository.FileManagerRepository
import com.dewa.filemanager.ui.components.StorageCard
import com.dewa.filemanager.ui.theme.MTBackground
import com.dewa.filemanager.ui.theme.MTOnSurface

@Composable
fun MTDrawer(
    drawerState: DrawerState,
    storageStats: FileManagerRepository.StorageStats?,
    content: @Composable () -> Unit
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (drawerState.isOpen) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { 
                        scope.launch { drawerState.close() }
                    }
                    .zIndex(10f)
            )

            // Drawer content
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .zIndex(11f),
                color = MTBackground
            ) {
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    Text(
                        "Manajer MT",
                        modifier = Modifier.padding(16.dp),
                        color = MTOnSurface,
                        style = MaterialTheme.typography.titleLarge
                    )
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                    NavigationDrawerItem(
                        label = { Text("Lokal") },
                        selected = true,
                        onClick = { },
                        icon = { Icon(Icons.Default.Storage, null) }
                    )
                    val stats = storageStats
                    if (stats != null) {
                        StorageCard(
                            title = "Penyimpanan Internal",
                            icon = Icons.Default.SdStorage,
                            usedBytes = stats.usedBytes,
                            totalBytes = stats.totalBytes,
                            availableBytes = stats.availableBytes
                        )
                    }
                }
            }
        }
    }
}
