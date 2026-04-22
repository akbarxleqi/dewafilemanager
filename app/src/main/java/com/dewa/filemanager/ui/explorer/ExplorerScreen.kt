package com.dewa.filemanager.ui.explorer

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
    onNavigateToEditor: (String) -> Unit = {}
) {
    val viewModel: ExplorerViewModel = viewModel()
    val leftPath by viewModel.leftPath.collectAsState()
    val rightPath by viewModel.rightPath.collectAsState()
    val leftFiles by viewModel.leftFiles.collectAsState()
    val rightFiles by viewModel.rightFiles.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()
    
    val pagerState = rememberPagerState(pageCount = { 2 })
    var showCreateDialog by remember { mutableStateOf(false) }
    var currentCreatingPath by remember { mutableStateOf("") }
    
    var selectedFileForAction by remember { mutableStateOf<FileEntity?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showActionMenu by remember { mutableStateOf(false) }
    var showInstallDialog by remember { mutableStateOf(false) }
    var isLeftSourceForAction by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val isProcessing by viewModel.isProcessing.collectAsState()

    MTDrawer(
        drawerState = drawerState,
        storageStats = storageStats
    ) {
        val currentPath = if (pagerState.currentPage == 0) leftPath else rightPath
        val currentFiles = if (pagerState.currentPage == 0) leftFiles else rightFiles
        val (folders, fileCount) = viewModel.getCounts(currentFiles)

        Scaffold(
            topBar = {
                MTTopBar(
                    title = currentPath,
                    folderCount = folders,
                    fileCount = fileCount,
                    storageStats = storageStats,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onRefresh = { viewModel.refreshAll() }
                )
            },
            bottomBar = {
                MTBottomBar(
                    onBack = { /* TODO */ },
                    onForward = { /* TODO */ },
                    onAdd = {
                        currentCreatingPath = if (pagerState.currentPage == 0) leftPath else rightPath
                        showCreateDialog = true
                    },
                    onTransfer = { /* TODO */ },
                    onUp = {
                        val path = if (pagerState.currentPage == 0) leftPath else rightPath
                        val parent = File(path).parent ?: path
                        if (pagerState.currentPage == 0) viewModel.navigateLeft(parent) else viewModel.navigateRight(parent)
                    }
                )
            }
        ) { innerPadding ->
            val context = androidx.compose.ui.platform.LocalContext.current
            Box(Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(innerPadding)) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            Column {
                                Breadcrumb(path = leftPath, onPathClick = { viewModel.navigateLeft(it) })
                                FilePagerList(
                                    files = leftFiles,
                                    onFileClick = { file ->
                                        if (file.isDirectory) {
                                            viewModel.navigateLeft(file.path)
                                        } else if (file.name.endsWith(".apk", ignoreCase = true)) {
                                            selectedFileForAction = file
                                            showInstallDialog = true
                                        } else {
                                            FileOpener.openFile(context, file.path)
                                        }
                                    },
                                    onFileLongClick = { file ->
                                        selectedFileForAction = file
                                        isLeftSourceForAction = true
                                        showActionMenu = true
                                    }
                                )
                            }
                        }
                        
                        VerticalDivider(color = Color.Gray.copy(alpha = 0.3f))
                        
                        Box(modifier = Modifier.weight(1f)) {
                            Column {
                                Breadcrumb(path = rightPath, onPathClick = { viewModel.navigateRight(it) })
                                FilePagerList(
                                    files = rightFiles,
                                    onFileClick = { file ->
                                        if (file.isDirectory) {
                                            viewModel.navigateRight(file.path)
                                        } else if (file.name.endsWith(".apk", ignoreCase = true)) {
                                            selectedFileForAction = file
                                            showInstallDialog = true
                                        } else {
                                            FileOpener.openFile(context, file.path)
                                        }
                                    },
                                    onFileLongClick = { file: FileEntity ->
                                        selectedFileForAction = file
                                        isLeftSourceForAction = false
                                        showActionMenu = true
                                    }
                                )
                            }
                        }
                    }
                }

                if (isProcessing) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Black.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator()
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

    if (showInstallDialog && selectedFileForAction != null) {
        val context = androidx.compose.ui.platform.LocalContext.current
        InstallApkDialog(
            fileName = selectedFileForAction!!.name,
            onDismiss = { showInstallDialog = false },
            onInstall = {
                com.dewa.filemanager.utils.ApkInstaller.install(context, selectedFileForAction!!.path)
                showInstallDialog = false
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
    onMenuClick: () -> Unit, 
    onRefresh: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Column {
            TopAppBar(
                title = { 
                    Text(
                        title, 
                        style = MaterialTheme.typography.labelMedium, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                },
                actions = {
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
