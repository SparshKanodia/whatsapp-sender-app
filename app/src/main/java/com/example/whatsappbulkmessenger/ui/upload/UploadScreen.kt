package com.example.whatsappbulkmessenger.ui.upload

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.whatsappbulkmessenger.data.model.Contact

@Composable
fun UploadScreenRoute(viewModel: UploadViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri ?: return@rememberLauncherForActivityResult
            val fileName = context.contentResolver.getDisplayName(uri)
            viewModel.onFileSelected(context.contentResolver, uri, fileName)
        }
    )

    UploadScreen(
        title = uiState.title,
        buttonLabel = uiState.uploadButtonText,
        selectedFileName = uiState.selectedFileName,
        statusMessage = uiState.statusMessage,
        contacts = uiState.contacts,
        onUploadClick = { documentLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) }
    )
}

@Composable
fun UploadScreen(
    title: String,
    buttonLabel: String,
    selectedFileName: String?,
    statusMessage: String,
    contacts: List<Contact>,
    onUploadClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Button(onClick = onUploadClick) {
            Text(text = buttonLabel)
        }

        if (!selectedFileName.isNullOrBlank()) {
            Text(
                text = "File: $selectedFileName",
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text(
            text = statusMessage,
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyMedium
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(contacts) { contact ->
                ContactItem(contact = contact)
            }
        }
    }
}

@Composable
private fun ContactItem(contact: Contact) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = contact.name, fontWeight = FontWeight.Bold)
            Text(text = contact.phone, style = MaterialTheme.typography.bodyMedium)

            contact.extraFields.forEach { (key, value) ->
                Text(
                    text = "$key: $value",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private fun android.content.ContentResolver.getDisplayName(uri: Uri): String? {
    return query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex < 0 || !cursor.moveToFirst()) null else cursor.getString(nameIndex)
    }
}
