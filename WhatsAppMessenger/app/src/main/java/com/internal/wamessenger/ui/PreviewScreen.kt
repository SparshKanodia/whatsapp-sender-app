package com.internal.wamessenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.internal.wamessenger.core.TemplateEngine
import com.internal.wamessenger.model.Contact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    val state by viewModel.homeState.collectAsState()
    val previews = remember(state.contacts, state.templates) {
        viewModel.getPreviewMessages()
    }
    val missingCount = previews.count { (contact, _) ->
        state.templates.any { t ->
            TemplateEngine.renderWithHighlights(t, contact).second.isNotEmpty()
        }
    }

    Scaffold(
        containerColor = Dark900,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Preview Messages", fontSize = 17.sp,
                            fontWeight = FontWeight.Bold, color = OnDark)
                        Text("${previews.size} messages",
                            fontSize = 12.sp, color = OnDarkMuted)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OnDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Dark800),
                actions = {
                    Button(
                        onClick = {
                            viewModel.startCampaign()
                            onConfirm()
                        },
                        modifier = Modifier.padding(end = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green500),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null,
                            tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Start", color = Color.Black, fontWeight = FontWeight.Bold,
                            fontSize = 13.sp)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Warning banner for missing fields
            if (missingCount > 0) {
                item {
                    InfoBanner(
                        text = "$missingCount message(s) have missing placeholder values (shown in orange).",
                        icon = Icons.Default.Warning,
                        color = WarnYellow
                    )
                }
            }

            itemsIndexed(previews) { index, (contact, message) ->
                PreviewCard(index = index + 1, contact = contact, message = message)
            }
        }
    }
}

@Composable
fun PreviewCard(index: Int, contact: Contact, message: String) {
    val hasMissing = message.contains("[MISSING:")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Dark800)
            .padding(14.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Dark700),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$index", fontSize = 11.sp, color = OnDarkMuted, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        contact.name.ifBlank { "Unknown" },
                        fontSize = 13.sp, fontWeight = FontWeight.Medium, color = OnDark
                    )
                    Text("+${contact.phone}", fontSize = 11.sp, color = OnDarkMuted)
                }
            }
            if (hasMissing) {
                Icon(Icons.Default.Warning, contentDescription = "Missing fields",
                    tint = WarnYellow, modifier = Modifier.size(16.dp))
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = "OK",
                    tint = Green500, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = Dark600)
        Spacer(Modifier.height(10.dp))

        // Message bubble
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Dark700)
                .padding(10.dp)
        ) {
            // Highlight [MISSING: x] parts in yellow
            val annotated = buildAnnotatedString {
                val parts = message.split(Regex("(?=\\[MISSING:)|(?<=\\])"))
                parts.forEach { part ->
                    if (part.startsWith("[MISSING:")) {
                        withStyle(SpanStyle(color = WarnYellow, background = WarnYellow.copy(alpha = 0.15f))) {
                            append(part)
                        }
                    } else {
                        withStyle(SpanStyle(color = OnDark)) {
                            append(part)
                        }
                    }
                }
            }
            Text(
                annotated,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontFamily = FontFamily.Default
            )
        }

        // Char count
        Spacer(Modifier.height(6.dp))
        Text(
            "${message.replace(Regex("\\[MISSING:\\w+]"), "").length} characters",
            fontSize = 10.sp, color = OnDarkMuted,
            modifier = Modifier.align(Alignment.End)
        )
    }
}
