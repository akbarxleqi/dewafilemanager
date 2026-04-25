package com.dewa.filemanager.data.repository

import java.io.File

data class NoteFile(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long
)

class NotesRepository(
    private val fileManagerRepository: FileManagerRepository = FileManagerRepository()
) {

    fun getNotesDir(): File {
        fileManagerRepository.ensureAppDirectories()
        val notesDir = File(fileManagerRepository.getManagerRootPath(), "note")
        if (!notesDir.exists()) notesDir.mkdirs()
        return notesDir
    }

    fun listNotes(): List<NoteFile> {
        val dir = getNotesDir()
        return dir.listFiles()
            ?.filter { it.isFile && it.extension.equals("txt", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?.map {
                NoteFile(
                    name = it.name,
                    path = it.absolutePath,
                    size = it.length(),
                    lastModified = it.lastModified()
                )
            }
            ?: emptyList()
    }

    fun createNote(rawName: String): File {
        val dir = getNotesDir()
        val baseName = rawName.trim().ifBlank { "Catatan" }
        val normalized = if (baseName.endsWith(".txt", ignoreCase = true)) baseName else "$baseName.txt"

        val initial = File(dir, normalized)
        if (!initial.exists()) {
            initial.writeText("")
            return initial
        }

        val stem = normalized.removeSuffix(".txt")
        var index = 2
        while (true) {
            val candidate = File(dir, "$stem ($index).txt")
            if (!candidate.exists()) {
                candidate.writeText("")
                return candidate
            }
            index++
        }
    }

    fun deleteNote(path: String): Boolean {
        val file = File(path)
        val notesRoot = getNotesDir().absolutePath
        if (!file.absolutePath.startsWith(notesRoot)) return false
        return file.exists() && file.delete()
    }
}
