package com.example.whatsappbulkmessenger.ui.upload

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.whatsappbulkmessenger.data.model.Contact
import com.example.whatsappbulkmessenger.domain.usecase.PrepareUploadUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UploadUiState(
    val title: String = "WhatsApp Messenger Tool",
    val uploadButtonText: String = "Upload Excel",
    val selectedFileName: String? = null,
    val contacts: List<Contact> = emptyList(),
    val statusMessage: String = "Select an .xlsx file to begin parsing."
)

class UploadViewModel(
    private val prepareUploadUseCase: PrepareUploadUseCase = PrepareUploadUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    fun onFileSelected(contentResolver: ContentResolver, uri: Uri, fileName: String?) {
        val result = prepareUploadUseCase(contentResolver, uri)
        _uiState.value = _uiState.value.copy(
            selectedFileName = fileName,
            contacts = result.contacts,
            statusMessage = result.message
        )
    }
}
