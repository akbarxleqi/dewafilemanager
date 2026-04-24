package com.dewa.filemanager.ui.apkextractor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dewa.filemanager.ui.theme.MTBackground
import com.dewa.filemanager.ui.theme.MTOnSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkExtractorScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var userApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var systemApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var selectedApp by remember { mutableStateOf<InstalledAppItem?>(null) }
    var isExtracting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        val result = withContext(Dispatchers.IO) {
            ApkExtractorRepository.loadInstalledApps(context)
        }
        userApps = result.userApps
        systemApps = result.systemApps
        isLoading = false
    }

    BackHandler(onBack = onBack)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Ekstrak APK") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MTBackground,
                    titleContentColor = MTOnSurface,
                    actionIconContentColor = MTOnSurface,
                    navigationIconContentColor = MTOnSurface
                )
            )
        },
        containerColor = MTBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Black,
                contentColor = MTOnSurface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("APLIKASI PENGGUNA", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("APLIKASI SISTEM", fontWeight = FontWeight.SemiBold) }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val apps = if (selectedTab == 0) userApps else systemApps
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        AppCard(
                            app = app,
                            onClick = { selectedApp = app }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }

    selectedApp?.let { app ->
        ApkExtractDetailDialog(
            app = app,
            isExtracting = isExtracting,
            onDismiss = {
                if (!isExtracting) selectedApp = null
            },
            onExtract = {
                scope.launch {
                    isExtracting = true
                    val result = withContext(Dispatchers.IO) {
                        runCatching { ApkExtractorRepository.extractApkFiles(app) }
                    }
                    isExtracting = false
                    result.onSuccess { extract ->
                        selectedApp = null
                        snackbarHostState.showSnackbar(
                            "Berhasil ekstrak ${extract.copiedFiles.size} APK ke ${extract.outputDir}"
                        )
                    }.onFailure { err ->
                        snackbarHostState.showSnackbar(err.message ?: "Gagal ekstrak APK")
                    }
                }
            }
        )
    }
}

@Composable
private fun AppCard(
    app: InstalledAppItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color(0xFF454545),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (app.icon != null) {
                Image(
                    bitmap = app.icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF2D2D2D), RoundedCornerShape(10.dp))
                )
            }

            Spacer(modifier = Modifier.size(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    color = Color(0xFFE2E2E2),
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val splitLabel = if (app.splitApkSizeBytes > 0L) {
                    "  SPLIT+${ApkExtractorRepository.formatMtSize(app.splitApkSizeBytes)}"
                } else {
                    ""
                }

                Text(
                    text = "${app.versionName}   ${ApkExtractorRepository.formatMtSize(app.baseApkSizeBytes)}$splitLabel",
                    color = Color(0xFFBDBDBD),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    color = Color(0xFFB5B5B5),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ApkExtractDetailDialog(
    app: InstalledAppItem,
    isExtracting: Boolean,
    onDismiss: () -> Unit,
    onExtract: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    fun copyValue(label: String, value: String) {
        clipboard.setText(AnnotatedString(value))
        Toast.makeText(context, "$label disalin", Toast.LENGTH_SHORT).show()
    }

    val rows = listOf(
        "Nama paket" to app.packageName,
        "Kode versi" to app.versionCode.toString(),
        "Ukuran file" to ApkExtractorRepository.formatMtSize(app.baseApkSizeBytes),
        "Tanda tangan" to app.signatureScheme,
        "Perlindungan" to "Tidak terdeteksi",
        "SDK Target" to ApkExtractorRepository.formatSdkLabel(app.targetSdk),
        "SDK Minimum" to ApkExtractorRepository.formatSdkLabel(app.minSdk),
        "Direktori data 1" to app.dataDir,
        "Direktori data 2" to app.deviceProtectedDataDir,
        "jalur APK" to app.baseApkPath,
        "Dipasang" to ApkExtractorRepository.formatDateTime(app.firstInstallTime),
        "Diperbarui" to ApkExtractorRepository.formatDateTime(app.lastUpdateTime),
        "UID" to app.uid.toString()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF4A4A4A),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (app.icon != null) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                }
                Column {
                    Text(
                        text = app.appName,
                        color = Color(0xFFE8E8E8),
                        fontSize = 28.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.versionName,
                        color = Color(0xFFCDCDCD),
                        fontSize = 16.sp
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                rows.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { copyValue(label, value) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = label,
                            color = Color(0xFFE0E0E0),
                            fontSize = 16.sp,
                            modifier = Modifier.weight(0.42f)
                        )
                        Text(
                            text = value,
                            color = Color(0xFFC8C8C8),
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(0.58f)
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isExtracting) {
                Text("BATAL")
            }
        },
        confirmButton = {
            TextButton(onClick = onExtract, enabled = !isExtracting) {
                Text(if (isExtracting) "MENYIAPKAN..." else "EKSTRAK APK")
            }
        }
    )
}
