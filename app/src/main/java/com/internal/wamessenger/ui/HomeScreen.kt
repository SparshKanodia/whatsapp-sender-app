package com.internal.wamessenger.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.internal.wamessenger.accessibility.WhatsAppAccessibilityService
import com.internal.wamessenger.core.QueueManager
import com.internal.wamessenger.core.WhatsAppLauncher
import com.internal.wamessenger.core.TemplateEngine

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onPreview: () -> Unit,
    onStartCampaign: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.homeState.collectAsState()
    val queueState by viewModel.queueState.collectAsState()

    val isAccessibilityEnabled = WhatsAppAccessibilityService.isServiceEnabled
    val isWhatsAppInstalled = WhatsAppLauncher.isWhatsAppInstalled(context)

    // CSV file picker
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileName = context.contentResolver.query(it, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                c.moveToFirst()
                c.getString(idx)
            } ?: "file.csv"
            viewModel.loadCsv(it, fileName)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Dark900)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Header ─────────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Green500),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Send, contentDescription = null,
                    tint = Color.Black, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("WA Messenger", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = OnDark, fontFamily = FontFamily.Monospace)
                Text("Internal Campaign Tool", fontSize = 12.sp, color = OnDarkMuted)
            }
        }

        // ── Status Chips ───────────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusChip(
                label = if (isWhatsAppInstalled) "WhatsApp ✓" else "WhatsApp ✗",
                ok = isWhatsAppInstalled
            )
            StatusChip(
                label = if (isAccessibilityEnabled) "Accessibility ✓" else "Accessibility ✗",
                ok = isAccessibilityEnabled,
                onClick = if (!isAccessibilityEnabled) {
                    { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                } else null
            )
        }
        if (!isAccessibilityEnabled) {
            InfoBanner(
                text = "Tap 'Accessibility ✗' → find 'WA Messenger Auto-Send' → enable it for auto-send to work.",
                icon = Icons.Default.Warning,
                color = WarnYellow
            )
        }

        // ── Resume Banner ──────────────────────────────────────────────────────
        if (queueState.hasActiveCampaign) {
            ResumeBanner(
                index = queueState.currentIndex,
                total = queueState.contacts.size,
                onClick = onStartCampaign
            )
        }

        // ── CSV Section ────────────────────────────────────────────────────────
        SectionCard(title = "1. Upload CSV") {
            if (state.csvFileName.isEmpty()) {
                Button(
                    onClick = { csvLauncher.launch("text/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Dark700, contentColor = OnDark
                    )
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Choose CSV File")
                }
                Spacer(Modifier.height(6.dp))
                Text("Required columns: phone  |  Optional: name, company, role, ...",
                    fontSize = 11.sp, color = OnDarkMuted)
            } else {
                // File loaded
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Dark700)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(state.csvFileName, fontSize = 13.sp, color = OnDark,
                            fontWeight = FontWeight.Medium, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Text("${state.contacts.size} contacts loaded" +
                                if (state.contacts.size >= QueueManager.MAX_RECIPIENTS) " (max)" else "",
                            fontSize = 12.sp, color = Green500)
                    }
                    IconButton(onClick = { csvLauncher.launch("text/*") }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Change file",
                            tint = OnDarkMuted)
                    }
                }

                // Errors
                state.csvErrors.forEach { err ->
                    Spacer(Modifier.height(4.dp))
                    InfoBanner(text = err, icon = Icons.Default.Error, color = ErrorRed)
                }
                // Warnings
                state.csvWarnings.take(3).forEach { warn ->
                    Spacer(Modifier.height(4.dp))
                    InfoBanner(text = warn, icon = Icons.Default.Warning, color = WarnYellow)
                }
                if (state.csvWarnings.size > 3) {
                    Text("+ ${state.csvWarnings.size - 3} more warnings",
                        fontSize = 11.sp, color = WarnYellow,
                        modifier = Modifier.padding(top = 2.dp))
                }
            }
        }

        // ── Template Section ───────────────────────────────────────────────────
        SectionCard(title = "2. Message Template(s)") {
            Text("Use {name}, {company}, {phone}, or any CSV column name as placeholders.",
                fontSize = 11.sp, color = OnDarkMuted)
            Spacer(Modifier.height(10.dp))

            state.templates.forEachIndexed { index, template ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = template,
                        onValueChange = { viewModel.updateTemplate(index, it) },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("Hi {name}, this is a message from {company}...",
                                fontSize = 12.sp, color = OnDarkMuted)
                        },
                        minLines = 3,
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Green500,
                            unfocusedBorderColor = Dark600,
                            focusedTextColor = OnDark,
                            unfocusedTextColor = OnDark,
                            cursorColor = Green500
                        ),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 13.sp, fontFamily = FontFamily.Monospace
                        )
                    )
                    if (state.templates.size > 1) {
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = { viewModel.removeTemplate(index) },
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove",
                                tint = ErrorRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Live preview of first contact
                if (state.contacts.isNotEmpty() && template.isNotBlank()) {
                    val (rendered, missing) = TemplateEngine.renderWithHighlights(
                        template, state.contacts.first()
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Dark700)
                            .padding(10.dp)
                    ) {
                        Text("Preview: $rendered",
                            fontSize = 12.sp,
                            color = if (missing.isEmpty()) Green100 else WarnYellow)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            OutlinedButton(
                onClick = { viewModel.addTemplate() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Dark600)
            ) {
                Icon(Icons.Default.Add, contentDescription = null,
                    modifier = Modifier.size(16.dp), tint = OnDarkMuted)
                Spacer(Modifier.width(6.dp))
                Text("Add Template Variation", fontSize = 12.sp, color = OnDarkMuted)
            }
        }

        // ── Options Section ────────────────────────────────────────────────────
        SectionCard(title = "3. Options") {
            ToggleRow(
                label = "Test Mode (Dry Run)",
                description = "Simulate sends without opening WhatsApp",
                checked = state.testMode,
                onCheckedChange = { viewModel.setTestMode(it) }
            )
            HorizontalDivider(color = Dark600, modifier = Modifier.padding(vertical = 10.dp))
            ToggleRow(
                label = "Manual Send Mode",
                description = "App opens WhatsApp — you tap Send yourself",
                checked = state.manualMode,
                onCheckedChange = { viewModel.setManualMode(it) }
            )
        }

        // ── Validation Errors ──────────────────────────────────────────────────
        AnimatedVisibility(visible = state.validationErrors.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.validationErrors.forEach { err ->
                    InfoBanner(text = err, icon = Icons.Default.Error, color = ErrorRed)
                }
            }
        }

        // ── Action Buttons ─────────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onPreview,
                enabled = state.contacts.isNotEmpty(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (state.contacts.isNotEmpty()) Green500 else Dark600)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Preview")
            }
            Button(
                onClick = {
                    if (viewModel.validate()) {
                        viewModel.startCampaign()
                        onStartCampaign()
                    }
                },
                enabled = state.contacts.isNotEmpty(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green500)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null,
                    tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Run Campaign", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Reusable Components ────────────────────────────────────────────────────────

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Dark800)
            .padding(16.dp)
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = OnDarkMuted, letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 12.dp))
        content()
    }
}

@Composable
fun StatusChip(label: String, ok: Boolean, onClick: (() -> Unit)? = null) {
    val bg = if (ok) Green700.copy(alpha = 0.3f) else ErrorRed.copy(alpha = 0.15f)
    val textColor = if (ok) Green500 else ErrorRed
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun InfoBanner(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = color,
            modifier = Modifier.size(15.dp).padding(top = 1.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 12.sp, color = color, lineHeight = 17.sp)
    }
}

@Composable
fun ToggleRow(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, fontSize = 14.sp, color = OnDark, fontWeight = FontWeight.Medium)
            Text(description, fontSize = 11.sp, color = OnDarkMuted, lineHeight = 15.sp)
        }
        Switch(
            checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Green500
            )
        )
    }
}

@Composable
fun ResumeBanner(index: Int, total: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(InfoBlue.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Resume Campaign?", fontSize = 13.sp, color = InfoBlue,
                fontWeight = FontWeight.Bold)
            Text("Paused at $index / $total contacts", fontSize = 12.sp, color = OnDarkMuted)
        }
        Icon(Icons.Default.ArrowForward, contentDescription = "Resume",
            tint = InfoBlue)
    }
}
