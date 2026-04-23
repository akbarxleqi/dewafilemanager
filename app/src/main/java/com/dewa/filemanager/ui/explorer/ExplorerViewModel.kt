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

    private val _leftArchiveState = MutableStateFlow<ArchivePaneState?>(null)
    val leftArchiveState: StateFlow<ArchivePaneState?> = _leftArchiveState

    private val _rightArchiveState = MutableStateFlow<ArchivePaneState?>(null)
    val rightArchiveState: StateFlow<ArchivePaneState?> = _rightArchiveState

    init {
        repository.ensureAppDirectories()
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
        _leftArchiveState.value = null
        _leftPath.value = newPath
        _leftSearchQuery.value = "" // Clear search on navigation
        refreshLeft()
    }

    fun navigateRight(newPath: String) {
        _rightArchiveState.value = null
        _rightPath.value = newPath
        _rightSearchQuery.value = "" // Clear search on navigation
        refreshRight()
    }

    fun resetPanelsToRoot() {
        val root = repository.getRootPath()
        _leftArchiveState.value = null
        _rightArchiveState.value = null
        _leftPath.value = root
        _rightPath.value = root
        _leftSearchQuery.value = ""
        _rightSearchQuery.value = ""
        refreshAll()
    }

    fun openRecycleBin(isLeftPane: Boolean) {
        repository.ensureAppDirectories()
        val recyclePath = repository.getRecyclePath()
        if (isLeftPane) {
            _leftArchiveState.value = null
            _leftPath.value = recyclePath
            _leftSearchQuery.value = ""
            refreshLeft()
        } else {
            _rightArchiveState.value = null
            _rightPath.value = recyclePath
            _rightSearchQuery.value = ""
            refreshRight()
        }
    }

    fun setArchiveState(isLeftPane: Boolean, state: ArchivePaneState?) {
        if (isLeftPane) _leftArchiveState.value = state else _rightArchiveState.value = state
    }

    fun getArchiveState(isLeftPane: Boolean): ArchivePaneState? {
        return if (isLeftPane) _leftArchiveState.value else _rightArchiveState.value
    }

    fun setArchivePath(isLeftPane: Boolean, path: String) {
        val state = getArchiveState(isLeftPane) ?: return
        val updatedTop = state.top.copy(currentPath = path)
        val updatedState = state.copy(layers = state.layers.dropLast(1) + updatedTop)
        setArchiveState(isLeftPane, updatedState)
    }

    fun navigateArchiveUp(isLeftPane: Boolean): Boolean {
        val state = getArchiveState(isLeftPane) ?: return false
        val top = state.top
        return if (top.currentPath.isNotBlank()) {
            setArchivePath(isLeftPane, archiveParentPath(top.currentPath))
            true
        } else if (state.layers.size > 1) {
            setArchiveState(isLeftPane, state.copy(layers = state.layers.dropLast(1)))
            true
        } else {
            setArchiveState(isLeftPane, null)
            true
        }
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

    fun compressToZip(srcPath: String, password: String? = null) {
        viewModelScope.launch {
            _processingMessage.value = "Mengkompres ke ZIP..."
            val zipPath = if (srcPath.endsWith("/")) srcPath.dropLast(1) + ".zip" else "$srcPath.zip"
            val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                ArchiveRepository.compressToZip(srcPath, zipPath, password)
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
