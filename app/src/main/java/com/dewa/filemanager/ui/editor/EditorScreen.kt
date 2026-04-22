package com.dewa.filemanager.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.launch
import com.dewa.filemanager.utils.SyntaxHighlighter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    filePath: String,
    onBack: () -> Unit
) {
    val viewModel: EditorViewModel = viewModel()
    val content by viewModel.content.collectAsState()
    val fileName by viewModel.fileName.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(filePath) {
        viewModel.loadFile(filePath)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(fileName, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            val success = viewModel.saveFile()
                            snackbarHostState.showSnackbar(
                                if (success) "File disimpan" else "Gagal menyimpan file"
                            )
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            var scale by remember { mutableFloatStateOf(1f) }

            BasicTextField(
                value = content,
                onValueChange = { viewModel.updateContent(it) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 5f)
                        }
                    },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = (14 * scale).sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                visualTransformation = { text ->
                    val highlighted = SyntaxHighlighter.highlight(text.text, fileName.substringAfterLast(".", ""))
                    TransformedText(highlighted, androidx.compose.ui.text.input.OffsetMapping.Identity)
                }
            )
        }
    }
}
