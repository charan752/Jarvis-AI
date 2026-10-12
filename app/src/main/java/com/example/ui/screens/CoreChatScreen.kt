package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.diagnostics.DiagnosticItem
import com.example.data.diagnostics.DiagnosticStatus
import com.example.data.diagnostics.DiagnosticSuiteReport
import com.example.data.model.ChatMessage
import com.example.data.model.JarvisState
import com.example.data.model.LanguageMode
import com.example.data.model.MessageSender
import com.example.data.model.MessagingServiceType
import com.example.ui.JarvisViewModel
import com.example.ui.components.AudioPulseWaveform
import com.example.ui.components.JarvisOrb
import com.example.ui.components.StatusPill
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDarkBg
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisSky
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun CoreChatScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.jarvisState.collectAsState()
    val audioLevel by viewModel.audioLevel.collectAsState()
    val isLiveMode by viewModel.isLiveMode.collectAsState()
    val activeVoiceName by viewModel.activeVoiceName.collectAsState()
    val isSpeechSupported by viewModel.isSpeechRecognitionSupported.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val language by viewModel.languageMode.collectAsState()
    val diagnosticReport by viewModel.diagnosticReport.collectAsState()
    val showDiagnosticsDialog by viewModel.showDiagnosticsDialog.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Core Diagnostics Dialog
    if (showDiagnosticsDialog) {
        CoreDiagnosticsDialog(
            report = diagnosticReport,
            onDismiss = { viewModel.dismissDiagnosticsDialog() },
            onRerun = { viewModel.startCoreDiagnostics() }
        )
    }

    // Auto-scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "Clear Room Chat History?",
                    color = JarvisTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Text(
                    text = "This will purge all ${messages.size} stored interactions from the local Room database (jarvis_core_db). Previous chat context will be reset.",
                    color = JarvisTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearConversation()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.JarvisRed)
                ) {
                    Text("Clear All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = JarvisCyan)
                }
            },
            containerColor = JarvisSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisDarkBg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. TOP FUTURISTIC ORB & STATUS SECTION ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(JarvisSurface.copy(alpha = 0.5f))
                .padding(top = 10.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(state = state)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Room Database Persistent History Counter & Clear Trigger
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(JarvisSurfaceVariant)
                            .border(1.dp, JarvisBorder, RoundedCornerShape(20.dp))
                            .clickable { showClearConfirmDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("room_history_pill"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Room History",
                            tint = JarvisSky,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${messages.size} msgs",
                            color = JarvisSky,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Live Mode Toggle Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isLiveMode) Color(0xFF0F2E1B) else JarvisSurfaceVariant)
                            .border(
                                1.dp,
                                if (isLiveMode) JarvisGreen else JarvisBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.toggleLiveMode() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("live_mode_toggle_pill"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isLiveMode) JarvisGreen else JarvisTextMuted)
                        )
                        Text(
                            text = if (isLiveMode) "LIVE: ON" else "LIVE: OFF",
                            color = if (isLiveMode) JarvisGreen else JarvisTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // --- SUB-BAR: SMALL "CORE DIAGNOSTICS" BUTTON & SUBSYSTEM TELEMETRY ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Small "CORE DIAGNOSTICS" button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (diagnosticReport?.isRunning == true) Color(0xFF2B2005) else Color(0xFF041926))
                        .border(
                            1.dp,
                            if (diagnosticReport?.isRunning == true) JarvisGold else JarvisCyan,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { viewModel.openDiagnosticsDialog() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("core_diagnostics_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Run Core Diagnostics",
                        tint = if (diagnosticReport?.isRunning == true) JarvisGold else JarvisCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (diagnosticReport?.isRunning == true) "PROBING SUBSYSTEMS..." else "CORE DIAGNOSTICS",
                        color = if (diagnosticReport?.isRunning == true) JarvisGold else JarvisCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                // Subsystem health preview indicator / status
                diagnosticReport?.let { report ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(JarvisSurfaceVariant)
                            .border(
                                1.dp,
                                if (report.isRunning) JarvisGold.copy(alpha = 0.5f) else JarvisGreen.copy(alpha = 0.5f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.showDiagnosticsDialog.value = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("diagnostic_status_badge"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (report.isRunning) JarvisGold else JarvisGreen)
                        )
                        Text(
                            text = if (report.isRunning) "CHECKING ${report.completedCount}/${report.totalCount}" else "ALL 10 VERIFIED",
                            color = if (report.isRunning) JarvisGold else JarvisGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Central Animated Orb with smooth state transitions
            JarvisOrb(
                state = state,
                audioLevel = audioLevel,
                size = 140.dp,
                modifier = Modifier
                    .testTag("jarvis_central_orb")
                    .clickable {
                        if (state == JarvisState.SPEAKING) {
                            viewModel.interruptSpeech()
                        } else if (state == JarvisState.LISTENING) {
                            viewModel.stopListening()
                        } else {
                            viewModel.startListening()
                        }
                    }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Animated Real-Time Audio Pulse Waveform (Reactive to Speech & Processing)
            AudioPulseWaveform(
                state = state,
                audioLevel = audioLevel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                height = 78.dp,
                showControls = true
            )

            // State and Live Mode feedback cues
            if (isLiveMode) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp, bottom = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0D2518))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(JarvisGreen)
                    )
                    Text(
                        text = "CONTINUOUS LISTENING // Say 'Stop listening' to halt",
                        color = JarvisGreen,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                AnimatedVisibility(
                    visible = state == JarvisState.SPEAKING,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "Tap Orb or Mic to interrupt speech",
                        color = JarvisCyan.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // --- 1.4 HOME_ARRIVAL DETECTION STATUS BAR (With Smooth Fade-in Animation) ---
        val protocolStatus by viewModel.protocolStatus.collectAsState()
        val pendingArrival by viewModel.pendingHomeArrivalState.collectAsState()
        val isHomeArrival = (protocolStatus?.phase?.contains("HOME ARRIVAL") == true) || (pendingArrival != null)

        AnimatedVisibility(
            visible = isHomeArrival,
            enter = fadeIn(animationSpec = tween(durationMillis = 650, easing = LinearOutSlowInEasing)) +
                    expandVertically(animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 350, easing = FastOutLinearInEasing)) +
                    shrinkVertically(animationSpec = tween(durationMillis = 350, easing = FastOutLinearInEasing)),
            modifier = Modifier.testTag("home_arrival_status_bar")
        ) {
            val proto = protocolStatus
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF261905))
                    .border(1.dp, JarvisGold.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(JarvisGold)
                        )
                        Text(
                            text = "JARVIS // HOME ARRIVAL",
                            color = JarvisGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = proto?.phase ?: "HOME ARRIVAL DETECTED",
                        color = JarvisGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                val detailsText = proto?.details ?: "Home perimeter reached. Scanning authorized messaging channels..."
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detailsText,
                    color = JarvisTextPrimary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                // If awaiting Home Arrival confirmation, show quick action buttons
                if (pendingArrival != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(JarvisCyan.copy(alpha = 0.25f))
                                .border(1.dp, JarvisCyan, RoundedCornerShape(6.dp))
                                .clickable { viewModel.openHomeArrivalService(MessagingServiceType.INSTAGRAM) }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("btn_open_instagram")
                        ) {
                            Text("Instagram", color = JarvisCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(JarvisCyan.copy(alpha = 0.25f))
                                .border(1.dp, JarvisCyan, RoundedCornerShape(6.dp))
                                .clickable { viewModel.openHomeArrivalService(MessagingServiceType.TELEGRAM) }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("btn_open_telegram")
                        ) {
                            Text("Telegram", color = JarvisCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(JarvisGreen.copy(alpha = 0.25f))
                                .border(1.dp, JarvisGreen, RoundedCornerShape(6.dp))
                                .clickable { viewModel.openBothHomeArrivalServices() }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("btn_open_both")
                        ) {
                            Text("Both", color = JarvisGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(JarvisSurfaceVariant)
                                .border(1.dp, JarvisBorder, RoundedCornerShape(6.dp))
                                .clickable { viewModel.dismissHomeArrivalPending() }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("btn_dismiss_arrival")
                        ) {
                            Text("Dismiss", color = JarvisTextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // --- 1.5 PROTOCOL HUD ACTIVITY PANEL (Real-time Information Retrieval Status) ---
        AnimatedVisibility(
            visible = !isHomeArrival && (protocolStatus != null || state == JarvisState.SEARCHING || state == JarvisState.ANALYZING),
            enter = fadeIn(animationSpec = tween(durationMillis = 400)),
            exit = fadeOut(animationSpec = tween(durationMillis = 300))
        ) {
            val proto = protocolStatus
            val phaseLabel = proto?.phase ?: when (state) {
                JarvisState.SEARCHING -> "SEARCHING WEB"
                JarvisState.ANALYZING -> "ANALYZING"
                else -> "PROCESSING"
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF041926))
                    .border(1.dp, JarvisCyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (phaseLabel.contains("READY")) JarvisGreen else JarvisCyan)
                        )
                        Text(
                            text = "JARVIS // PROTOCOL",
                            color = JarvisCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = phaseLabel,
                        color = if (phaseLabel.contains("READY")) JarvisGreen else JarvisGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (!proto?.query.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = proto.query,
                        color = JarvisTextPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!proto?.details.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = proto.details,
                        color = JarvisTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (proto != null && proto.sourcesFound > 0) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "SOURCE RETRIEVAL: ${proto.sourcesFound} sources found",
                        color = JarvisSky,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // --- 1.6 WEBSITE FALLBACK CONFIRMATION BANNER ---
        val pendingWebsiteFallback by viewModel.pendingWebsiteFallbackState.collectAsState()
        AnimatedVisibility(
            visible = pendingWebsiteFallback != null,
            enter = fadeIn(animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)) +
                    expandVertically(animationSpec = tween(durationMillis = 400)),
            exit = fadeOut(animationSpec = tween(durationMillis = 300)) +
                    shrinkVertically(animationSpec = tween(durationMillis = 300)),
            modifier = Modifier.testTag("website_fallback_banner")
        ) {
            pendingWebsiteFallback?.let { pending ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F1E28))
                        .border(1.dp, JarvisSky.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "JARVIS // APP DETECTION",
                        color = JarvisSky,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${pending.displayName} isn't installed, sir. Would you like me to open the official website?",
                        color = JarvisTextPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(JarvisCyan.copy(alpha = 0.3f))
                                .border(1.dp, JarvisCyan, RoundedCornerShape(6.dp))
                                .clickable { viewModel.confirmWebsiteFallback(true) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("btn_confirm_website")
                        ) {
                            Text("Open Website", color = JarvisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(JarvisSurfaceVariant)
                                .border(1.dp, JarvisBorder, RoundedCornerShape(6.dp))
                                .clickable { viewModel.confirmWebsiteFallback(false) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("btn_cancel_website")
                        ) {
                            Text("No", color = JarvisTextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // --- 2. CONVERSATION PANEL ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(
                    message = msg,
                    onSpeakAgain = { viewModel.voiceManager.speak(msg.text) },
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("JARVIS Text", msg.text))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = {
                        viewModel.deleteMessage(msg.id)
                    }
                )
            }
        }

        // --- 3. QUICK TELEMETRY & VOICE SIMULATION CHIPS ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (state == JarvisState.LISTENING) "🎙️ LISTENING // Speak or tap test utterance:" else "QUICK TELEMETRY & VOICE COMMANDS:",
                    color = if (state == JarvisState.LISTENING) JarvisCyan else JarvisTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = activeVoiceName,
                    color = JarvisSky,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val voiceCommands = listOf(
                    "🏠 I'm back home" to "I'm back home",
                    "⚡ Core diagnostics" to "Run core diagnostics suite on all subsystems",
                    "Hey Jarvis, what's the time?" to "What is the exact current time and date?",
                    "System diagnostics" to "Give me full system diagnostics and battery metrics",
                    "Search latest AI news" to "Search the web for the latest artificial intelligence breakthroughs",
                    "Open YouTube" to "Open YouTube for me",
                    "Explain Quantum Physics" to "Explain Quantum Entanglement simply",
                    "Stop listening" to "Stop listening"
                )

                items(voiceCommands) { (label, prompt) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (state == JarvisState.LISTENING) Color(0xFF0C243B) else JarvisSurfaceVariant)
                            .border(1.dp, if (state == JarvisState.LISTENING) JarvisCyan else JarvisBorder, RoundedCornerShape(14.dp))
                            .clickable {
                                if (state == JarvisState.LISTENING) {
                                    viewModel.simulateVoiceCommand(prompt)
                                } else {
                                    viewModel.sendMessage(prompt)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (state == JarvisState.LISTENING) JarvisCyan else JarvisSky,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // --- 4. BOTTOM INPUT & VOICE CONTROLS ---
        AnimatedVisibility(
            visible = state == JarvisState.LISTENING || state == JarvisState.THINKING,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (state == JarvisState.LISTENING) Color(0xFF041926) else Color(0xFF261904))
                    .border(
                        1.dp,
                        if (state == JarvisState.LISTENING) JarvisCyan.copy(alpha = 0.4f) else JarvisGold.copy(alpha = 0.4f)
                    )
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (state == JarvisState.LISTENING) JarvisCyan else JarvisGold)
                    )
                    Text(
                        text = if (state == JarvisState.LISTENING) "SPEECH INGRESS ACTIVE // SPEAK NOW" else "NEURAL REASONING // GEMINI PROCESSING...",
                        color = if (state == JarvisState.LISTENING) JarvisCyan else JarvisGold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (state == JarvisState.LISTENING) "${(audioLevel * 100).toInt()}% LEVEL" else "SYNTHESIS",
                    color = JarvisTextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Surface(
            color = JarvisSurface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Prominent Futuristic Microphone Button
                val micColor = when (state) {
                    JarvisState.LISTENING -> JarvisCyan
                    JarvisState.SPEAKING -> JarvisGold
                    else -> JarvisBorder
                }

                FloatingActionButton(
                    onClick = {
                        if (state == JarvisState.LISTENING) {
                            viewModel.stopListening()
                        } else if (state == JarvisState.SPEAKING) {
                            viewModel.interruptSpeech()
                        } else {
                            viewModel.startListening()
                        }
                    },
                    containerColor = if (state == JarvisState.LISTENING) JarvisCyan else JarvisSurfaceVariant,
                    contentColor = if (state == JarvisState.LISTENING) Color(0xFF00363D) else JarvisCyan,
                    modifier = Modifier
                        .size(50.dp)
                        .border(1.5.dp, micColor, CircleShape)
                        .testTag("microphone_button"),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = when (state) {
                            JarvisState.LISTENING -> Icons.Default.Mic
                            JarvisState.SPEAKING -> Icons.Default.Stop
                            else -> Icons.Default.Mic
                        },
                        contentDescription = "Voice Input"
                    )
                }

                // Dedicated Live Mode Continuous Listening Quick Button
                IconButton(
                    onClick = { viewModel.toggleLiveMode() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isLiveMode) Color(0xFF0F361F) else JarvisSurfaceVariant)
                        .border(
                            1.dp,
                            if (isLiveMode) JarvisGreen else JarvisBorder,
                            CircleShape
                        )
                        .testTag("live_mode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Toggle Live Mode",
                        tint = if (isLiveMode) JarvisGreen else JarvisTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Text Input Bar with glowing styling
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.inputText.value = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    placeholder = {
                        Text(
                            text = if (state == JarvisState.LISTENING) "Listening..." else "Command JARVIS...",
                            color = JarvisTextMuted,
                            fontSize = 13.sp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { viewModel.sendMessage() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisBorder,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary,
                        focusedContainerColor = Color(0xFF08101E),
                        unfocusedContainerColor = Color(0xFF08101E)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )

                // Send Button
                IconButton(
                    onClick = { viewModel.sendMessage() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) JarvisCyan else JarvisSurfaceVariant)
                        .testTag("send_message_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Command",
                        tint = if (inputText.isNotBlank()) Color(0xFF00363D) else JarvisTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onSpeakAgain: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    val context = LocalContext.current
    val formattedTime = remember(message.timestamp) {
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        sdf.format(java.util.Date(message.timestamp))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Sender Header + Formatted Timestamp
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
        ) {
            Text(
                text = if (isUser) "COMMANDER" else "JARVIS // PROTOCOL",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isUser) JarvisSky else JarvisCyan
            )
            Text(
                text = "• $formattedTime",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = JarvisTextMuted
            )
        }

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isUser) 14.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 14.dp
                    )
                )
                .background(if (isUser) Color(0xFF0D2545) else JarvisSurfaceVariant)
                .border(
                    1.dp,
                    if (isUser) Color(0xFF1B457B) else JarvisBorder,
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isUser) 14.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 14.dp
                    )
                )
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    color = JarvisTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                // Verified Clickable Sources (Requirement #5: Source Transparency)
                val sourcesList = remember(message.text) {
                    parseSources(message.text)
                }

                if (sourcesList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SOURCES // VERIFIED REFERENCES",
                        color = JarvisSky,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(sourcesList) { (sourceName, sourceUrl) ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0C243B))
                                    .border(1.dp, JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (sourceUrl.isNotBlank()) {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl)).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Could not open source URL", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Open Source Link",
                                    tint = JarvisCyan,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = sourceName,
                                    color = JarvisCyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Tool Execution Card (if message invoked a tool)
                message.toolCall?.let { tool ->
                    val isLiveSearch = tool.toolName == "searchWeb" || tool.toolName == "getNews" || tool.toolName == "getCurrentInformation"
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF070F1D))
                            .border(
                                1.dp,
                                if (tool.isSimulated) JarvisGold.copy(alpha = 0.5f) else JarvisCyan.copy(alpha = 0.5f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "TOOL: ${tool.toolName}",
                                color = JarvisCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            if (isLiveSearch) {
                                Text(
                                    text = "[LIVE WEB]",
                                    color = JarvisCyan,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (tool.isSimulated) {
                                Text(
                                    text = "[SIMULATED]",
                                    color = JarvisGold,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (tool.resultMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tool.resultMessage,
                                color = JarvisTextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Action icons (speak, copy, delete from Room)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!isUser) {
                        IconButton(
                            onClick = onSpeakAgain,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak Text",
                                tint = JarvisTextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Text",
                            tint = JarvisTextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete from Room",
                            tint = JarvisTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun parseSources(text: String): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()
    if (!text.contains("Sources:", ignoreCase = true)) return result

    val afterSources = text.substringAfter("Sources:", "").trim()
    val lines = afterSources.lines()
    for (line in lines) {
        val clean = line.trim().removePrefix("•").removePrefix("-").removePrefix("*").trim()
        if (clean.isBlank()) continue
        // Format: Name (URL) or Name: URL or just URL or Name
        val parenMatch = Regex("^(.*?)\\s*\\((https?://[^)]+)\\)").find(clean)
        if (parenMatch != null) {
            val name = parenMatch.groupValues[1].trim()
            val url = parenMatch.groupValues[2].trim()
            result.add(Pair(name.ifEmpty { "Source" }, url))
            continue
        }
        val colonMatch = Regex("^(.*?):\\s*(https?://\\S+)").find(clean)
        if (colonMatch != null) {
            val name = colonMatch.groupValues[1].trim()
            val url = colonMatch.groupValues[2].trim()
            result.add(Pair(name.ifEmpty { "Source" }, url))
            continue
        }
        val urlMatch = Regex("https?://\\S+").find(clean)
        if (urlMatch != null) {
            val url = urlMatch.value
            val name = clean.replace(url, "").trim().ifEmpty { "Web Source" }
            result.add(Pair(name, url))
        } else if (clean.length < 40 && !clean.contains("\n")) {
            result.add(Pair(clean, "https://www.google.com/search?q=" + Uri.encode(clean)))
        }
    }
    return result
}

@Composable
fun CoreDiagnosticsDialog(
    report: DiagnosticSuiteReport?,
    onDismiss: () -> Unit,
    onRerun: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onRerun,
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Rerun",
                    tint = JarvisDarkBg,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RERUN DIAGNOSTICS",
                    color = JarvisDarkBg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(
                    text = "CLOSE",
                    color = JarvisTextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Core Diagnostics",
                        tint = JarvisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "CORE DIAGNOSTICS",
                        color = JarvisTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                if (report != null) {
                    Text(
                        text = if (report.isRunning) "${report.completedCount}/${report.totalCount}" else "${report.durationMs}ms",
                        color = if (report.isRunning) JarvisGold else JarvisSky,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status banner
                val overall = report?.overallStatus ?: "INITIALIZING"
                val isNominal = overall.contains("NOMINAL")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (report?.isRunning == true) Color(0xFF261D03) else if (isNominal) Color(0xFF072615) else JarvisSurfaceVariant)
                        .border(
                            1.dp,
                            if (report?.isRunning == true) JarvisGold.copy(alpha = 0.6f) else if (isNominal) JarvisGreen.copy(alpha = 0.6f) else JarvisBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STATUS // $overall",
                        color = if (report?.isRunning == true) JarvisGold else if (isNominal) JarvisGreen else JarvisTextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (report?.isRunning == true) {
                        Text(
                            text = "PROBING...",
                            color = JarvisGold,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Subsystems list (all 10 items)
                val items = report?.items ?: emptyList()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        DiagnosticItemCard(item = item)
                    }
                }
            }
        },
        containerColor = JarvisSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun DiagnosticItemCard(item: DiagnosticItem) {
    val borderColor = when (item.status) {
        DiagnosticStatus.PASSED -> JarvisGreen.copy(alpha = 0.5f)
        DiagnosticStatus.RUNNING -> JarvisGold.copy(alpha = 0.8f)
        DiagnosticStatus.WARNING -> JarvisGold.copy(alpha = 0.6f)
        DiagnosticStatus.FAILED -> com.example.ui.theme.JarvisRed.copy(alpha = 0.6f)
        DiagnosticStatus.PENDING -> JarvisBorder
    }

    val iconText = when (item.status) {
        DiagnosticStatus.PASSED -> "✓"
        DiagnosticStatus.RUNNING -> "⏳"
        DiagnosticStatus.WARNING -> "⚠"
        DiagnosticStatus.FAILED -> "✗"
        DiagnosticStatus.PENDING -> "○"
    }

    val iconColor = when (item.status) {
        DiagnosticStatus.PASSED -> JarvisGreen
        DiagnosticStatus.RUNNING -> JarvisGold
        DiagnosticStatus.WARNING -> JarvisGold
        DiagnosticStatus.FAILED -> com.example.ui.theme.JarvisRed
        DiagnosticStatus.PENDING -> JarvisTextMuted
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(JarvisDarkBg.copy(alpha = 0.8f))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = iconText,
                    color = iconColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = item.name,
                    color = JarvisTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (item.latencyMs > 0 || item.status == DiagnosticStatus.PASSED) {
                Text(
                    text = "${item.latencyMs}ms",
                    color = JarvisSky,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.summary,
            color = if (item.status == DiagnosticStatus.RUNNING) JarvisGold else JarvisTextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 13.sp
        )
    }
}
