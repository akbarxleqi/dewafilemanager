package com.dewa.filemanager.ui.signaturekey

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dewa.filemanager.data.model.SignatureKeyCreateRequest
import com.dewa.filemanager.data.model.SignatureKeyEntry
import com.dewa.filemanager.data.model.SignatureStoreType
import com.dewa.filemanager.ui.theme.MTOnSurface
import com.dewa.filemanager.ui.theme.MTPrimary
import com.dewa.filemanager.utils.toReadableDate
import com.dewa.filemanager.utils.toReadableSize
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureKeyManagerScreen(
    onBack: () -> Unit
) {
    val viewModel: SignatureKeyManagerViewModel = viewModel()
    val entries by viewModel.entries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var keyToDelete by remember { mutableStateOf<SignatureKeyEntry?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Kunci Tanda Tangan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                entries.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum ada file kunci.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Buat file JKS atau BKS baru dari tombol +.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(entries, key = { it.path }) { item ->
                            SignatureKeyRow(
                                item = item,
                                onLongClick = { keyToDelete = item }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateSignatureKeyDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { request ->
                scope.launch {
                    val result = viewModel.createKey(request)
                    if (result.isSuccess) {
                        showCreateDialog = false
                        snackbarHostState.showSnackbar("Kunci berhasil dibuat")
                    } else {
                        snackbarHostState.showSnackbar(
                            result.exceptionOrNull()?.message ?: "Gagal membuat kunci"
                        )
                    }
                }
            }
        )
    }

    keyToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { keyToDelete = null },
            title = { Text("Hapus keystore") },
            text = {
                Text("Yakin ingin menghapus file \"${item.name}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val deleted = viewModel.deleteKey(item.path)
                            keyToDelete = null
                            snackbarHostState.showSnackbar(
                                if (deleted) "Keystore dihapus" else "Gagal menghapus keystore"
                            )
                        }
                    }
                ) {
                    Text("Hapus", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { keyToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun SignatureKeyRow(
    item: SignatureKeyEntry,
    onLongClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (item.type == SignatureStoreType.JKS) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    } else {
                        Color(0xFF388E3C).copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = item.type.name,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = if (item.type == SignatureStoreType.JKS) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color(0xFF2E7D32)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${item.size.toReadableSize()}  •  ${item.lastModified.toReadableDate()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tekan lama untuk hapus",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun CreateSignatureKeyDialog(
    onDismiss: () -> Unit,
    onCreate: (SignatureKeyCreateRequest) -> Unit
) {
    var selectedType by remember { mutableStateOf(SignatureStoreType.JKS) }
    var storePassword by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var aliasPassword by remember { mutableStateOf("") }
    var advanced by remember { mutableStateOf(true) }
    var validityYears by remember { mutableStateOf("25") }
    var commonName by remember { mutableStateOf("") }
    var organizationalUnit by remember { mutableStateOf("") }
    var organization by remember { mutableStateOf("") }
    var locality by remember { mutableStateOf("") }
    var stateOrProvince by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("ID") }
    var passwordVisibilityMask by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            color = Color.Black.copy(alpha = 0.22f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.92f),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF3A3A3A),
                    tonalElevation = 0.dp
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 22.dp, vertical = 20.dp)
                        ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Buat kunci",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MTOnSurface,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold
                        )
                        StoreTypeOption(
                            label = "JKS",
                            selected = selectedType == SignatureStoreType.JKS,
                            onClick = { selectedType = SignatureStoreType.JKS }
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        StoreTypeOption(
                            label = "BKS",
                            selected = selectedType == SignatureStoreType.BKS,
                            onClick = { selectedType = SignatureStoreType.BKS }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (selectedType == SignatureStoreType.JKS) {
                            "Mode JKS dibuat sebagai keystore kompatibel signing APK Android di device."
                        } else {
                            "BKS dibuat untuk kompatibilitas tertentu. Untuk signing APK Android, pilih mode JKS."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MTOnSurface.copy(alpha = 0.72f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PasswordField(
                        value = storePassword,
                        onValueChange = { storePassword = it },
                        label = "Kata sandi",
                        visible = passwordVisibilityMask and 1 != 0,
                        onToggleVisible = { passwordVisibilityMask = passwordVisibilityMask xor 1 }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = alias,
                        onValueChange = { alias = it },
                        label = { Text("Alias") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = signatureFieldColors()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PasswordField(
                        value = aliasPassword,
                        onValueChange = { aliasPassword = it },
                        label = "Kata Sandi Alias",
                        visible = passwordVisibilityMask and 2 != 0,
                        onToggleVisible = { passwordVisibilityMask = passwordVisibilityMask xor 2 }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Opsi lanjutan",
                                style = MaterialTheme.typography.titleMedium,
                                color = MTOnSurface
                            )
                            Text(
                                text = "Atur identitas sertifikat dan masa berlaku",
                                style = MaterialTheme.typography.bodySmall,
                                color = MTOnSurface.copy(alpha = 0.65f)
                            )
                        }
                        Switch(
                            checked = advanced,
                            onCheckedChange = { advanced = it }
                        )
                    }

                    if (advanced) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = validityYears,
                            onValueChange = { validityYears = it.filter(Char::isDigit) },
                            label = { Text("Validitas (tahun)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = signatureFieldColors()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = commonName,
                            onValueChange = { commonName = it },
                            label = { Text("Nama pertama dan terakhir") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = signatureFieldColors()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = organizationalUnit,
                            onValueChange = { organizationalUnit = it },
                            label = { Text("Unit organisasi") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = signatureFieldColors()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = organization,
                            onValueChange = { organization = it },
                            label = { Text("Organisasi") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = signatureFieldColors()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = locality,
                            onValueChange = { locality = it },
                            label = { Text("Kota atau Lokalitas") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = signatureFieldColors()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = stateOrProvince,
                            onValueChange = { stateOrProvince = it },
                            label = { Text("Negara atau Provinsi") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = signatureFieldColors()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = countryCode,
                            onValueChange = { countryCode = it.uppercase().take(2) },
                            label = { Text("Kode Negara (XX)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = signatureFieldColors()
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("MEMBATALKAN", color = MTPrimary)
                            }
                            TextButton(
                                onClick = {
                                    val finalValidity = validityYears.toIntOrNull() ?: 25
                                    onCreate(
                                        SignatureKeyCreateRequest(
                                            storeType = selectedType,
                                            storePassword = storePassword.trim(),
                                            alias = alias.trim(),
                                            aliasPassword = aliasPassword,
                                            validityYears = finalValidity,
                                            commonName = commonName.trim(),
                                            organizationalUnit = organizationalUnit.trim(),
                                            organization = organization.trim(),
                                            locality = locality.trim(),
                                            stateOrProvince = stateOrProvince.trim(),
                                            countryCode = countryCode.trim().ifBlank { "ID" }
                                        )
                                    )
                                },
                                enabled = storePassword.isNotBlank() && alias.isNotBlank()
                            ) {
                                Text("OKE", color = MTPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreTypeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = label,
            color = MTOnSurface.copy(alpha = 0.95f)
        )
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisible: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        colors = signatureFieldColors(),
        trailingIcon = {
            TextButton(onClick = onToggleVisible) {
                Text(if (visible) "Hide" else "Show", color = MTPrimary)
            }
        }
    )
}

@Composable
private fun signatureFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedTextColor = MTOnSurface,
    unfocusedTextColor = MTOnSurface,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    cursorColor = MTPrimary,
    focusedBorderColor = MTPrimary,
    focusedLabelColor = MTPrimary,
    unfocusedLabelColor = MTOnSurface.copy(alpha = 0.75f),
    unfocusedBorderColor = MTOnSurface.copy(alpha = 0.42f)
)
