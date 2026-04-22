package com.dewa.filemanager.data.model

import java.io.File

data class FileEntity(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val extension: String = "",
    val isHidden: Boolean = false
) {
    companion object {
        fun fromFile(file: File): FileEntity {
            return FileEntity(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                size = if (file.isDirectory) 0 else file.length(),
                lastModified = file.lastModified(),
                extension = file.extension,
                isHidden = file.name.startsWith(".")
            )
        }
    }
}
