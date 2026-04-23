package com.dewa.filemanager.ui.explorer

import com.dewa.filemanager.data.model.FileEntity
import com.dewa.filemanager.data.repository.ArchiveRepository
import java.util.Locale

data class ArchiveLayer(
    val realArchivePath: String,
    val displayName: String,
    val password: String?,
    val currentPath: String,
    val entries: List<ArchiveRepository.ArchiveEntryInfo>
)

data class ArchivePaneState(
    val layers: List<ArchiveLayer>
) {
    val top: ArchiveLayer get() = layers.last()
}

data class ArchivePasswordRequest(
    val isLeftPane: Boolean,
    val realArchivePath: String,
    val displayName: String,
    val existingLayers: List<ArchiveLayer>,
    val reason: String
)

private val editableExtensions = setOf(
    "txt", "md", "log", "conf", "ini", "properties", "xml", "java", "kt", "kts", "js", "mjs",
    "ts", "html", "htm", "css", "scss", "sass", "less", "php", "py", "c", "cpp", "h", "hpp",
    "cs", "go", "rs", "rb", "swift", "dart", "smali", "json", "yml", "yaml", "sql", "sh", "bat"
)

private val editableDotFiles = setOf(
    ".env", ".env.example", ".gitignore", ".htaccess", ".editorconfig",
    ".npmrc", ".yarnrc", ".prettierrc", ".eslintrc"
)

fun String.isArchiveExt(): Boolean {
    val ext = this.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return ext == "zip" || ext == "rar"
}

fun isEditableTextFileName(fileName: String): Boolean {
    val lower = fileName.lowercase(Locale.ROOT)
    if (lower in editableDotFiles) return true
    if (lower.startsWith('.') && '.' !in lower.drop(1)) return true

    val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return ext in editableExtensions
}

fun archiveDisplayPath(state: ArchivePaneState): String {
    val roots = state.layers.joinToString("/") { it.displayName }
    val sub = state.top.currentPath.trim('/')
    return if (sub.isBlank()) "$roots/" else "$roots/$sub/"
}

fun buildArchiveNodes(state: ArchivePaneState): List<FileEntity> {
    val prefix = state.top.currentPath
    val map = linkedMapOf<String, FileEntity>()

    state.top.entries.forEach { entry ->
        val normalized = entry.fullPath.replace('\\', '/')
        if (!normalized.startsWith(prefix) || normalized == prefix) return@forEach

        val remaining = normalized.removePrefix(prefix)
        if (remaining.isBlank()) return@forEach

        val segment = remaining.substringBefore('/')
        val hasChildren = remaining.contains('/')
        val isDir = hasChildren || entry.isDirectory
        val full = if (isDir) "$prefix$segment/" else "$prefix$segment"

        if (!map.containsKey(full)) {
            map[full] = FileEntity(
                name = segment,
                path = full,
                isDirectory = isDir,
                size = if (isDir) 0 else entry.size,
                lastModified = entry.lastModified,
                extension = if (isDir) "" else segment.substringAfterLast('.', ""),
                isHidden = segment.startsWith('.')
            )
        }
    }

    return map.values.sortedWith(
        compareByDescending<FileEntity> { it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) }
    )
}

fun archiveParentPath(path: String): String {
    val clean = path.trim('/').split('/').filter { it.isNotBlank() }
    if (clean.isEmpty()) return ""
    return clean.dropLast(1).joinToString("/").let { if (it.isBlank()) "" else "$it/" }
}
