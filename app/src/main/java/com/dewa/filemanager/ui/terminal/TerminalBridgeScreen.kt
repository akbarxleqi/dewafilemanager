package com.dewa.filemanager.ui.terminal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.InterruptedIOException
import java.io.OutputStreamWriter

private enum class TerminalLineKind { SYSTEM, INPUT, OUTPUT }

private data class TerminalLine(
    val text: String,
    val kind: TerminalLineKind
)

private enum class TerminalThemePreset { DEFAULT, SOLARIZED, AMOLED }

private data class TerminalPalette(
    val appBackground: Color,
    val panelBackground: Color,
    val inputBackground: Color,
    val textPrimary: Color,
    val textSystem: Color,
    val textInput: Color,
    val accent: Color,
    val border: Color
)

private class TerminalSessionState(
    val id: Int,
    title: String,
    startDir: String
) {
    var title by mutableStateOf(title)
    var currentDir by mutableStateOf(startDir)
    var commandInput by mutableStateOf("")
    val lines = mutableStateListOf<TerminalLine>()
    val commandHistory = mutableStateListOf<String>()
    var historyIndex by mutableIntStateOf(-1)
    var process: Process? by mutableStateOf(null)
    var writer: OutputStreamWriter? by mutableStateOf(null)
    var readerJob: Job? by mutableStateOf(null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalBridgeScreen(
    initialPath: String,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val sessions = remember { mutableStateListOf<TerminalSessionState>() }
    var nextSessionId by remember { mutableIntStateOf(1) }
    var activeSessionId by remember { mutableIntStateOf(-1) }
    var currentTheme by remember { mutableStateOf(TerminalThemePreset.DEFAULT) }
    var showThemeMenu by remember { mutableStateOf(false) }

    fun paletteFor(theme: TerminalThemePreset): TerminalPalette = when (theme) {
        TerminalThemePreset.DEFAULT -> TerminalPalette(
            appBackground = Color(0xFF0B0D12),
            panelBackground = Color(0xFF121722),
            inputBackground = Color(0xFF1A2130),
            textPrimary = Color(0xFFE7EAF0),
            textSystem = Color(0xFF8AB4F8),
            textInput = Color(0xFF7ED38B),
            accent = Color(0xFF7ED38B),
            border = Color(0xFF3B465E)
        )
        TerminalThemePreset.SOLARIZED -> TerminalPalette(
            appBackground = Color(0xFF002B36),
            panelBackground = Color(0xFF073642),
            inputBackground = Color(0xFF0A4755),
            textPrimary = Color(0xFFEEE8D5),
            textSystem = Color(0xFF93A1A1),
            textInput = Color(0xFFB58900),
            accent = Color(0xFF2AA198),
            border = Color(0xFF586E75)
        )
        TerminalThemePreset.AMOLED -> TerminalPalette(
            appBackground = Color(0xFF000000),
            panelBackground = Color(0xFF050505),
            inputBackground = Color(0xFF0B0B0B),
            textPrimary = Color(0xFFEFEFEF),
            textSystem = Color(0xFF9E9E9E),
            textInput = Color(0xFF5CFF8A),
            accent = Color(0xFF5CFF8A),
            border = Color(0xFF2B2B2B)
        )
    }

    val palette = paletteFor(currentTheme)

    fun appendLine(session: TerminalSessionState, text: String, kind: TerminalLineKind = TerminalLineKind.OUTPUT) {
        session.lines.add(TerminalLine(text = text, kind = kind))
    }

    fun closeSession(session: TerminalSessionState) {
        session.readerJob?.cancel()
        session.readerJob = null
        session.writer?.close()
        session.writer = null
        session.process?.destroy()
        session.process = null
    }

    fun createSession(startPath: String): TerminalSessionState {
        val path = normalizeStartPath(startPath)
        val session = TerminalSessionState(
            id = nextSessionId,
            title = "S$nextSessionId",
            startDir = path
        )
        nextSessionId += 1
        sessions.add(session)
        activeSessionId = session.id
        return session
    }

    fun activeSession(): TerminalSessionState? = sessions.firstOrNull { it.id == activeSessionId }

    fun startSession(session: TerminalSessionState) {
        closeSession(session)
        scope.launch(Dispatchers.IO) {
            try {
                val workingDir = File(session.currentDir).takeIf { it.exists() && it.isDirectory } ?: File("/storage/emulated/0")
                val newProcess = ProcessBuilder("/system/bin/sh")
                    .directory(workingDir)
                    .redirectErrorStream(true)
                    .start()
                val outputWriter = OutputStreamWriter(newProcess.outputStream)

                withContext(Dispatchers.Main) {
                    session.process = newProcess
                    session.writer = outputWriter
                    session.lines.clear()
                    appendLine(session, "D-Manager Internal Terminal ${session.title}", TerminalLineKind.SYSTEM)
                    appendLine(session, "$ ${workingDir.absolutePath}", TerminalLineKind.SYSTEM)
                    appendLine(session, "", TerminalLineKind.SYSTEM)
                }

                val job = scope.launch(Dispatchers.IO) {
                    try {
                        val reader = BufferedReader(InputStreamReader(newProcess.inputStream))
                        var line: String? = null
                        while (isActive && reader.readLine().also { line = it } != null) {
                            val output = line ?: continue
                            launch(Dispatchers.Main) {
                                appendLine(session, output, TerminalLineKind.OUTPUT)
                            }
                        }
                    } catch (_: InterruptedIOException) {
                        // Expected when process stream is closed during tab close/back.
                    } catch (_: java.io.IOException) {
                        // Stream closed while switching/closing sessions.
                    } catch (e: Exception) {
                        launch(Dispatchers.Main) {
                            appendLine(
                                session,
                                "Reader error: ${e.message ?: "unknown error"}",
                                TerminalLineKind.SYSTEM
                            )
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    session.readerJob = job
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appendLine(session, "Gagal memulai terminal: ${e.message ?: "unknown error"}", TerminalLineKind.SYSTEM)
                }
            }
        }
    }

    fun resolveCdPath(currentDir: String, rawArg: String): String {
        if (rawArg.isBlank()) return "/storage/emulated/0"
        val target = if (rawArg.startsWith('/')) File(rawArg) else File(currentDir, rawArg)
        return runCatching { target.canonicalPath }.getOrDefault(target.absolutePath)
    }

    fun sendRaw(session: TerminalSessionState, value: String) {
        try {
            session.writer?.apply {
                write(value)
                flush()
            } ?: appendLine(session, "Session tidak aktif", TerminalLineKind.SYSTEM)
        } catch (e: Exception) {
            appendLine(session, "Gagal kirim command: ${e.message ?: "unknown error"}", TerminalLineKind.SYSTEM)
        }
    }

    fun submitCommand(session: TerminalSessionState) {
        val cmd = session.commandInput.trimEnd()
        if (cmd.isBlank()) return

        appendLine(session, "$ $cmd", TerminalLineKind.INPUT)
        session.commandHistory.add(cmd)
        session.historyIndex = -1

        when {
            cmd == "clear" -> session.lines.clear()
            cmd.startsWith("cd") -> {
                val arg = cmd.removePrefix("cd").trim()
                val nextDir = resolveCdPath(session.currentDir, arg)
                val nextFile = File(nextDir)
                if (nextFile.exists() && nextFile.isDirectory) {
                    session.currentDir = nextDir
                    startSession(session)
                } else {
                    appendLine(session, "cd: no such directory: $arg", TerminalLineKind.SYSTEM)
                }
            }
            cmd.startsWith("pkg") || cmd.startsWith("apt") || cmd.startsWith("apt-get") -> {
                appendLine(
                    session,
                    "Perintah package manager Termux tidak tersedia di mode internal. Gunakan command shell bawaan Android (ls, cd, cat, sh, toybox, dll).",
                    TerminalLineKind.SYSTEM
                )
            }
            else -> sendRaw(session, "$cmd\n")
        }

        session.commandInput = ""
    }

    fun pickHistory(session: TerminalSessionState, up: Boolean) {
        if (session.commandHistory.isEmpty()) return

        session.historyIndex = if (up) {
            if (session.historyIndex < 0) session.commandHistory.lastIndex else (session.historyIndex - 1).coerceAtLeast(0)
        } else {
            if (session.historyIndex < 0) return else (session.historyIndex + 1).coerceAtMost(session.commandHistory.lastIndex)
        }

        session.commandInput = session.commandHistory[session.historyIndex]
    }

    fun sendExtraKey(session: TerminalSessionState, key: String) {
        when (key) {
            "ESC" -> sendRaw(session, "\u001b")
            "TAB" -> session.commandInput += "\t"
            "CTRL" -> sendRaw(session, "\u0003")
            "ALT" -> sendRaw(session, "\u001b")
            "/", "-", "|" -> session.commandInput += key
        }
    }

    fun closeActiveTab() {
        if (sessions.size <= 1) {
            onBack()
            return
        }
        val session = activeSession() ?: return
        closeSession(session)
        sessions.remove(session)
        activeSessionId = sessions.lastOrNull()?.id ?: -1
    }

    LaunchedEffect(Unit) {
        if (sessions.isEmpty()) {
            val first = createSession(initialPath)
            startSession(first)
        }
    }

    val current = activeSession()

    LaunchedEffect(current?.lines?.size, current?.id) {
        if (current != null && current.lines.isNotEmpty()) {
            listState.animateScrollToItem(current.lines.lastIndex)
        }
    }

    DisposableEffect(Unit) {
        onDispose { sessions.forEach(::closeSession) }
    }

    Scaffold(
        containerColor = palette.appBackground,
        topBar = {
            TopAppBar(
                title = { Text("Terminal", color = palette.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showThemeMenu = true }) {
                        Icon(Icons.Default.Palette, contentDescription = "Theme", tint = palette.textPrimary)
                    }
                    DropdownMenu(expanded = showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
                        DropdownMenuItem(text = { Text("Default") }, onClick = {
                            currentTheme = TerminalThemePreset.DEFAULT
                            showThemeMenu = false
                        })
                        DropdownMenuItem(text = { Text("Solarized") }, onClick = {
                            currentTheme = TerminalThemePreset.SOLARIZED
                            showThemeMenu = false
                        })
                        DropdownMenuItem(text = { Text("Amoled") }, onClick = {
                            currentTheme = TerminalThemePreset.AMOLED
                            showThemeMenu = false
                        })
                    }
                    IconButton(onClick = { current?.let(::startSession) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart session", tint = palette.textPrimary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(palette.appBackground)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sessions.forEach { session ->
                    FilterChip(
                        selected = session.id == activeSessionId,
                        onClick = { activeSessionId = session.id },
                        label = { Text(session.title) }
                    )
                }
                IconButton(onClick = {
                    val newSession = createSession(current?.currentDir ?: initialPath)
                    startSession(newSession)
                }) {
                    Icon(Icons.Default.Add, contentDescription = "New session", tint = palette.textPrimary)
                }
                IconButton(onClick = { closeActiveTab() }) {
                    Icon(Icons.Default.Close, contentDescription = "Close session", tint = palette.textPrimary)
                }
            }

            if (current == null) return@Column

            Text(
                text = current.currentDir,
                color = palette.accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                itemsIndexed(current.lines) { _, line ->
                    val color = when (line.kind) {
                        TerminalLineKind.SYSTEM -> palette.textSystem
                        TerminalLineKind.INPUT -> palette.textInput
                        TerminalLineKind.OUTPUT -> palette.textPrimary
                    }
                    Text(
                        text = line.text,
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            Surface(
                color = palette.panelBackground,
                shadowElevation = 8.dp,
                tonalElevation = 2.dp,
                shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ESC", "TAB", "CTRL", "ALT", "/", "-", "|").forEach { key ->
                            TextButton(onClick = { sendExtraKey(current, key) }) {
                                Text(key)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { sendRaw(current, "\u0003") }) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ctrl+C")
                        }
                        TextButton(onClick = { pickHistory(current, up = true) }) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(17.dp))
                        }
                        TextButton(onClick = { pickHistory(current, up = false) }) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(17.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$",
                            color = palette.accent,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        TextField(
                            value = current.commandInput,
                            onValueChange = { current.commandInput = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { submitCommand(current) }),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = palette.inputBackground,
                                unfocusedContainerColor = palette.inputBackground,
                                focusedIndicatorColor = palette.accent,
                                unfocusedIndicatorColor = palette.border,
                                focusedTextColor = palette.textPrimary,
                                unfocusedTextColor = palette.textPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { submitCommand(current) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Run")
                        }
                    }
                }
            }
        }
    }
}

private fun normalizeStartPath(path: String): String {
    val target = File(path)
    return if (target.exists() && target.isDirectory) target.absolutePath else "/storage/emulated/0"
}
