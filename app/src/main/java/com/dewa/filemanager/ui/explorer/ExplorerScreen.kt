package com.dewa.filemanager.ui.explorer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dewa.filemanager.data.model.FileEntity
import com.dewa.filemanager.data.repository.ArchiveRepository
import com.dewa.filemanager.data.repository.FileManagerRepository
import com.dewa.filemanager.ui.components.*
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.dewa.filemanager.utils.FileOpener
import com.dewa.filemanager.utils.toReadableSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    isDarkMode: Boolean = true,
    onThemeToggle: () -> Unit = {},
    onNavigateToEditor: (String, com.dewa.filemanager.ui.editor.ArchiveEditTarget?) -> Unit = { _, _ -> },
    onNavigateToImageViewer: (String) -> Unit = {},
    onNavigateToVideoPlayer: (String) -> Unit = {},
    onNavigateToArchive: (String) -> Unit = {},
    onNavigateToApkExtractor: () -> Unit = {},
    onNavigateToTextNotes: () -> Unit = {},
    onNavigateToPasswordManager: () -> Unit = {},
    onNavigateToSignatureKeyManager: () -> Unit = {}
) {
    val viewModel: ExplorerViewModel = viewModel()
    val leftPath by viewModel.leftPath.collectAsState()
    val rightPath by viewModel.rightPath.collectAsState()
    val leftFiles by viewModel.leftFiles.collectAsState()
    val rightFiles by viewModel.rightFiles.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()
    val leftArchiveState by viewModel.leftArchiveState.collectAsState()
    val rightArchiveState by viewModel.rightArchiveState.collectAsState()
    
    var activePane by remember { mutableIntStateOf(0) } // 0 for left, 1 for right
    var showCreateDialog by remember { mutableStateOf(false) }
    var currentCreatingPath by remember { mutableStateOf("") }
    
    var selectedFileForAction by remember { mutableStateOf<FileEntity?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showActionMenu by remember { mutableStateOf(false) }
    var showInstallDialog by remember { mutableStateOf(false) }
    var showZipPasswordDialog by remember { mutableStateOf(false) }
    var zipPasswordInput by remember { mutableStateOf("") }
    var selectedApkInfo by remember { mutableStateOf<com.dewa.filemanager.utils.ApkInfo?>(null) }
    var isLeftSourceForAction by remember { mutableStateOf(true) }
    var archivePasswordRequest by remember { mutableStateOf<ArchivePasswordRequest?>(null) }
    var archivePasswordInput by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val context = LocalContext.current

    val processingMessage by viewModel.processingMessage.collectAsState()
    
    val leftSearchQuery by viewModel.leftSearchQuery.collectAsState()
    val rightSearchQuery by viewModel.rightSearchQuery.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }

    fun getPaneArchive(isLeft: Boolean): ArchivePaneState? = if (isLeft) leftArchiveState else rightArchiveState
    fun setPaneArchive(isLeft: Boolean, value: ArchivePaneState?) {
        viewModel.setArchiveState(isLeftPane = isLeft, state = value)
    }

    fun setPaneArchivePath(isLeft: Boolean, path: String) {
        viewModel.setArchivePath(isLeftPane = isLeft, path = path)
    }

    suspend fun mountArchive(
        isLeft: Boolean,
        archiveRealPath: String,
        displayName: String,
        existingLayers: List<ArchiveLayer>,
        password: String?
    ) {
        when (val browse = withContext(Dispatchers.IO) {
            ArchiveRepository.browseArchive(archiveRealPath, password)
        }) {
            is ArchiveRepository.ArchiveBrowseResult.Success -> {
                val layer = ArchiveLayer(
                    realArchivePath = archiveRealPath,
                    displayName = displayName,
                    password = password,
                    currentPath = "",
                    entries = browse.entries
                )
                setPaneArchive(isLeft, ArchivePaneState(existingLayers + layer))
            }
            is ArchiveRepository.ArchiveBrowseResult.PasswordRequired -> {
                archivePasswordRequest = ArchivePasswordRequest(
                    isLeftPane = isLeft,
                    realArchivePath = archiveRealPath,
                    displayName = displayName,
                    existingLayers = existingLayers,
                    reason = "Arsip terkunci. Masukkan kata sandi."
                )
                archivePasswordInput = ""
            }
            is ArchiveRepository.ArchiveBrowseResult.InvalidPassword -> {
                archivePasswordRequest = ArchivePasswordRequest(
                    isLeftPane = isLeft,
                    realArchivePath = archiveRealPath,
                    displayName = displayName,
                    existingLayers = existingLayers,
                    reason = "Kata sandi salah. Coba lagi."
                )
            }
            is ArchiveRepository.ArchiveBrowseResult.Error -> {
                // No-op for now; avoid blocking explorer flow.
            }
        }
    }

    suspend fun refreshArchivePane(isLeft: Boolean) {
        val state = getPaneArchive(isLeft) ?: return
        val top = state.top
        when (val browse = withContext(Dispatchers.IO) {
            ArchiveRepository.browseArchive(top.realArchivePath, top.password)
        }) {
            is ArchiveRepository.ArchiveBrowseResult.Success -> {
                val updatedTop = top.copy(entries = browse.entries)
                setPaneArchive(isLeft, state.copy(layers = state.layers.dropLast(1) + updatedTop))
            }
            else -> Unit
        }
    }

    fun navigateArchiveUp(isLeft: Boolean): Boolean {
        return viewModel.navigateArchiveUp(isLeftPane = isLeft)
    }

    suspend fun openArchiveEntry(isLeft: Boolean, file: FileEntity) {
        val state = getPaneArchive(isLeft) ?: return
        val top = state.top
        val outputDir = File(context.cacheDir, "archive_open").apply { mkdirs() }
        val extracted = withContext(Dispatchers.IO) {
            ArchiveRepository.extractArchiveEntry(
                archivePath = top.realArchivePath,
                entryPath = file.path,
                destPath = outputDir.absolutePath,
                password = top.password
            )
        } ?: return

        if (file.name.isArchiveExt()) {
            mountArchive(
                isLeft = isLeft,
                archiveRealPath = extracted,
                displayName = file.name,
                existingLayers = state.layers,
                password = null
            )
            return
        }

        val ext = file.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val isImage = ext in listOf("jpg", "jpeg", "png", "webp", "bmp", "gif")
        val isVideo = ext in listOf("mp4", "mkv", "avi", "webm", "mov")
        if (isImage) {
            onNavigateToImageViewer(extracted)
        } else if (isVideo) {
            onNavigateToVideoPlayer(extracted)
        } else if (isEditableTextFileName(file.name)) {
            onNavigateToEditor(
                extracted,
                com.dewa.filemanager.ui.editor.ArchiveEditTarget(
                    archivePath = top.realArchivePath,
                    entryPath = file.path,
                    password = top.password
                )
            )
        } else {
            FileOpener.openFile(context, extracted)
        }
    }

    val activeArchive = if (activePane == 0) leftArchiveState else rightArchiveState
    val activeCanArchiveBack = activeArchive?.let { it.top.currentPath.isNotBlank() || it.layers.size > 1 } == true
    val activeFsPath = if (activePane == 0) leftPath else rightPath
    val canGoFsBack = activeFsPath != "/"

    androidx.activity.compose.BackHandler(enabled = isSearchActive || activeCanArchiveBack || activeArchive != null || canGoFsBack) {
        if (isSearchActive) {
            isSearchActive = false
            viewModel.setLeftSearchQuery("")
            viewModel.setRightSearchQuery("")
        } else if (activeArchive != null) {
            navigateArchiveUp(activePane == 0)
        } else {
            val path = if (activePane == 0) leftPath else rightPath
            val parent = File(path).parent ?: "/"
            if (activePane == 0) viewModel.navigateLeft(parent) else viewModel.navigateRight(parent)
        }
    }

    MTDrawer(
        drawerState = drawerState,
        storageStats = storageStats,
        isDarkMode = isDarkMode,
        onThemeToggle = onThemeToggle,
        onStorageClick = {
            activePane = 0
            isSearchActive = false
            viewModel.resetPanelsToRoot()
        },
        onRecycleBinClick = {
            isSearchActive = false
            viewModel.openRecycleBin(isLeftPane = activePane == 0)
        },
        onApkExtractClick = {
            onNavigateToApkExtractor()
        },
        onTextEditorClick = {
            onNavigateToTextNotes()
        },
        onPasswordManagerClick = {
            onNavigateToPasswordManager()
        },
        onSignatureKeyClick = {
            onNavigateToSignatureKeyManager()
        }
    ) {
        val displayedLeftPath = leftArchiveState?.let(::archiveDisplayPath) ?: leftPath
        val displayedRightPath = rightArchiveState?.let(::archiveDisplayPath) ?: rightPath
        val displayedLeftFiles = leftArchiveState?.let(::buildArchiveNodes) ?: leftFiles
        val displayedRightFiles = rightArchiveState?.let(::buildArchiveNodes) ?: rightFiles

        val currentPath = if (activePane == 0) displayedLeftPath else displayedRightPath
        val currentFiles = if (activePane == 0) displayedLeftFiles else displayedRightFiles
        val (folders, fileCount) = viewModel.getCounts(currentFiles)

        Scaffold(
            modifier = Modifier.systemBarsPadding(),
            topBar = {
                MTTopBar(
                    title = currentPath,
                    folderCount = folders,
                    fileCount = fileCount,
                    storageStats = storageStats,
                    searchQuery = if (activePane == 0) leftSearchQuery else rightSearchQuery,
                    isSearchActive = isSearchActive,
                    onSearchQueryChange = { query ->
                        if (activePane == 0) viewModel.setLeftSearchQuery(query) else viewModel.setRightSearchQuery(query)
                    },
                    onSearchToggle = { 
                        isSearchActive = !isSearchActive 
                        if (!isSearchActive) {
                            viewModel.setLeftSearchQuery("")
                            viewModel.setRightSearchQuery("")
                        }
                    },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onRefresh = {
                        scope.launch {
                            if (activePane == 0 && leftArchiveState != null) {
                                refreshArchivePane(isLeft = true)
                            } else if (activePane == 1 && rightArchiveState != null) {
                                refreshArchivePane(isLeft = false)
                            } else {
                                viewModel.refreshAll()
                            }
                        }
                    }
                )
            },
            bottomBar = {
                MTBottomBar(
                    onBack = { 
                        if (activePane == 0 && leftArchiveState != null) {
                            navigateArchiveUp(isLeft = true)
                        } else if (activePane == 1 && rightArchiveState != null) {
                            navigateArchiveUp(isLeft = false)
                        } else {
                            val path = if (activePane == 0) leftPath else rightPath
                            val parent = File(path).parent ?: path
                            if (activePane == 0) viewModel.navigateLeft(parent) else viewModel.navigateRight(parent)
                        }
                    },
                    onForward = { /* TODO */ },
                    onAdd = {
                        if (activePane == 0 && leftArchiveState == null) {
                            currentCreatingPath = leftPath
                            showCreateDialog = true
                        } else if (activePane == 1 && rightArchiveState == null) {
                            currentCreatingPath = rightPath
                            showCreateDialog = true
                        }
                    },
                    onTransfer = { /* TODO */ },
                    onUp = {
                        if (activePane == 0 && leftArchiveState != null) {
                            navigateArchiveUp(isLeft = true)
                        } else if (activePane == 1 && rightArchiveState != null) {
                            navigateArchiveUp(isLeft = false)
                        } else {
                            val path = if (activePane == 0) leftPath else rightPath
                            val parent = File(path).parent ?: path
                            if (activePane == 0) viewModel.navigateLeft(parent) else viewModel.navigateRight(parent)
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(innerPadding)) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.clickable { activePane = 0 }) {
                                Breadcrumb(
                                    path = displayedLeftPath,
                                    onPathClick = { 
                                        activePane = 0
                                        if (leftArchiveState != null) {
                                            val state = leftArchiveState!!
                                            val full = it.trim('/')
                                            val stackPrefix = state.layers.joinToString("/") { layer -> layer.displayName }
                                            val relative = full.removePrefix(stackPrefix).trim('/').let { rel ->
                                                if (rel.isBlank()) "" else "$rel/"
                                            }
                                            setPaneArchivePath(isLeft = true, path = relative)
                                        } else {
                                            viewModel.navigateLeft(it)
                                        }
                                    },
                                    isActive = activePane == 0
                                )
                                FilePagerList(
                                    files = displayedLeftFiles,
                                    onFileClick = { file ->
                                        if (activePane != 0) {
                                            activePane = 0
                                            return@FilePagerList
                                        }
                                        activePane = 0
                                        val archiveState = leftArchiveState
                                        if (archiveState != null) {
                                            if (file.isDirectory) {
                                                setPaneArchivePath(isLeft = true, path = file.path)
                                            } else {
                                                scope.launch { openArchiveEntry(isLeft = true, file = file) }
                                            }
                                        } else if (file.isDirectory) {
                                            viewModel.navigateLeft(file.path)
                                        } else if (file.name.endsWith(".apk", ignoreCase = true)) {
                                            selectedFileForAction = file
                                            selectedApkInfo = com.dewa.filemanager.utils.ApkInfoHelper.getApkInfo(context, file.path)
                                            showInstallDialog = true
                                        } else {
                                            val ext = file.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
                                            val isImage = ext in listOf("jpg", "jpeg", "png", "webp", "bmp", "gif")
                                            val isVideo = ext in listOf("mp4", "mkv", "avi", "webm", "mov")
                                            
                                            if (file.name.isArchiveExt()) {
                                                scope.launch {
                                                    mountArchive(
                                                        isLeft = true,
                                                        archiveRealPath = file.path,
                                                        displayName = file.name,
                                                        existingLayers = emptyList(),
                                                        password = null
                                                    )
                                                }
                                            } else if (isImage) {
                                                onNavigateToImageViewer(file.path)
                                            } else if (isVideo) {
                                                onNavigateToVideoPlayer(file.path)
                                            } else if (isEditableTextFileName(file.name)) {
                                                onNavigateToEditor(file.path, null)
                                            } else {
                                                FileOpener.openFile(context, file.path)
                                            }
                                        }
                                    },
                                    onFileLongClick = { file ->
                                        if (activePane != 0) {
                                            activePane = 0
                                            return@FilePagerList
                                        }
                                        activePane = 0
                                        if (leftArchiveState != null) return@FilePagerList
                                        selectedFileForAction = file
                                        isLeftSourceForAction = true
                                        showActionMenu = true
                                    }
                                )
                            }
                        }
                        
                        VerticalDivider(color = Color.Gray.copy(alpha = 0.3f))
                        
                        Box(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.clickable { activePane = 1 }) {
                                Breadcrumb(
                                    path = displayedRightPath, 
                                    onPathClick = { 
                                        activePane = 1
                                        if (rightArchiveState != null) {
                                            val state = rightArchiveState!!
                                            val full = it.trim('/')
                                            val stackPrefix = state.layers.joinToString("/") { layer -> layer.displayName }
                                            val relative = full.removePrefix(stackPrefix).trim('/').let { rel ->
                                                if (rel.isBlank()) "" else "$rel/"
                                            }
                                            setPaneArchivePath(isLeft = false, path = relative)
                                        } else {
                                            viewModel.navigateRight(it)
                                        }
                                    },
                                    isActive = activePane == 1
                                )
                                FilePagerList(
                                    files = displayedRightFiles,
                                    onFileClick = { file ->
                                        if (activePane != 1) {
                                            activePane = 1
                                            return@FilePagerList
                                        }
                                        activePane = 1
                                        val archiveState = rightArchiveState
                                        if (archiveState != null) {
                                            if (file.isDirectory) {
                                                setPaneArchivePath(isLeft = false, path = file.path)
                                            } else {
                                                scope.launch { openArchiveEntry(isLeft = false, file = file) }
                                            }
                                        } else if (file.isDirectory) {
                                            viewModel.navigateRight(file.path)
                                        } else if (file.name.endsWith(".apk", ignoreCase = true)) {
                                            selectedFileForAction = file
                                            selectedApkInfo = com.dewa.filemanager.utils.ApkInfoHelper.getApkInfo(context, file.path)
                                            showInstallDialog = true
                                        } else {
                                            val ext = file.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
                                            val isImage = ext in listOf("jpg", "jpeg", "png", "webp", "bmp", "gif")
                                            val isVideo = ext in listOf("mp4", "mkv", "avi", "webm", "mov")
                                            
                                            if (file.name.isArchiveExt()) {
                                                scope.launch {
                                                    mountArchive(
                                                        isLeft = false,
                                                        archiveRealPath = file.path,
                                                        displayName = file.name,
                                                        existingLayers = emptyList(),
                                                        password = null
                                                    )
                                                }
                                            } else if (isImage) {
                                                onNavigateToImageViewer(file.path)
                                            } else if (isVideo) {
                                                onNavigateToVideoPlayer(file.path)
                                            } else if (isEditableTextFileName(file.name)) {
                                                onNavigateToEditor(file.path, null)
                                            } else {
                                                FileOpener.openFile(context, file.path)
                                            }
                                        }
                                    },
                                    onFileLongClick = { file: FileEntity ->
                                        if (activePane != 1) {
                                            activePane = 1
                                            return@FilePagerList
                                        }
                                        activePane = 1
                                        if (rightArchiveState != null) return@FilePagerList
                                        selectedFileForAction = file
                                        isLeftSourceForAction = false
                                        showActionMenu = true
                                    }
                                )
                            }
                        }
                    }
                }

                val processingMsg = processingMessage
                if (processingMsg != null) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Black.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = processingMsg,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, isFolder ->
                viewModel.createItem(currentCreatingPath, name, isFolder)
                showCreateDialog = false
            }
        )
    }

    if (showActionMenu && selectedFileForAction != null) {
        val sourcePath = if (isLeftSourceForAction) leftPath else rightPath
        val targetPath = if (isLeftSourceForAction) rightPath else leftPath
        AlertDialog(
            onDismissRequest = { showActionMenu = false },
            title = { 
                Text(
                    selectedFileForAction!!.name, 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                ) 
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Transfer Actions
                    ActionMenuItem(
                        icon = Icons.Default.ContentCopy,
                        label = "Salin ke panel sebelah",
                        onClick = {
                            showActionMenu = false
                            viewModel.transferFile(selectedFileForAction!!.path, targetPath, isMove = false)
                        }
                    )
                    ActionMenuItem(
                        icon = Icons.Default.DriveFileMove,
                        label = "Pindahkan ke panel sebelah",
                        onClick = {
                            showActionMenu = false
                            viewModel.transferFile(selectedFileForAction!!.path, targetPath, isMove = true)
                        }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))
                    
                    // Archive Actions
                    if (selectedFileForAction!!.name.endsWith(".zip", ignoreCase = true)) {
                        ActionMenuItem(
                            icon = Icons.Default.Unarchive,
                            label = "Ekstrak di sini",
                            onClick = {
                                showActionMenu = false
                                viewModel.extractZip(selectedFileForAction!!.path, sourcePath)
                            }
                        )
                    } else {
                        ActionMenuItem(
                            icon = Icons.Default.FolderZip,
                            label = "Kompres ke ZIP",
                            onClick = {
                                showActionMenu = false
                                zipPasswordInput = ""
                                showZipPasswordDialog = true
                            }
                        )
                    }

                    // Management Actions
                    ActionMenuItem(
                        icon = Icons.Default.Edit,
                        label = "Ubah Nama",
                        onClick = {
                            showActionMenu = false
                            showRenameDialog = true
                        }
                    )
                    ActionMenuItem(
                        icon = Icons.Default.Delete,
                        label = "Hapus",
                        color = Color.Red,
                        onClick = {
                            showActionMenu = false
                            showDeleteDialog = true
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showActionMenu = false }) {
                    Text("BATAL")
                }
            }
        )
    }

    if (showRenameDialog && selectedFileForAction != null) {
        RenameDialog(
            initialName = selectedFileForAction!!.name,
            onDismiss = { showRenameDialog = false },
            onRename = { newName ->
                viewModel.renameItem(selectedFileForAction!!.path, newName)
                showRenameDialog = false
            }
        )
    }

    if (showDeleteDialog && selectedFileForAction != null) {
        ConfirmDeleteDialog(
            fileName = selectedFileForAction!!.name,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.deleteItem(selectedFileForAction!!.path)
                showDeleteDialog = false
            }
        )
    }

    if (showZipPasswordDialog && selectedFileForAction != null) {
        AlertDialog(
            onDismissRequest = { showZipPasswordDialog = false },
            title = { Text("Kompres ke ZIP") },
            text = {
                Column {
                    Text(
                        "Opsional: isi kata sandi jika ingin ZIP terkunci.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = zipPasswordInput,
                        onValueChange = { zipPasswordInput = it },
                        singleLine = true,
                        label = { Text("Kata sandi ZIP (opsional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val password = zipPasswordInput.trim().ifBlank { null }
                    viewModel.compressToZip(selectedFileForAction!!.path, password)
                    showZipPasswordDialog = false
                }) { Text("Kompres") }
            },
            dismissButton = {
                TextButton(onClick = { showZipPasswordDialog = false }) { Text("Batal") }
            }
        )
    }

    archivePasswordRequest?.let { req ->
        AlertDialog(
            onDismissRequest = { archivePasswordRequest = null },
            title = { Text("Arsip Terkunci") },
            text = {
                Column {
                    Text(
                        text = req.reason,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = archivePasswordInput,
                        onValueChange = { archivePasswordInput = it },
                        singleLine = true,
                        label = { Text("Kata sandi") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pwd = archivePasswordInput
                        scope.launch {
                            mountArchive(
                                isLeft = req.isLeftPane,
                                archiveRealPath = req.realArchivePath,
                                displayName = req.displayName,
                                existingLayers = req.existingLayers,
                                password = pwd
                            )
                        }
                        archivePasswordRequest = null
                    },
                    enabled = archivePasswordInput.isNotBlank()
                ) { Text("Buka") }
            },
            dismissButton = {
                TextButton(onClick = { archivePasswordRequest = null }) { Text("Batal") }
            }
        )
    }

    if (showInstallDialog && selectedFileForAction != null && selectedApkInfo != null) {
        val context = androidx.compose.ui.platform.LocalContext.current
        ApkDetailDialog(
            info = selectedApkInfo!!,
            onDismiss = { 
                showInstallDialog = false
                selectedApkInfo = null
            },
            onInstall = {
                com.dewa.filemanager.utils.ApkInstaller.install(context, selectedFileForAction!!.path)
                showInstallDialog = false
                selectedApkInfo = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MTTopBar(
    title: String, 
    folderCount: Int, 
    fileCount: Int, 
    storageStats: FileManagerRepository.StorageStats?,
    searchQuery: String,
    isSearchActive: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onMenuClick: () -> Unit, 
    onRefresh: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Column {
            TopAppBar(
                title = { 
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search files...", fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        )
                    } else {
                        Text(
                            title, 
                            style = MaterialTheme.typography.labelMedium, 
                            maxLines = 1, 
                            overflow = TextOverflow.Ellipsis
                        ) 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                },
                actions = {
                    IconButton(onClick = onSearchToggle, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (isSearchActive) Icons.Default.Close else Icons.Default.Search, 
                            contentDescription = null, 
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = WindowInsets(0.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Folders: $folderCount Files: $fileCount",
                    style = TextStyle(fontSize = 10.sp),
                    color = Color.Gray
                )
                
                storageStats?.let { stats ->
                    Text(
                        text = "Storage: ${stats.usedBytes.toReadableSize()}/${stats.totalBytes.toReadableSize()}",
                        style = TextStyle(fontSize = 10.sp),
                        color = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
fun MTBottomBar(onBack: () -> Unit, onForward: () -> Unit, onAdd: () -> Unit, onTransfer: () -> Unit, onUp: () -> Unit) {
    BottomAppBar(
        actions = {
            IconButton(onClick = onBack) { Icon(Icons.Default.ChevronLeft, null) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onForward) { Icon(Icons.Default.ChevronRight, null) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onAdd) { Icon(Icons.Default.Add, null) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onTransfer) { Icon(Icons.Default.SwapHoriz, null) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onUp) { Icon(Icons.Default.ArrowUpward, null) }
        }
    )
}

@Composable
fun FilePagerList(
    files: List<FileEntity>,
    onFileClick: (FileEntity) -> Unit,
    onFileLongClick: (FileEntity) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(files) { file ->
            FileRow(
                file = file, 
                onClick = { onFileClick(file) },
                onLongClick = { onFileLongClick(file) }
            )
        }
    }
}

@Composable
fun ActionMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = color
            )
        }
    }
}
