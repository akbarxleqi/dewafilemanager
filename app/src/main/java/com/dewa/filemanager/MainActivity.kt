package com.dewa.filemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dewa.filemanager.ui.explorer.ExplorerScreen
import com.dewa.filemanager.ui.theme.DewaManagerTheme
import com.dewa.filemanager.utils.PermissionManager
import com.dewa.filemanager.ui.editor.ArchiveEditTarget
import com.dewa.filemanager.data.repository.ArchiveRepository
import com.dewa.filemanager.ui.apkextractor.ApkExtractorScreen
import com.dewa.filemanager.ui.notes.TextNotesScreen
import com.dewa.filemanager.ui.password.PasswordManagerScreen
import com.dewa.filemanager.ui.signaturekey.SignatureKeyManagerScreen

enum class ViewerType { NONE, EDITOR, IMAGE, VIDEO, ARCHIVE, APK_EXTRACTOR, TEXT_NOTES, PASSWORD_MANAGER, SIGNATURE_KEY_MANAGER }

class MainActivity : ComponentActivity() {
    private val hasPermissionState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasPermissionState.value = PermissionManager.hasAllFilesAccess()
        enableEdgeToEdge()
        setContent {
            var isDarkMode by remember { mutableStateOf(true) }
            val hasPermission by hasPermissionState
            
            DewaManagerTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (hasPermission) {
                        var currentViewer by remember { mutableStateOf(ViewerType.NONE) }
                        var currentPathForViewer by remember { mutableStateOf<String?>(null) }
                        var currentArchiveEditTarget by remember { mutableStateOf<ArchiveEditTarget?>(null) }

                        when (currentViewer) {
                            ViewerType.NONE -> {
                                ExplorerScreen(
                                    isDarkMode = isDarkMode,
                                    onThemeToggle = { isDarkMode = !isDarkMode },
                                    onNavigateToEditor = { path, archiveTarget ->
                                        currentPathForViewer = path
                                        currentArchiveEditTarget = archiveTarget
                                        currentViewer = ViewerType.EDITOR
                                    },
                                    onNavigateToImageViewer = { path ->
                                        currentPathForViewer = path
                                        currentArchiveEditTarget = null
                                        currentViewer = ViewerType.IMAGE
                                    },
                                    onNavigateToVideoPlayer = { path ->
                                        currentPathForViewer = path
                                        currentArchiveEditTarget = null
                                        currentViewer = ViewerType.VIDEO
                                    },
                                    onNavigateToArchive = { path ->
                                        currentPathForViewer = path
                                        currentArchiveEditTarget = null
                                        currentViewer = ViewerType.ARCHIVE
                                    },
                                    onNavigateToApkExtractor = {
                                        currentPathForViewer = null
                                        currentArchiveEditTarget = null
                                        currentViewer = ViewerType.APK_EXTRACTOR
                                    },
                                    onNavigateToTextNotes = {
                                        currentPathForViewer = null
                                        currentArchiveEditTarget = null
                                        currentViewer = ViewerType.TEXT_NOTES
                                    },
                                    onNavigateToPasswordManager = {
                                        currentPathForViewer = null
                                        currentArchiveEditTarget = null
                                        currentViewer = ViewerType.PASSWORD_MANAGER
                                    },
                                    onNavigateToSignatureKeyManager = {
                                        currentPathForViewer = null
                                        currentArchiveEditTarget = null
                                        currentViewer = ViewerType.SIGNATURE_KEY_MANAGER
                                    }
                                )
                            }
                            ViewerType.EDITOR -> {
                                BackHandler { currentViewer = ViewerType.NONE }
                                currentPathForViewer?.let { path ->
                                    com.dewa.filemanager.ui.editor.EditorScreen(
                                        filePath = path,
                                        onSaveOverride = { content ->
                                            val target = currentArchiveEditTarget
                                            if (target == null) {
                                                null
                                            } else {
                                                ArchiveRepository.replaceArchiveTextEntry(
                                                    archivePath = target.archivePath,
                                                    entryPath = target.entryPath,
                                                    content = content,
                                                    password = target.password
                                                )
                                            }
                                        },
                                        onBack = {
                                            currentArchiveEditTarget = null
                                            currentViewer = ViewerType.NONE
                                        }
                                    )
                                }
                            }
                            ViewerType.IMAGE -> {
                                BackHandler { currentViewer = ViewerType.NONE }
                                currentPathForViewer?.let { path ->
                                    com.dewa.filemanager.ui.viewer.ImageViewerScreen(
                                        filePath = path,
                                        onBack = { currentViewer = ViewerType.NONE }
                                    )
                                }
                            }
                            ViewerType.VIDEO -> {
                                BackHandler { currentViewer = ViewerType.NONE }
                                currentPathForViewer?.let { path ->
                                    com.dewa.filemanager.ui.viewer.VideoPlayerScreen(
                                        filePath = path,
                                        onBack = { currentViewer = ViewerType.NONE }
                                    )
                                }
                            }
                            ViewerType.ARCHIVE -> {
                                BackHandler { currentViewer = ViewerType.NONE }
                                currentPathForViewer?.let { path ->
                                    com.dewa.filemanager.ui.viewer.ArchiveViewerScreen(
                                        filePath = path,
                                        onBack = { currentViewer = ViewerType.NONE }
                                    )
                                }
                            }
                            ViewerType.APK_EXTRACTOR -> {
                                BackHandler { currentViewer = ViewerType.NONE }
                                ApkExtractorScreen(
                                    onBack = { currentViewer = ViewerType.NONE }
                                )
                            }
                            ViewerType.TEXT_NOTES -> {
                                BackHandler { currentViewer = ViewerType.NONE }
                                TextNotesScreen(
                                    onBackToExplorer = { currentViewer = ViewerType.NONE }
                                )
                            }
                            ViewerType.PASSWORD_MANAGER -> {
                                BackHandler { currentViewer = ViewerType.NONE }
                                PasswordManagerScreen(
                                    onBack = { currentViewer = ViewerType.NONE }
                                )
                            }
                            ViewerType.SIGNATURE_KEY_MANAGER -> {
                                BackHandler { currentViewer = ViewerType.NONE }
                                SignatureKeyManagerScreen(
                                    onBack = { currentViewer = ViewerType.NONE }
                                )
                            }
                        }
                    } else {
                        PermissionRequestScreen {
                            PermissionManager.requestAllFilesAccess(this@MainActivity)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasPermissionState.value = PermissionManager.hasAllFilesAccess()
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
