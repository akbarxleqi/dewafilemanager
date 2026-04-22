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

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

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

    init {
        refreshAll()
    }

    fun refreshAll() {
        refreshLeft()
        refreshRight()
        updateStorageStats()
    }

    fun navigateLeft(newPath: String) {
        _leftPath.value = newPath
        refreshLeft()
    }

    fun navigateRight(newPath: String) {
        _rightPath.value = newPath
        refreshRight()
    }

    private fun refreshLeft() {
        viewModelScope.launch {
            _leftFiles.value = repository.listFiles(_leftPath.value)
        }
    }

    private fun refreshRight() {
        viewModelScope.launch {
            _rightFiles.value = repository.listFiles(_rightPath.value)
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
            _isProcessing.value = true
            val success = if (isMove) repository.moveItem(srcPath, destPath) else repository.copyItem(srcPath, destPath)
            if (success) refreshAll()
            _isProcessing.value = false
        }
    }

    fun extractZip(zipPath: String, destPath: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val success = ArchiveRepository.extractZip(zipPath, destPath)
            if (success) refreshAll()
            _isProcessing.value = false
        }
    }

    fun compressToZip(srcPath: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val zipPath = if (srcPath.endsWith("/")) srcPath.dropLast(1) + ".zip" else "$srcPath.zip"
            val success = ArchiveRepository.compressToZip(srcPath, zipPath)
            if (success) refreshAll()
            _isProcessing.value = false
        }
    }

    fun getCounts(files: List<FileEntity>): Pair<Int, Int> {
        val folders = files.count { it.isDirectory }
        val filesCount = files.count { !it.isDirectory }
        return Pair(folders, filesCount)
    }
}
