package com.dewa.filemanager.ui.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dewa.filemanager.data.model.PasswordEntry
import com.dewa.filemanager.data.repository.PasswordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PasswordManagerViewModel(
    private val repo: PasswordRepository = PasswordRepository()
) : ViewModel() {

    private val _entries = MutableStateFlow<List<PasswordEntry>>(emptyList())
    val entries: StateFlow<List<PasswordEntry>> = _entries.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _entries.value = withContext(Dispatchers.IO) { repo.loadAll() }
            _isLoading.value = false
        }
    }

    suspend fun addEntry(name: String, contact: String, password: String): Boolean {
        val ok = withContext(Dispatchers.IO) { repo.add(name, contact, password) }
        if (ok) refresh()
        return ok
    }

    suspend fun updateEntry(id: String, name: String, contact: String, password: String): Boolean {
        val ok = withContext(Dispatchers.IO) { repo.update(id, name, contact, password) }
        if (ok) refresh()
        return ok
    }

    suspend fun deleteEntry(id: String): Boolean {
        val ok = withContext(Dispatchers.IO) { repo.delete(id) }
        if (ok) refresh()
        return ok
    }
}
