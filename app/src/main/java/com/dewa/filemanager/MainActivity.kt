package com.dewa.filemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dewa.filemanager.ui.explorer.ExplorerScreen
import com.dewa.filemanager.ui.theme.DewaManagerTheme
import com.dewa.filemanager.utils.PermissionManager
import com.dewa.filemanager.ui.editor.ArchiveEditTarget
import com.dewa.filemanager.data.repository.ArchiveRepository
import com.dewa.filemanager.ui.apkextractor.ApkExtractorScreen
import com.dewa.filemanager.ui.notes.TextNotesScreen
import com.dewa.filemanager.ui.password.PasswordManagerScreen
import com.dewa.filemanager.ui.signaturekey.SignatureKeyManagerScreen
import androidx.compose.ui.text.font.FontWeight

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
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                        colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Siapkan D-Manager",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp
                ),
                color = colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Agar bisa membaca, memindahkan, mengarsip, dan mengelola file perangkat, izinkan akses penyimpanan penuh.",
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onBackground.copy(alpha = 0.84f)
            )

            Spacer(modifier = Modifier.height(18.dp))

            ElevatedCard(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PermissionBenefitRow(
                        icon = Icons.Default.Folder,
                        title = "Akses semua folder",
                        description = "Telusuri storage internal dan SD card tanpa batas."
                    )
                    PermissionBenefitRow(
                        icon = Icons.Default.Security,
                        title = "Operasi file lebih aman",
                        description = "Salin, pindah, hapus, ekstrak, dan edit file dengan stabil."
                    )
                    PermissionBenefitRow(
                        icon = Icons.Default.Lock,
                        title = "Privasi tetap terkendali",
                        description = "Akses hanya dipakai untuk fitur file manager, bukan data cloud."
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.tertiaryContainer.copy(alpha = 0.45f)
            ) {
                Text(
                    text = "Kamu bisa ubah izin ini kapan saja lewat Pengaturan Android.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface.copy(alpha = 0.78f),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = onRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Berikan Izin Sekarang",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun PermissionBenefitRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colorScheme.primary.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurface.copy(alpha = 0.74f)
            )
        }
    }
}
