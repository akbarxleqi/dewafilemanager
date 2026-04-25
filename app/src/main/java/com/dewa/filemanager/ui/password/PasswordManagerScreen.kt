package com.dewa.filemanager.ui.password

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dewa.filemanager.data.model.PasswordEntry
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordManagerScreen(
    onBack: () -> Unit
) {
    val vm: PasswordManagerViewModel = viewModel()
    val entries by vm.entries.collectAsStateWithLifecycleCompat()
    val isLoading by vm.isLoading.collectAsStateWithLifecycleCompat()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAdd by remember { mutableStateOf(false) }
    var formName by remember { mutableStateOf("") }
    var formContact by remember { mutableStateOf("") }
    var formPassword by remember { mutableStateOf("") }
    var showFormPassword by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var deletingItem by remember { mutableStateOf<PasswordEntry?>(null) }

    val visibleRows = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Kata Sandi Umum") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                formName = ""
                formContact = ""
                formPassword = ""
                showFormPassword = false
                editingId = null
                showAdd = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (entries.isEmpty()) {
                Text(
                    text = "Belum ada data password.\nTekan tombol + untuk menambahkan.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(entries, key = { it.id }) { item ->
                        val visible = visibleRows[item.id] == true
                        PasswordCard(
                            item = item,
                            visible = visible,
                            onToggle = { visibleRows[item.id] = !visible },
                            onEdit = {
                                editingId = item.id
                                formName = item.name
                                formContact = item.contact
                                formPassword = item.password
                                showFormPassword = false
                                showAdd = true
                            },
                            onDelete = {
                                deletingItem = item
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = {
                showAdd = false
                editingId = null
            },
            title = { Text(if (editingId == null) "Tambah Data Password" else "Edit Data Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = formName,
                        onValueChange = { formName = it },
                        label = { Text("Nama") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = formContact,
                        onValueChange = { formContact = it },
                        label = { Text("Email / No") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = formPassword,
                        onValueChange = { formPassword = it },
                        label = { Text("Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showFormPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showFormPassword = !showFormPassword }) {
                                Icon(
                                    imageVector = if (showFormPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val isEdit = editingId != null
                            val finalOk = if (isEdit) {
                                vm.updateEntry(
                                    id = editingId!!,
                                    name = formName.trim(),
                                    contact = formContact.trim(),
                                    password = formPassword
                                )
                            } else {
                                vm.addEntry(
                                    name = formName.trim(),
                                    contact = formContact.trim(),
                                    password = formPassword
                                )
                            }
                            if (finalOk) {
                                showAdd = false
                                snackbar.showSnackbar(
                                    if (isEdit) "Data password diperbarui" else "Data password disimpan"
                                )
                                editingId = null
                            } else {
                                snackbar.showSnackbar("Gagal menyimpan data")
                            }
                        }
                    },
                    enabled = formName.isNotBlank() && formPassword.isNotBlank()
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAdd = false
                    editingId = null
                }) {
                    Text("Batal")
                }
            }
        )
    }

    deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            title = { Text("Hapus Data Password") },
            text = { Text("Yakin ingin menghapus data \"${item.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val ok = vm.deleteEntry(item.id)
                        deletingItem = null
                        snackbar.showSnackbar(
                            if (ok) "Data password dihapus" else "Gagal menghapus data"
                        )
                    }
                }) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { deletingItem = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun PasswordCard(
    item: PasswordEntry,
    visible: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.contact,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (visible) item.password else "••••••••••",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace
                )
                IconButton(onClick = onToggle, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit"
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete"
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> StateFlow<T>.collectAsStateWithLifecycleCompat(): State<T> = collectAsState()
