package com.dewa.filemanager.data.repository

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ArchiveRepository {

    fun extractZip(zipFilePath: String, destPath: String): Boolean {
        return try {
            val destDir = File(destPath)
            if (!destDir.exists()) destDir.mkdirs()

            ZipInputStream(FileInputStream(zipFilePath)).use { zis ->
                var zipEntry = zis.nextEntry
                while (zipEntry != null) {
                    val newFile = File(destDir, zipEntry.name)
                    if (zipEntry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        // Create parent directories if missing
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zipEntry = zis.nextEntry
                }
                zis.closeEntry()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun compressToZip(srcPath: String, zipFilePath: String): Boolean {
        return try {
            val srcFile = File(srcPath)
            ZipOutputStream(FileOutputStream(zipFilePath)).use { zos ->
                if (srcFile.isDirectory) {
                    compressDirectory(srcFile, srcFile.name, zos)
                } else {
                    compressFile(srcFile, srcFile.name, zos)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun compressDirectory(dir: File, baseName: String, zos: ZipOutputStream) {
        val files = dir.listFiles() ?: return
        if (files.isEmpty()) {
            zos.putNextEntry(ZipEntry("$baseName/"))
            zos.closeEntry()
            return
        }
        for (file in files) {
            if (file.isDirectory) {
                compressDirectory(file, "$baseName/${file.name}", zos)
            } else {
                compressFile(file, "$baseName/${file.name}", zos)
            }
        }
    }

    private fun compressFile(file: File, fileName: String, zos: ZipOutputStream) {
        zos.putNextEntry(ZipEntry(fileName))
        FileInputStream(file).use { fis ->
            fis.copyTo(zos)
        }
        zos.closeEntry()
    }
}
