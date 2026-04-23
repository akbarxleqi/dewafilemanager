package com.dewa.filemanager.ui.explorer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dewa.filemanager.data.repository.FileManagerRepository
import com.dewa.filemanager.utils.toReadableSize
import kotlinx.coroutines.launch

@Composable
fun MTDrawer(
    drawerState: DrawerState,
    storageStats: FileManagerRepository.StorageStats?,
    isDarkMode: Boolean = true,
    onThemeToggle: () -> Unit = {},
    onStorageClick: () -> Unit = {},
    onRecycleBinClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var localExpanded by remember { mutableStateOf(true) }
    var toolsExpanded by remember { mutableStateOf(true) }

    val surface = if (isDarkMode) Color(0xFF323232) else Color(0xFFF2F3F5)
    val header = if (isDarkMode) Color(0xFF121212) else Color(0xFFE8EBEF)
    val iconCircle = if (isDarkMode) Color(0xFF121212) else Color(0xFFD9DEE5)
    val textPrimary = if (isDarkMode) Color(0xFFE6E6E6) else Color(0xFF191919)
    val textSecondary = if (isDarkMode) Color(0xFF9F9F9F) else Color(0xFF666A70)
    val iconOnCircle = if (isDarkMode) Color.White else Color(0xFF202327)
    val blue = Color(0xFF2F8FFF)
    val progressTrack = if (isDarkMode) Color(0xFF8E8E8E) else Color(0xFFB8BEC6)

    val toolItems = listOf(
        DrawerToolItem("Tempat sampah", Icons.Default.Delete),
        DrawerToolItem("Manajer Plugin", Icons.Default.Extension),
        DrawerToolItem("Manajemen Jarak Jauh", Icons.Default.Description),
        DrawerToolItem("Pemilih Warna Layar", Icons.Default.ColorLens),
        DrawerToolItem("Kunci Tanda Tangan", Icons.Default.Key),
        DrawerToolItem("Kata Sandi Umum", Icons.Default.Lock),
        DrawerToolItem("Ekstrak APK", Icons.Default.Layers),
        DrawerToolItem("Editor Teks", Icons.Default.EditNote),
        DrawerToolItem("Simulator Terminal", Icons.Default.Terminal),
        DrawerToolItem("Catatan Aktivitas", Icons.Default.PhoneAndroid)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (drawerState.isOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { scope.launch { drawerState.close() } }
            )

            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(316.dp),
                color = surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .background(header)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = null,
                                tint = textPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "D-Manager",
                                color = textPrimary,
                                style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium)
                            )
                        }
                        Row {
                            IconButton(onClick = onThemeToggle, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Theme",
                                    tint = textPrimary,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                            IconButton(onClick = {
                                Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show()
                            }, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = null,
                                    tint = textPrimary,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                    }

                    DrawerSectionHeader(
                        title = "Lokal",
                        expanded = localExpanded,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onToggle = { localExpanded = !localExpanded }
                    )

                    if (localExpanded) {
                        storageStats?.let { stats ->
                            DrawerStorageItem(
                                title = "Direktori Akar",
                                icon = Icons.Default.PhoneAndroid,
                                usedBytes = stats.totalBytes,
                                totalBytes = stats.totalBytes,
                                availableBytes = 0L,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                iconOnCircle = iconOnCircle,
                                primary = blue,
                                progressTrack = progressTrack,
                                iconCircle = iconCircle,
                                onClick = {
                                    onStorageClick()
                                    scope.launch { drawerState.close() }
                                }
                            )

                            DrawerStorageItem(
                                title = "Direktori Penyimpanan",
                                icon = Icons.Default.SdStorage,
                                usedBytes = stats.usedBytes,
                                totalBytes = stats.totalBytes,
                                availableBytes = stats.availableBytes,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                iconOnCircle = iconOnCircle,
                                primary = blue,
                                progressTrack = progressTrack,
                                iconCircle = iconCircle,
                                onClick = {
                                    onStorageClick()
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                    }

                    DrawerSectionHeader(
                        title = "Peralatan",
                        expanded = toolsExpanded,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onToggle = { toolsExpanded = !toolsExpanded }
                    )

                    if (toolsExpanded) {
                        toolItems.forEach { item ->
                            DrawerToolRow(
                                label = item.label,
                                icon = item.icon,
                                textPrimary = textPrimary,
                                iconOnCircle = iconOnCircle,
                                iconCircle = iconCircle,
                                onClick = {
                                    if (item.label == "Tempat sampah") {
                                        onRecycleBinClick()
                                        scope.launch { drawerState.close() }
                                    } else {
                                        Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

private data class DrawerToolItem(
    val label: String,
    val icon: ImageVector
)

@Composable
private fun DrawerSectionHeader(
    title: String,
    expanded: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = textSecondary,
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)
        )
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = textPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun DrawerStorageItem(
    title: String,
    icon: ImageVector,
    usedBytes: Long,
    totalBytes: Long,
    availableBytes: Long,
    textPrimary: Color,
    textSecondary: Color,
    iconOnCircle: Color,
    primary: Color,
    progressTrack: Color,
    iconCircle: Color,
    onClick: () -> Unit
) {
    val progress = if (totalBytes > 0) usedBytes.toFloat() / totalBytes.toFloat() else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = iconCircle,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconOnCircle, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = textPrimary,
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = primary,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = primary,
                trackColor = progressTrack
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${usedBytes.toReadableSize()} digunakan, ${availableBytes.toReadableSize()} tersedia",
                color = textSecondary,
                style = TextStyle(fontSize = 10.sp)
            )
        }
    }
}

@Composable
private fun DrawerToolRow(
    label: String,
    icon: ImageVector,
    textPrimary: Color,
    iconOnCircle: Color,
    iconCircle: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = iconCircle,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconOnCircle, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = textPrimary,
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)
        )
    }
}
