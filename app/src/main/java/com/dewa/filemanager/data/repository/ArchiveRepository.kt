package com.dewa.filemanager.data.repository

import com.github.junrar.Archive
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.FileHeader
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object ArchiveRepository {

    data class ArchiveEntryInfo(
        val fullPath: String,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long
    )

    sealed class ArchiveBrowseResult {
        data class Success(val entries: List<ArchiveEntryInfo>) : ArchiveBrowseResult()
        data object PasswordRequired : ArchiveBrowseResult()
        data object InvalidPassword : ArchiveBrowseResult()
        data class Error(val message: String) : ArchiveBrowseResult()
    }

    fun browseArchive(archivePath: String, password: String? = null): ArchiveBrowseResult {
        return when (File(archivePath).extension.lowercase(Locale.ROOT)) {
            "zip" -> browseZip(archivePath, password)
            "rar" -> browseRar(archivePath, password)
            else -> ArchiveBrowseResult.Error("Format arsip belum didukung")
        }
    }

    fun extractArchiveEntry(
        archivePath: String,
        entryPath: String,
        destPath: String,
        password: String? = null
    ): String? {
        return when (File(archivePath).extension.lowercase(Locale.ROOT)) {
            "zip" -> extractZipEntry(archivePath, entryPath, destPath, password)
            "rar" -> extractRarEntry(archivePath, entryPath, destPath, password)
            else -> null
        }
    }

    fun replaceArchiveTextEntry(
        archivePath: String,
        entryPath: String,
        content: String,
        password: String? = null
    ): Boolean {
        return when (File(archivePath).extension.lowercase(Locale.ROOT)) {
            "zip" -> replaceZipTextEntry(archivePath, entryPath, content, password)
            else -> false
        }
    }

    fun browseZip(zipFilePath: String, password: String? = null): ArchiveBrowseResult {
        return try {
            val zipFile = buildZipFile(zipFilePath, password)
            if (zipFile.isEncrypted && password.isNullOrBlank()) return ArchiveBrowseResult.PasswordRequired

            val entries = zipFile.fileHeaders.map { header ->
                ArchiveEntryInfo(
                    fullPath = header.fileName,
                    isDirectory = header.isDirectory,
                    size = header.uncompressedSize,
                    lastModified = header.lastModifiedTimeEpoch
                )
            }
            ArchiveBrowseResult.Success(entries)
        } catch (e: ZipException) {
            if (isWrongPasswordError(e)) {
                ArchiveBrowseResult.InvalidPassword
            } else {
                ArchiveBrowseResult.Error(e.message ?: "Gagal membaca ZIP")
            }
        } catch (e: Exception) {
            ArchiveBrowseResult.Error(e.message ?: "Gagal membaca ZIP")
        }
    }

    private fun browseRar(rarFilePath: String, password: String? = null): ArchiveBrowseResult {
        var archive: Archive? = null
        return try {
            archive = createRarArchive(rarFilePath, password)

            val entries = mutableListOf<ArchiveEntryInfo>()
            while (true) {
                val header = archive.nextFileHeader() ?: break
                val path = resolveRarEntryName(header) ?: continue
                entries += ArchiveEntryInfo(
                    fullPath = path,
                    isDirectory = invokeBool(header, "isDirectory") ?: path.endsWith('/'),
                    size = invokeLong(header, "getFullUnpackSize") ?: 0L,
                    lastModified = invokeTimeMillis(header)
                )
            }

            ArchiveBrowseResult.Success(entries)
        } catch (e: Exception) {
            val msg = (e.message ?: "").lowercase(Locale.ROOT)
            if (msg.contains("password") || msg.contains("encrypted")) {
                if (password.isNullOrBlank()) ArchiveBrowseResult.PasswordRequired else ArchiveBrowseResult.InvalidPassword
            } else {
                ArchiveBrowseResult.Error(e.message ?: "Gagal membaca RAR")
            }
        } finally {
            try {
                archive?.close()
            } catch (_: Exception) {
            }
        }
    }

    fun extractZip(zipFilePath: String, destPath: String, password: String? = null): Boolean {
        return try {
            val destDir = File(destPath)
            if (!destDir.exists()) destDir.mkdirs()

            val zipFile = buildZipFile(zipFilePath, password)
            if (zipFile.isEncrypted && password.isNullOrBlank()) return false
            zipFile.extractAll(destDir.absolutePath)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun extractZipEntry(
        zipFilePath: String,
        entryPath: String,
        destPath: String,
        password: String? = null
    ): String? {
        return try {
            val zipFile = buildZipFile(zipFilePath, password)
            if (zipFile.isEncrypted && password.isNullOrBlank()) return null

            val destDir = File(destPath)
            if (!destDir.exists()) destDir.mkdirs()

            val header = findHeader(zipFile, entryPath) ?: return null
            if (header.isDirectory) return null

            val outputName = File(header.fileName).name
            zipFile.extractFile(header, destDir.absolutePath, outputName)
            File(destDir, outputName).absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun extractRarEntry(
        rarFilePath: String,
        entryPath: String,
        destPath: String,
        password: String? = null
    ): String? {
        var archive: Archive? = null
        return try {
            archive = createRarArchive(rarFilePath, password)
            val destDir = File(destPath)
            if (!destDir.exists()) destDir.mkdirs()

            while (true) {
                val header = archive.nextFileHeader() ?: break
                val path = resolveRarEntryName(header) ?: continue
                if (path.trimEnd('/') != entryPath.trimEnd('/')) continue

                val isDir = invokeBool(header, "isDirectory") ?: path.endsWith('/')
                if (isDir) return null

                val outputName = File(path).name
                val outputFile = File(destDir, outputName)
                FileOutputStream(outputFile).use { out ->
                    archive.extractFile(header, out)
                }
                return outputFile.absolutePath
            }
            null
        } catch (_: Exception) {
            null
        } finally {
            try {
                archive?.close()
            } catch (_: Exception) {
            }
        }
    }

    fun compressToZip(srcPath: String, zipFilePath: String, password: String? = null): Boolean {
        return try {
            val srcFile = File(srcPath)
            if (!srcFile.exists()) return false

            val zipTarget = File(zipFilePath)
            if (zipTarget.exists()) zipTarget.delete()
            zipTarget.parentFile?.mkdirs()

            val zipFile = buildZipFile(zipFilePath, password)
            val params = ZipParameters().apply {
                compressionMethod = CompressionMethod.DEFLATE
                compressionLevel = CompressionLevel.NORMAL
                if (!password.isNullOrBlank()) {
                    isEncryptFiles = true
                    encryptionMethod = EncryptionMethod.AES
                }
            }

            if (srcFile.isDirectory) {
                zipFile.addFolder(srcFile, params)
            } else {
                zipFile.addFile(srcFile, params)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun replaceZipTextEntry(
        zipFilePath: String,
        entryPath: String,
        content: String,
        password: String?
    ): Boolean {
        return try {
            val zipFile = buildZipFile(zipFilePath, password)
            if (zipFile.isEncrypted && password.isNullOrBlank()) return false

            val normalizedPath = entryPath.replace('\\', '/')
            val tempFile = File.createTempFile("zip_edit_", ".tmp")
            tempFile.writeText(content, Charsets.UTF_8)

            if (zipFile.fileHeaders.any { it.fileName == normalizedPath || it.fileName == "$normalizedPath/" }) {
                zipFile.removeFile(normalizedPath)
            }

            val params = ZipParameters().apply {
                fileNameInZip = normalizedPath
                compressionMethod = CompressionMethod.DEFLATE
                compressionLevel = CompressionLevel.NORMAL
                if (!password.isNullOrBlank()) {
                    isEncryptFiles = true
                    encryptionMethod = EncryptionMethod.AES
                }
            }
            zipFile.addFile(tempFile, params)
            tempFile.delete()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun buildZipFile(path: String, password: String?): ZipFile {
        return if (password.isNullOrBlank()) ZipFile(path) else ZipFile(path, password.toCharArray())
    }

    private fun findHeader(zipFile: ZipFile, entryPath: String): FileHeader? {
        return zipFile.fileHeaders.firstOrNull {
            it.fileName == entryPath || it.fileName == "$entryPath/"
        }
    }

    private fun createRarArchive(path: String, password: String?): Archive {
        return if (password.isNullOrBlank()) {
            Archive(File(path))
        } else {
            // Try password-enabled constructor (varies by junrar versions)
            val ctor = Archive::class.java.constructors.firstOrNull { constructor ->
                val params = constructor.parameterTypes
                params.size == 2 && params[0] == File::class.java && params[1] == String::class.java
            }
            if (ctor != null) {
                ctor.newInstance(File(path), password) as Archive
            } else {
                Archive(File(path))
            }
        }
    }

    private fun resolveRarEntryName(header: Any): String? {
        val fromString = invokeString(header, "getFileNameString")
        if (!fromString.isNullOrBlank()) return fromString

        val fromName = invokeString(header, "getFileName")
        if (!fromName.isNullOrBlank()) return fromName

        return null
    }

    private fun invokeString(target: Any, methodName: String): String? {
        return try {
            target.javaClass.getMethod(methodName).invoke(target) as? String
        } catch (_: Exception) {
            null
        }
    }

    private fun invokeLong(target: Any, methodName: String): Long? {
        return try {
            val value = target.javaClass.getMethod(methodName).invoke(target)
            when (value) {
                is Long -> value
                is Int -> value.toLong()
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun invokeBool(target: Any, methodName: String): Boolean? {
        return try {
            target.javaClass.getMethod(methodName).invoke(target) as? Boolean
        } catch (_: Exception) {
            null
        }
    }

    private fun invokeTimeMillis(header: Any): Long {
        return try {
            val dateObj = header.javaClass.getMethod("getMTime").invoke(header)
            val time = dateObj?.javaClass?.getMethod("getTime")?.invoke(dateObj)
            (time as? Long) ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun isWrongPasswordError(e: ZipException): Boolean {
        val msg = (e.message ?: "").lowercase(Locale.ROOT)
        return msg.contains("wrong password") || msg.contains("password")
    }
}
