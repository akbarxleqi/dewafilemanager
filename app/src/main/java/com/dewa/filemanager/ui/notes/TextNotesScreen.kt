package com.dewa.filemanager.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dewa.filemanager.data.repository.NoteFile
import com.dewa.filemanager.data.repository.NotesRepository
import com.dewa.filemanager.ui.components.ConfirmDeleteDialog
import com.dewa.filemanager.ui.editor.EditorScreen
import com.dewa.filemanager.utils.toReadableDate
import com.dewa.filemanager.utils.toReadableSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextNotesScreen(
    onBackToExplorer: () -> Unit
) {
    val repository = remember { NotesRepository() }
    val scope = rememberCoroutineScope()

    var notes by remember { mutableStateOf<List<NoteFile>>(emptyList()) }
    var selectedNotePath by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newNoteName by remember { mutableStateOf("") }
    var noteToDelete by remember { mutableStateOf<NoteFile?>(null) }

    suspend fun refreshNotes() {
        notes = withContext(Dispatchers.IO) { repository.listNotes() }
    }

    LaunchedEffect(Unit) {
        refreshNotes()
    }

    if (selectedNotePath != null) {
        BackHandler {
            selectedNotePath = null
            scope.launch { refreshNotes() }
        }

        EditorScreen(
            filePath = selectedNotePath!!,
            onBack = {
                selectedNotePath = null
                scope.launch { refreshNotes() }
            }
        )
        return
    }

    BackHandler(onBack = onBackToExplorer)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor Teks") },
                navigationIcon = {
                    IconButton(onClick = onBackToExplorer) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                newNoteName = ""
                showCreateDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Buat catatan")
            }
        }
    ) { innerPadding ->
        if (notes.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Belum ada catatan.",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Catatan .txt akan disimpan di D-Manager/note",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                items(notes, key = { it.path }) { note ->
                    NoteRow(
                        note = note,
                        onClick = { selectedNotePath = note.path },
                        onLongClick = { noteToDelete = note }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Catatan Baru") },
            text = {
                TextField(
                    value = newNoteName,
                    onValueChange = { newNoteName = it },
                    singleLine = true,
                    label = { Text("Nama catatan") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    scope.launch {
                        val created = withContext(Dispatchers.IO) {
                            repository.createNote(newNoteName)
                        }
                        refreshNotes()
                        selectedNotePath = created.absolutePath
                    }
                }) {
                    Text("BUAT")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("BATAL")
                }
            }
        )
    }

    noteToDelete?.let { note ->
        ConfirmDeleteDialog(
            fileName = note.name,
            onDismiss = { noteToDelete = null },
            onConfirm = {
                noteToDelete = null
                scope.launch {
                    withContext(Dispatchers.IO) { repository.deleteNote(note.path) }
                    refreshNotes()
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteRow(
    note: NoteFile,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.size(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = note.name,
                fontSize = 14.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row {
                Text(
                    text = note.lastModified.toReadableDate(),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = note.size.toReadableSize(),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        }
    }
}
