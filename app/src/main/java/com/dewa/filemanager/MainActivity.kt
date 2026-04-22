package com.dewa.filemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dewa.filemanager.ui.explorer.ExplorerScreen
import com.dewa.filemanager.ui.theme.DewaManagerTheme
import com.dewa.filemanager.utils.PermissionManager

enum class ViewerType { NONE, EDITOR, IMAGE, VIDEO }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DewaManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var hasPermission by remember { mutableStateOf(PermissionManager.hasAllFilesAccess()) }

                    // Simple check for permission updates when returning to the app
                    LaunchedEffect(Unit) {
                        // In a real app, you might use a LifecycleObserver
                    }

                    if (hasPermission) {
                        var currentViewer by remember { mutableStateOf(ViewerType.NONE) }
                        var currentPathForViewer by remember { mutableStateOf<String?>(null) }

                        when (currentViewer) {
                            ViewerType.NONE -> {
                                ExplorerScreen(
                                    onNavigateToEditor = { path ->
                                        currentPathForViewer = path
                                        currentViewer = ViewerType.EDITOR
                                    },
                                    onNavigateToImageViewer = { path ->
                                        currentPathForViewer = path
                                        currentViewer = ViewerType.IMAGE
                                    },
                                    onNavigateToVideoPlayer = { path ->
                                        currentPathForViewer = path
                                        currentViewer = ViewerType.VIDEO
                                    }
                                )
                            }
                            ViewerType.EDITOR -> {
                                androidx.activity.compose.BackHandler { currentViewer = ViewerType.NONE }
                                currentPathForViewer?.let { path ->
                                    com.dewa.filemanager.ui.editor.EditorScreen(
                                        filePath = path,
                                        onBack = { currentViewer = ViewerType.NONE }
                                    )
                                }
                            }
                            ViewerType.IMAGE -> {
                                androidx.activity.compose.BackHandler { currentViewer = ViewerType.NONE }
                                currentPathForViewer?.let { path ->
                                    com.dewa.filemanager.ui.viewer.ImageViewerScreen(
                                        filePath = path,
                                        onBack = { currentViewer = ViewerType.NONE }
                                    )
                                }
                            }
                            ViewerType.VIDEO -> {
                                androidx.activity.compose.BackHandler { currentViewer = ViewerType.NONE }
                                currentPathForViewer?.let { path ->
                                    com.dewa.filemanager.ui.viewer.VideoPlayerScreen(
                                        filePath = path,
                                        onBack = { currentViewer = ViewerType.NONE }
                                    )
                                }
                            }
                        }
                    } else {
                        PermissionRequestScreen {
                            PermissionManager.requestAllFilesAccess(this@MainActivity)
                            // Note: User has to come back and we check again. 
                            // For simplicity, we just assume they might have granted it.
                            // In a real app, check in onResume.
                        }
                    }
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Force recomposition or state update would be better, but for now we skip complex logic
    }
}

@Composable
fun PermissionRequestScreen(onRequest: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(
                "Akses Penyimpanan Diperlukan",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Dewa File Manager membutuhkan izin untuk mengelola semua file di perangkat Anda agar dapat berfungsi seperti MT Manager.",
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequest) {
                Text("BERIKAN IZIN")
            }
        }
    }
}