package com.example.whatsappbulkmessenger.ui.upload

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun UploadScreenRoute(viewModel: UploadViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    UploadScreen(
        title = uiState.title,
        buttonLabel = uiState.uploadButtonText,
        onUploadClick = viewModel::onUploadClicked
    )
}

@Composable
fun UploadScreen(
    title: String,
    buttonLabel: String,
    onUploadClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )

        Button(
            modifier = Modifier.padding(top = 24.dp),
            onClick = onUploadClick
        ) {
            Text(text = buttonLabel)
        }
    }
}
