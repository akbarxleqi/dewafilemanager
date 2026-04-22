package com.dewa.filemanager.ui.explorer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dewa.filemanager.data.model.FileEntity
import com.dewa.filemanager.data.repository.FileManagerRepository
import com.dewa.filemanager.ui.components.*
import com.dewa.filemanager.ui.theme.MTBackground
import com.dewa.filemanager.ui.theme.MTOnSurface
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.dewa.filemanager.utils.FileOpener
import com.dewa.filemanager.utils.toReadableSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    onNavigateToEditor: (String) -> Unit = {},
    onNavigateToImageViewer: (String) -> Unit = {},
    onNavigateToVideoPlayer: (String) -> Unit = {}
) {
    val viewModel: ExplorerViewModel = viewModel()
    val leftPath by viewModel.leftPath.collectAsState()
    val rightPath by viewModel.rightPath.collectAsState()
    val leftFiles by viewModel.leftFiles.collectAsState()
    val rightFiles by viewModel.rightFiles.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()
    
    var activePane by remember { mutableIntStateOf(0) } // 0 for left, 1 for right
    var showCreateDialog by remember { mutableStateOf(false) }
    var currentCreatingPath by remember { mutableStateOf("") }
    
    var selectedFileForAction by remember { mutableStateOf<FileEntity?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showActionMenu by remember { mutableStateOf(false) }
    var showInstallDialog by remember { mutableStateOf(false) }
    var selectedApkInfo by remember { mutableStateOf<com.dewa.filemanager.utils.ApkInfo?>(null) }
    var isLeftSourceForAction by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val processingMessage by viewModel.processingMessage.collectAsState()
    
    val leftSearchQuery by viewModel.leftSearchQuery.collectAsState()
    val rightSearchQuery by viewModel.rightSearchQuery.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = isSearchActive || (activePane == 0 && leftPath != "/") || (activePane == 1 && rightPath != "/")) {
        if (isSearchActive) {
            isSearchActive = false
            viewModel.setLeftSearchQuery("")
            viewModel.setRightSearchQuery("")
        } else {
            val path = if (activePane == 0) leftPath else rightPath
            val parent = File(path).parent ?: "/"
            if (activePane == 0) viewModel.navigateLeft(parent) else viewModel.navigateRight(parent)
        }
    }

    MTDrawer(
        drawerState = drawerState,
        storageStats = storageStats
    ) {
        val currentPath = if (activePane == 0) leftPath else rightPath
        val currentFiles = if (activePane == 0) leftFiles else rightFiles
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
                    onRefresh = { viewModel.refreshAll() }
                )
            },
            bottomBar = {
                MTBottomBar(
                    onBack = { 
                        val path = if (activePane == 0) leftPath else rightPath
                        val parent = File(path).parent ?: path
                        if (activePane == 0) viewModel.navigateLeft(parent) else viewModel.navigateRight(parent)
                    },
                    onForward = { /* TODO */ },
                    onAdd = {
                        currentCreatingPath = if (activePane == 0) leftPath else rightPath
                        showCreateDialog = true
                    },
                    onTransfer = { /* TODO */ },
                    onUp = {
                        val path = if (activePane == 0) leftPath else rightPath
                        val parent = File(path).parent ?: path
                        if (activePane == 0) viewModel.navigateLeft(parent) else viewModel.navigateRight(parent)
                    }
                )
            }
        ) { innerPadding ->
            val context = androidx.compose.ui.platform.LocalContext.current
            Box(Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(innerPadding)) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.clickable { activePane = 0 }) {
                                Breadcrumb(
                                    path = leftPath,
                                    onPathClick = { 
                                        activePane = 0
                                        viewModel.navigateLeft(it) 
                                    },
                                    isActive = activePane == 0
                                )
                                FilePagerList(
                                    files = leftFiles,
                                    onFileClick = { file ->
                                        activePane = 0
                                        if (file.isDirectory) {
                                            viewModel.navigateLeft(file.path)
                                        } else if (file.name.endsWith(".apk", ignoreCase = true)) {
                                            selectedFileForAction = file
                                            selectedApkInfo = com.dewa.filemanager.utils.ApkInfoHelper.getApkInfo(context, file.path)
                                            showInstallDialog = true
                                        } else {
                                            val ext = file.name.substringAfterLast('.', "").lowercase()
                                            val editableExtensions = setOf("txt", "md", "log", "conf", "ini", "properties", "xml", "java", "kt", "kts", "js", "mjs", "ts", "html", "htm", "css", "scss", "sass", "less", "php", "py", "c", "cpp", "h", "hpp", "cs", "go", "rs", "rb", "swift", "dart", "smali", "json", "yml", "yaml", "sql", "sh", "bat")
                                            val isImage = ext in listOf("jpg", "jpeg", "png", "webp", "bmp", "gif")
                                            val isVideo = ext in listOf("mp4", "mkv", "avi", "webm", "mov")
                                            
                                            if (isImage) {
                                                onNavigateToImageViewer(file.path)
                                            } else if (isVideo) {
                                                onNavigateToVideoPlayer(file.path)
                                            } else if (ext in editableExtensions) {
                                                onNavigateToEditor(file.path)
                                            } else {
                                                FileOpener.openFile(context, file.path)
                                            }
                                        }
                                    },
                                    onFileLongClick = { file ->
                                        activePane = 0
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
                                    path = rightPath, 
                                    onPathClick = { 
                                        activePane = 1
                                        viewModel.navigateRight(it) 
                                    },
                                    isActive = activePane == 1
                                )
                                FilePagerList(
                                    files = rightFiles,
                                    onFileClick = { file ->
                                        activePane = 1
                                        if (file.isDirectory) {
                                            viewModel.navigateRight(file.path)
                                        } else if (file.name.endsWith(".apk", ignoreCase = true)) {
                                            selectedFileForAction = file
                                            selectedApkInfo = com.dewa.filemanager.utils.ApkInfoHelper.getApkInfo(context, file.path)
                                            showInstallDialog = true
                                        } else {
                                            val ext = file.name.substringAfterLast('.', "").lowercase()
                                            val editableExtensions = setOf("txt", "md", "log", "conf", "ini", "properties", "xml", "java", "kt", "kts", "js", "mjs", "ts", "html", "htm", "css", "scss", "sass", "less", "php", "py", "c", "cpp", "h", "hpp", "cs", "go", "rs", "rb", "swift", "dart", "smali", "json", "yml", "yaml", "sql", "sh", "bat")
                                            val isImage = ext in listOf("jpg", "jpeg", "png", "webp", "bmp", "gif")
                                            val isVideo = ext in listOf("mp4", "mkv", "avi", "webm", "mov")
                                            
                                            if (isImage) {
                                                onNavigateToImageViewer(file.path)
                                            } else if (isVideo) {
                                                onNavigateToVideoPlayer(file.path)
                                            } else if (ext in editableExtensions) {
                                                onNavigateToEditor(file.path)
                                            } else {
                                                FileOpener.openFile(context, file.path)
                                            }
                                        }
                                    },
                                    onFileLongClick = { file: FileEntity ->
                                        activePane = 1
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
                                viewModel.compressToZip(selectedFileForAction!!.path)
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
