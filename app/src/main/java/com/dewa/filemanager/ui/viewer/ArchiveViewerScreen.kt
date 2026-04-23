package com.dewa.filemanager.ui.viewer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dewa.filemanager.data.repository.ArchiveRepository
import com.dewa.filemanager.utils.FileOpener
import com.dewa.filemanager.utils.toReadableSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private data class ArchiveNode(
    val name: String,
    val fullPath: String,
    val isDirectory: Boolean,
    val size: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveViewerScreen(
    filePath: String,
    onBack: () -> Unit
) {
    val snackbarHost = remember { SnackbarHostState() }
    val archiveName = remember(filePath) { File(filePath).name }
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var currentPath by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<ArchiveRepository.ArchiveEntryInfo>>(emptyList()) }

    var password by remember { mutableStateOf<String?>(null) }
    var passwordInput by remember { mutableStateOf("") }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val ext = remember(filePath) { File(filePath).extension.lowercase() }

    fun parentPath(path: String): String {
        val clean = path.trim('/').split('/').filter { it.isNotBlank() }
        if (clean.isEmpty()) return ""
        return clean.dropLast(1).joinToString("/").let { if (it.isBlank()) "" else "$it/" }
    }

    suspend fun loadArchive() {
        isLoading = true
        when (val result = withContext(Dispatchers.IO) { ArchiveRepository.browseArchive(filePath, password) }) {
            is ArchiveRepository.ArchiveBrowseResult.Success -> {
                entries = result.entries
                passwordError = null
                showPasswordDialog = false
            }
            is ArchiveRepository.ArchiveBrowseResult.PasswordRequired -> {
                showPasswordDialog = true
                passwordError = "Arsip ini terkunci. Masukkan kata sandi."
            }
            is ArchiveRepository.ArchiveBrowseResult.InvalidPassword -> {
                showPasswordDialog = true
                passwordError = "Kata sandi salah. Coba lagi."
            }
            is ArchiveRepository.ArchiveBrowseResult.Error -> {
                snackbarHost.showSnackbar(result.message)
            }
        }
        isLoading = false
    }

    fun currentNodes(): List<ArchiveNode> {
        val prefix = if (currentPath.isBlank()) "" else currentPath
        val map = linkedMapOf<String, ArchiveNode>()

        entries.forEach { entry ->
            val normalized = entry.fullPath.replace('\\', '/')
            if (!normalized.startsWith(prefix) || normalized == prefix) return@forEach

            val remaining = normalized.removePrefix(prefix)
            if (remaining.isBlank()) return@forEach

            val segment = remaining.substringBefore('/')
            val hasChildren = remaining.contains('/')
            val isDir = hasChildren || entry.isDirectory
            val full = if (isDir) "$prefix$segment/" else "$prefix$segment"

            if (!map.containsKey(full)) {
                map[full] = ArchiveNode(
                    name = segment,
                    fullPath = full,
                    isDirectory = isDir,
                    size = if (isDir) 0L else entry.size
                )
            }
        }

        return map.values.sortedWith(
            compareByDescending<ArchiveNode> { it.isDirectory }.thenBy { it.name.lowercase() }
        )
    }

    suspend fun openNode(node: ArchiveNode) {
        if (node.isDirectory) {
            currentPath = node.fullPath
            return
        }

        val outputDir = File(context.cacheDir, "archive_open").apply { mkdirs() }
        val extracted = withContext(Dispatchers.IO) {
            ArchiveRepository.extractArchiveEntry(
                archivePath = filePath,
                entryPath = node.fullPath,
                destPath = outputDir.absolutePath,
                password = password
            )
        }

        if (extracted != null) {
            FileOpener.openFile(context, extracted)
        } else {
            snackbarHost.showSnackbar("Gagal membuka file dari arsip")
        }
    }

    LaunchedEffect(filePath, password) {
        loadArchive()
    }

    BackHandler(enabled = currentPath.isNotBlank()) {
        currentPath = parentPath(currentPath)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (currentPath.isBlank()) archiveName else "$archiveName/${currentPath.trimEnd('/')}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentPath.isNotBlank()) currentPath = parentPath(currentPath) else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Membaca arsip...")
                }
            } else {
                val nodes = currentNodes()
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(nodes) { node ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { openNode(node) }
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (node.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = if (node.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.size(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(node.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (!node.isDirectory) {
                                    Text(
                                        node.size.toReadableSize(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Arsip Terkunci") },
            text = {
                Column {
                    if (!passwordError.isNullOrBlank()) {
                        Text(
                            text = passwordError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Kata sandi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        password = passwordInput
                    },
                    enabled = passwordInput.isNotBlank()
                ) { Text("Buka") }
            },
            dismissButton = {
                TextButton(onClick = onBack) { Text("Batal") }
            }
        )
    }
}
