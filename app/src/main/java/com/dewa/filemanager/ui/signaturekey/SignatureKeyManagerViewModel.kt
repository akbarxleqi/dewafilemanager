package com.dewa.filemanager.ui.signaturekey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dewa.filemanager.data.model.SignatureKeyCreateRequest
import com.dewa.filemanager.data.model.SignatureKeyEntry
import com.dewa.filemanager.data.repository.SignatureKeyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignatureKeyManagerViewModel(
    private val repository: SignatureKeyRepository = SignatureKeyRepository()
) : ViewModel() {

    private val _entries = MutableStateFlow<List<SignatureKeyEntry>>(emptyList())
    val entries: StateFlow<List<SignatureKeyEntry>> = _entries.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _entries.value = withContext(Dispatchers.IO) { repository.listKeys() }
            _isLoading.value = false
        }
    }

    suspend fun createKey(request: SignatureKeyCreateRequest): Result<SignatureKeyRepository.CreateResult> {
        val result = withContext(Dispatchers.IO) { repository.createKey(request) }
        if (result.isSuccess) refresh()
        return result
    }

    suspend fun deleteKey(path: String): Boolean {
        val deleted = withContext(Dispatchers.IO) { repository.deleteKey(path) }
        if (deleted) refresh()
        return deleted
    }
}
