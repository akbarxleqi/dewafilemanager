package com.dewa.filemanager.data.repository

import android.os.Environment
import android.os.StatFs
import com.dewa.filemanager.data.model.FileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileManagerRepository {

    suspend fun listFiles(path: String): List<FileEntity> = withContext(Dispatchers.IO) {
        val directory = File(path)
        if (!directory.exists() || !directory.isDirectory) return@withContext emptyList<FileEntity>()

        directory.listFiles()?.map { FileEntity.fromFile(it) }?.sortedWith(
            compareByDescending<FileEntity> { it.isDirectory }.thenBy { it.name.lowercase() }
        ) ?: emptyList()
    }

    fun getStorageStats(path: String = Environment.getExternalStorageDirectory().absolutePath): StorageStats {
        val stat = StatFs(path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        return StorageStats(
            totalBytes = totalBlocks * blockSize,
            availableBytes = availableBlocks * blockSize,
            usedBytes = (totalBlocks - availableBlocks) * blockSize
        )
    }

    fun getRootPath(): String = Environment.getExternalStorageDirectory().absolutePath

    fun deleteItem(path: String): Boolean {
        val file = File(path)
        return if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
    }

    fun renameItem(oldPath: String, newName: String): Boolean {
        val oldFile = File(oldPath)
        val parent = oldFile.parentFile ?: return false
        val newFile = File(parent, newName)
        return oldFile.renameTo(newFile)
    }

    fun copyItem(srcPath: String, destPath: String): Boolean {
        return try {
            val src = File(srcPath)
            val dest = File(destPath, src.name)
            src.copyRecursively(dest, overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun moveItem(srcPath: String, destPath: String): Boolean {
        return try {
            val src = File(srcPath)
            val dest = File(destPath, src.name)
            if (src.renameTo(dest)) {
                true
            } else {
                if (src.copyRecursively(dest, overwrite = true)) {
                    src.deleteRecursively()
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    fun readFile(path: String): String {
        return try {
            File(path).readText()
        } catch (e: Exception) {
            ""
        }
    }

    fun saveFile(path: String, content: String): Boolean {
        return try {
            File(path).writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    data class StorageStats(
        val totalBytes: Long,
        val availableBytes: Long,
        val usedBytes: Long
    )
}
