package com.example.whatsappbulkmessenger.domain.usecase

import android.content.ContentResolver
import android.net.Uri
import com.example.whatsappbulkmessenger.data.repository.ParseContactsResult
import com.example.whatsappbulkmessenger.data.repository.UploadRepository

class PrepareUploadUseCase(
    private val uploadRepository: UploadRepository = UploadRepository()
) {
    operator fun invoke(contentResolver: ContentResolver, uri: Uri): ParseContactsResult {
        return uploadRepository.parseContactsFromExcel(contentResolver, uri)
    }
}
