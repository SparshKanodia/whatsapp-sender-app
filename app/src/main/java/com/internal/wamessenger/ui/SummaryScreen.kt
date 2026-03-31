package com.internal.wamessenger.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    viewModel: MainViewModel,
    onNewCampaign: () -> Unit
) {
    val cs by viewModel.campaignState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val elapsed = cs.elapsedSeconds
    val elapsedFormatted = formatDuration(elapsed)
    val successRate = if (cs.sent + cs.failed == 0) 0f
        else cs.sent.toFloat() / (cs.sent + cs.failed) * 100f

    Scaffold(
        containerColor = Dark900,
        topBar = {
            TopAppBar(
                title = { Text("Campaign Summary", fontWeight = FontWeight.Bold,
                    color = OnDark, fontSize = 17.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Dark800)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Hero Card ──────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (cs.isStopped) ErrorRed.copy(alpha = 0.1f)
                        else if (cs.failed == 0) Green500.copy(alpha = 0.1f)
                        else WarnYellow.copy(alpha = 0.1f)
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val (icon, label, color) = when {
                    cs.isStopped -> Triple(Icons.Default.StopCircle, "Stopped Early", ErrorRed)
                    cs.failed == 0 && cs.sent > 0 -> Triple(Icons.Default.CheckCircle, "All Sent!", Green500)
                    else -> Triple(Icons.Default.TaskAlt, "Campaign Done", WarnYellow)
                }
                Icon(icon, contentDescription = null, tint = color,
                    modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(10.dp))
                Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = color)
                Text("Completed in $elapsedFormatted",
                    fontSize = 13.sp, color = OnDarkMuted)
            }

            // ── Stats Grid ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryStatCard("✅ Sent", "${cs.sent}", Green500, Modifier.weight(1f))
                SummaryStatCard("❌ Failed", "${cs.failed}", ErrorRed, Modifier.weight(1f))
                SummaryStatCard("⏭ Skipped", "${cs.skipped}", WarnYellow, Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryStatCard("📊 Total", "${cs.currentIndex}", InfoBlue, Modifier.weight(1f))
                SummaryStatCard("✔ Rate",
                    "${successRate.toInt()}%",
                    if (successRate >= 80) Green500 else WarnYellow,
                    Modifier.weight(1f)
                )
                SummaryStatCard("⏱ Time", elapsedFormatted, OnDarkMuted, Modifier.weight(1f))
            }

            // ── Success Bar ────────────────────────────────────────────────────
            if (cs.sent + cs.failed > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Dark800)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Success Rate", fontSize = 13.sp, color = OnDark)
                        Text("${successRate.toInt()}%", fontSize = 13.sp,
                            color = if (successRate >= 80) Green500 else WarnYellow,
                            fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { successRate / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (successRate >= 80) Green500 else WarnYellow,
                        trackColor = Dark600
                    )
                }
            }

            // ── Action Buttons ─────────────────────────────────────────────────
            if (cs.failed > 0) {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val file = viewModel.exportFailed()
                                Toast.makeText(context,
                                    "Exported to ${file?.path ?: "files"}", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export failed: ${e.message}",
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed.copy(alpha = 0.15f),
                        contentColor = ErrorRed
                    )
                ) {
                    Icon(Icons.Default.Download, contentDescription = null,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export ${cs.failed} Failed Messages as CSV",
                        fontWeight = FontWeight.Medium)
                }
            }

            Button(
                onClick = onNewCampaign,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green500)
            ) {
                Icon(Icons.Default.Add, contentDescription = null,
                    tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("New Campaign", color = Color.Black, fontWeight = FontWeight.Bold,
                    fontSize = 14.sp)
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun SummaryStatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Dark800)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            color = color, fontFamily = FontFamily.Monospace)
        Text(label, fontSize = 10.sp, color = OnDarkMuted, lineHeight = 14.sp)
    }
}

private fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return "0s"
    val h = TimeUnit.SECONDS.toHours(seconds)
    val m = TimeUnit.SECONDS.toMinutes(seconds) % 60
    val s = seconds % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${s}s"
        else -> "${s}s"
    }
}
