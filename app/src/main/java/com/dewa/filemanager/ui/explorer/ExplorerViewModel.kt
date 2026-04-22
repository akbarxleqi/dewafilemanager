package com.dewa.filemanager.ui.explorer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dewa.filemanager.data.model.FileEntity
import com.dewa.filemanager.data.repository.ArchiveRepository
import com.dewa.filemanager.data.repository.FileManagerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class ExplorerViewModel(
    private val repository: FileManagerRepository = FileManagerRepository()
) : ViewModel() {

    private val _processingMessage = MutableStateFlow<String?>(null)
    val processingMessage: StateFlow<String?> = _processingMessage

    private val _leftPath = MutableStateFlow(repository.getRootPath())
    val leftPath: StateFlow<String> = _leftPath

    private val _rightPath = MutableStateFlow(repository.getRootPath())
    val rightPath: StateFlow<String> = _rightPath

    private val _leftFiles = MutableStateFlow<List<FileEntity>>(emptyList())
    val leftFiles: StateFlow<List<FileEntity>> = _leftFiles

    private val _rightFiles = MutableStateFlow<List<FileEntity>>(emptyList())
    val rightFiles: StateFlow<List<FileEntity>> = _rightFiles

    private val _storageStats = MutableStateFlow<FileManagerRepository.StorageStats?>(null)
    val storageStats: StateFlow<FileManagerRepository.StorageStats?> = _storageStats

    private val _leftSearchQuery = MutableStateFlow("")
    val leftSearchQuery: StateFlow<String> = _leftSearchQuery

    private val _rightSearchQuery = MutableStateFlow("")
    val rightSearchQuery: StateFlow<String> = _rightSearchQuery

    init {
        refreshAll()
    }

    fun setLeftSearchQuery(query: String) {
        _leftSearchQuery.value = query
        refreshLeft()
    }

    fun setRightSearchQuery(query: String) {
        _rightSearchQuery.value = query
        refreshRight()
    }

    fun refreshAll() {
        refreshLeft()
        refreshRight()
        updateStorageStats()
    }

    fun navigateLeft(newPath: String) {
        _leftPath.value = newPath
        _leftSearchQuery.value = "" // Clear search on navigation
        refreshLeft()
    }

    fun navigateRight(newPath: String) {
        _rightPath.value = newPath
        _rightSearchQuery.value = "" // Clear search on navigation
        refreshRight()
    }

    private fun refreshLeft() {
        viewModelScope.launch {
            val allFiles = repository.listFiles(_leftPath.value)
            _leftFiles.value = if (_leftSearchQuery.value.isEmpty()) {
                allFiles
            } else {
                allFiles.filter { it.name.contains(_leftSearchQuery.value, ignoreCase = true) }
            }
        }
    }

    private fun refreshRight() {
        viewModelScope.launch {
            val allFiles = repository.listFiles(_rightPath.value)
            _rightFiles.value = if (_rightSearchQuery.value.isEmpty()) {
                allFiles
            } else {
                allFiles.filter { it.name.contains(_rightSearchQuery.value, ignoreCase = true) }
            }
        }
    }

    private fun updateStorageStats() {
        _storageStats.value = repository.getStorageStats()
    }

    fun createItem(path: String, name: String, isFolder: Boolean) {
        viewModelScope.launch {
            val file = File(path, name)
            if (isFolder) file.mkdirs() else file.createNewFile()
            refreshAll()
        }
    }

    fun deleteItem(path: String) {
        viewModelScope.launch {
            repository.deleteItem(path)
            refreshAll()
        }
    }

    fun renameItem(oldPath: String, newName: String) {
        viewModelScope.launch {
            repository.renameItem(oldPath, newName)
            refreshAll()
        }
    }

    fun transferFile(srcPath: String, destPath: String, isMove: Boolean) {
        viewModelScope.launch {
            _processingMessage.value = if (isMove) "Memindahkan file..." else "Menyalin file..."
            val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                if (isMove) repository.moveItem(srcPath, destPath) else repository.copyItem(srcPath, destPath)
            }
            if (success) refreshAll()
            _processingMessage.value = null
        }
    }

    fun extractZip(zipPath: String, destPath: String) {
        viewModelScope.launch {
            _processingMessage.value = "Mengekstrak file ZIP..."
            val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                ArchiveRepository.extractZip(zipPath, destPath)
            }
            if (success) refreshAll()
            _processingMessage.value = null
        }
    }

    fun compressToZip(srcPath: String) {
        viewModelScope.launch {
            _processingMessage.value = "Mengkompres ke ZIP..."
            val zipPath = if (srcPath.endsWith("/")) srcPath.dropLast(1) + ".zip" else "$srcPath.zip"
            val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                ArchiveRepository.compressToZip(srcPath, zipPath)
            }
            if (success) refreshAll()
            _processingMessage.value = null
        }
    }

    fun getCounts(files: List<FileEntity>): Pair<Int, Int> {
        val folders = files.count { it.isDirectory }
        val filesCount = files.count { !it.isDirectory }
        return Pair(folders, filesCount)
    }
}
