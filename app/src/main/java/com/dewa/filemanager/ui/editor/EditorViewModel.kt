package com.dewa.filemanager.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dewa.filemanager.data.repository.FileManagerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class EditorViewModel(
    private val repository: FileManagerRepository = FileManagerRepository()
) : ViewModel() {

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content

    private val _fileName = MutableStateFlow("")
    val fileName: StateFlow<String> = _fileName

    private var currentPath: String = ""

    fun loadFile(path: String) {
        currentPath = path
        _fileName.value = File(path).name
        viewModelScope.launch {
            _content.value = repository.readFile(path)
        }
    }

    fun updateContent(newContent: String) {
        _content.value = newContent
    }

    suspend fun saveFile(): Boolean {
        return repository.saveFile(currentPath, _content.value)
    }
}
