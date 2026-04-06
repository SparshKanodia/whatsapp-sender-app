package com.example.whatsappbulkmessenger.ui.upload

import androidx.lifecycle.ViewModel
import com.example.whatsappbulkmessenger.domain.usecase.PrepareUploadUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UploadUiState(
    val title: String = "WhatsApp Messenger Tool",
    val uploadButtonText: String = "Upload Excel"
)

class UploadViewModel(
    private val prepareUploadUseCase: PrepareUploadUseCase = PrepareUploadUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    fun onUploadClicked() {
        prepareUploadUseCase.invoke()
    }
}
