package com.internal.wamessenger.ui

import androidx.lifecycle.viewModelScope
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.internal.wamessenger.core.CampaignState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSummary: () -> Unit
) {
    val cs by viewModel.campaignState.collectAsState()
    var showStopDialog by remember { mutableStateOf(false) }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            containerColor = Dark800,
            title = { Text("Stop Campaign?", color = OnDark) },
            text = { Text("This will end the campaign. Sent messages cannot be undone.",
                color = OnDarkMuted) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.stopCampaign()
                    showStopDialog = false
                }) {
                    Text("Stop", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("Cancel", color = OnDarkMuted)
                }
            }
        )
    }

    Scaffold(
        containerColor = Dark900,
        topBar = {
            TopAppBar(
                title = { Text("Campaign", fontWeight = FontWeight.Bold,
                    color = OnDark, fontSize = 17.sp) },
                navigationIcon = {
                    if (!cs.isRunning) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OnDark)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Dark800),
                actions = {
                    if (!cs.isComplete && !cs.isStopped) {
                        IconButton(onClick = { showStopDialog = true }) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = ErrorRed)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Big Progress Circle ────────────────────────────────────────────
            BigProgressSection(cs)

            // ── Stats Row ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard("Sent", "${cs.sent}", Green500, Modifier.weight(1f))
                StatCard("Failed", "${cs.failed}", ErrorRed, Modifier.weight(1f))
                StatCard("Skipped", "${cs.skipped}", WarnYellow, Modifier.weight(1f))
            }

            // ── Current Contact ────────────────────────────────────────────────
            AnimatedVisibility(
                visible = cs.currentPhone.isNotEmpty(),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                CurrentContactCard(cs)
            }

            // ── Status Message ─────────────────────────────────────────────────
            AnimatedContent(
                targetState = cs.statusMessage,
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { msg ->
                if (msg.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Dark800)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (cs.isPaused) {
                            Icon(Icons.Default.Pause, tint = WarnYellow,
                                contentDescription = null, modifier = Modifier.size(16.dp))
                        } else if (cs.isRunning) {
                            PulsingDot()
                        } else {
                            Icon(Icons.Default.CheckCircle, tint = Green500,
                                contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(msg, fontSize = 13.sp, color = OnDark, lineHeight = 18.sp)
                    }
                }
            }

            // ── Controls ───────────────────────────────────────────────────────
            if (cs.isRunning || cs.isPaused) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (cs.isPaused) {
                        CampaignButton(
                            label = "Resume", icon = Icons.Default.PlayArrow,
                            color = Green500, textColor = Color.Black,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.resumeCampaign() }
                        )
                    } else {
                        CampaignButton(
                            label = "Pause", icon = Icons.Default.Pause,
                            color = WarnYellow, textColor = Color.Black,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.pauseCampaign() }
                        )
                    }
                    CampaignButton(
                        label = "Stop", icon = Icons.Default.Stop,
                        color = ErrorRed.copy(alpha = 0.15f), textColor = ErrorRed,
                        modifier = Modifier.weight(1f),
                        onClick = { showStopDialog = true }
                    )
                }
            }

            // ── Complete / Stopped ─────────────────────────────────────────────
            if (cs.isComplete || cs.isStopped) {
                Button(
                    onClick = onSummary,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green500)
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null,
                        tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("View Summary", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.viewModelScope.launch { viewModel.exportFailed() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null,
                        modifier = Modifier.size(16.dp), tint = OnDarkMuted)
                    Spacer(Modifier.width(8.dp))
                    Text("Export Failed Messages", color = OnDarkMuted, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun BigProgressSection(cs: CampaignState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Dark800)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { cs.progress },
                modifier = Modifier.size(120.dp),
                strokeWidth = 9.dp,
                color = Green500,
                trackColor = Dark600,
                strokeCap = StrokeCap.Round
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${cs.currentIndex}",
                    fontSize = 32.sp, fontWeight = FontWeight.Bold,
                    color = OnDark, fontFamily = FontFamily.Monospace
                )
                Text("of ${cs.total}", fontSize = 13.sp, color = OnDarkMuted)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Linear progress bar
        LinearProgressIndicator(
            progress = { cs.progress },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = Green500,
            trackColor = Dark600
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "${(cs.progress * 100).toInt()}% complete",
            fontSize = 12.sp, color = OnDarkMuted
        )
    }
}

@Composable
fun CurrentContactCard(cs: CampaignState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Dark800)
            .padding(14.dp)
    ) {
        Text("CURRENT", fontSize = 10.sp, color = OnDarkMuted,
            fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Green500.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    cs.currentName.firstOrNull()?.uppercase() ?: "?",
                    color = Green500, fontWeight = FontWeight.Bold, fontSize = 16.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(cs.currentName.ifBlank { "Unknown" },
                    fontSize = 14.sp, color = OnDark, fontWeight = FontWeight.Medium)
                Text("+${cs.currentPhone}", fontSize = 12.sp, color = OnDarkMuted)
            }
        }
        if (cs.currentMessage.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Dark600)
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Dark700)
                    .padding(10.dp)
            ) {
                Text(cs.currentMessage, fontSize = 12.sp, color = OnDark,
                    lineHeight = 18.sp, maxLines = 4)
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Dark800)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold,
            color = color, fontFamily = FontFamily.Monospace)
        Text(label, fontSize = 11.sp, color = OnDarkMuted)
    }
}

@Composable
fun CampaignButton(
    label: String,
    icon: ImageVector,
    color: Color,
    textColor: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Icon(icon, contentDescription = null, tint = textColor,
            modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = textColor, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}

@Composable
fun PulsingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    Box(
        modifier = Modifier
            .size(10.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Green500)
    )
}
