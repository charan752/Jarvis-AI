package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LanguageMode
import com.example.data.model.ResponseStyle
import com.example.ui.haptics.HapticIntensity
import com.example.ui.JarvisViewModel
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDarkBg
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSky
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun SettingsScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val currentLang by viewModel.languageMode.collectAsState()
    val responseStyle by viewModel.responseStyle.collectAsState()
    val modelName by viewModel.selectedModel.collectAsState()
    val customKey by viewModel.customApiKey.collectAsState()
    val isHandsFree by viewModel.isHandsFreeEnabled.collectAsState()
    val isMemoryEnabled by viewModel.isMemoryInjectionEnabled.collectAsState()

    val bgInstagram by viewModel.bgCheckInstagram.collectAsState()
    val bgTelegram by viewModel.bgCheckTelegram.collectAsState()
    val bgWhatsApp by viewModel.bgCheckWhatsApp.collectAsState()
    val bgGmail by viewModel.bgCheckGmail.collectAsState()
    val bgDiscord by viewModel.bgCheckDiscord.collectAsState()

    val privacyUnread by viewModel.privacyUnreadCounts.collectAsState()
    val privacyReadContents by viewModel.privacyReadContents.collectAsState()
    val privacySenderNames by viewModel.privacySenderNames.collectAsState()
    val isSimulatedMode by viewModel.simulatedTestingMode.collectAsState()

    var userTitleInput by remember { mutableStateOf(viewModel.userTitle.value) }
    var apiKeyInput by remember { mutableStateOf(customKey) }
    var voicePitch by remember { mutableFloatStateOf(viewModel.voiceManager.speechPitch) }
    var voiceRate by remember { mutableFloatStateOf(viewModel.voiceManager.speechRate) }
    var isTtsEnabled by remember { mutableStateOf(viewModel.voiceManager.isTtsEnabled) }
    var requireConfirmation by remember { mutableStateOf(viewModel.toolsManager.requireConfirmationForDangerousActions) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. TITLE ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = JarvisCyan,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "JARVIS CONFIGURATION MATRIX",
                    color = JarvisCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Intelligence, Speech, Security & Language Parameters",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // --- 2. GEMINI AI CONFIGURATION ---
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Key, contentDescription = "API", tint = JarvisCyan)
                    Text(
                        text = "GEMINI INTELLIGENCE ENGINE",
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Model Selector
                Text("Select Model Architecture", color = JarvisTextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("gemini-3.5-flash", "gemini-3.1-pro-preview").forEach { m ->
                        val isSelected = modelName == m
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) JarvisCyan else JarvisSurfaceVariant)
                                .border(1.dp, if (isSelected) JarvisCyan else JarvisBorder, RoundedCornerShape(8.dp))
                                .clickable { viewModel.updateSelectedModel(m) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (m.contains("pro")) "Gemini 3.1 Pro" else "Gemini 3.5 Flash",
                                color = if (isSelected) Color(0xFF003036) else JarvisTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // API Key override with auto-persistence
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = {
                        apiKeyInput = it
                        viewModel.updateCustomApiKey(it)
                    },
                    label = { Text("Gemini API Key (Leave empty to use Secrets)") },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisBorder,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    )
                )
                Text(
                    text = "Configured via AI Studio Secrets Panel or saved persistently here.",
                    color = JarvisTextMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // --- 2B. NETWORK & INTERNET TELEMETRY ---
        val isOnline by viewModel.isOnline.collectAsState()
        val connType by viewModel.connectionType.collectAsState()
        val latencyMs by viewModel.lastLatencyMs.collectAsState()

        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isOnline) JarvisBorder else com.example.ui.theme.JarvisRed
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Wifi, contentDescription = "Network", tint = if (isOnline) JarvisCyan else com.example.ui.theme.JarvisRed)
                    Text(
                        text = "INTERNET & NETWORK STATUS",
                        color = if (isOnline) JarvisCyan else com.example.ui.theme.JarvisRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isOnline) "Status: Online (Active)" else "Status: Disconnected",
                            color = if (isOnline) com.example.ui.theme.JarvisGreen else com.example.ui.theme.JarvisRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Transport: $connType ${latencyMs?.let { "• Latency: ${it}ms" } ?: ""}",
                            color = JarvisTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = { viewModel.runNetworkDiagnostics() },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Probe Net", color = JarvisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- 3. LANGUAGE MATRIX (ENGLISH, TELUGU, TANGLISH) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Language, contentDescription = "Language", tint = JarvisCyan)
                    Text(
                        text = "PRIMARY ASSISTANT LANGUAGE",
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LanguageMode.values().forEach { mode ->
                    val isSelected = currentLang == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) JarvisSurfaceVariant else Color.Transparent)
                            .border(1.dp, if (isSelected) JarvisCyan else JarvisBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.setLanguage(mode) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = mode.displayName,
                            color = if (isSelected) JarvisCyan else JarvisTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Text("ACTIVE", color = JarvisCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- 4. RESPONSE PERSONALITY & STYLE ---
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "RESPONSE STYLE & LENGTH",
                    color = JarvisSky,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                ResponseStyle.values().forEach { style ->
                    val isSelected = responseStyle == style
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) JarvisSurfaceVariant else Color.Transparent)
                            .border(1.dp, if (isSelected) JarvisSky else JarvisBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.responseStyle.value = style }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = style.displayName,
                                color = if (isSelected) JarvisSky else JarvisTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = style.description,
                                color = JarvisTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // --- 5. VOICE ASSISTANT PARAMETERS ---
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = "Voice", tint = JarvisCyan)
                    Text(
                        text = "VOICE ENGINE & SYNTHESIS",
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // TTS Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Text-to-Speech Vocalization", color = JarvisTextPrimary, fontSize = 13.sp)
                    Switch(
                        checked = isTtsEnabled,
                        onCheckedChange = {
                            isTtsEnabled = it
                            viewModel.voiceManager.isTtsEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan)
                    )
                }

                // Live Mode Toggle (Runs continuously until 'stop listening')
                val isLiveMode by viewModel.isLiveMode.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Live Mode (Continuous Background Dialogue)", color = JarvisCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Runs continuously until you say 'Stop listening'", color = JarvisGreen, fontSize = 10.sp)
                    }
                    Switch(
                        checked = isLiveMode,
                        onCheckedChange = { viewModel.toggleLiveMode(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = JarvisGreen, checkedTrackColor = Color(0xFF0F361F))
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Hands-Free Loop Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hands-Free Conversational Loop", color = JarvisTextPrimary, fontSize = 13.sp)
                        Text("Auto-listens after speaking finishes", color = JarvisTextMuted, fontSize = 10.sp)
                    }
                    Switch(
                        checked = isHandsFree,
                        onCheckedChange = { viewModel.isHandsFreeEnabled.value = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan)
                    )
                }

                // Voice Presets & Testing
                val voiceWarning by viewModel.voiceManager.languageWarning.collectAsState()
                val availableVoices by viewModel.voiceManager.availableVoices.collectAsState()
                val activeVoiceName by viewModel.voiceManager.activeVoiceName.collectAsState()
                val selectedVoice by viewModel.voiceManager.selectedVoice.collectAsState()

                if (voiceWarning != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF332005))
                            .border(1.dp, JarvisGold, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "⚠️ ${voiceWarning}",
                            color = JarvisGold,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Voice Selection
                Text("Installed Voice Engine", color = JarvisTextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(JarvisSurfaceVariant)
                        .border(1.dp, JarvisBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Active: $activeVoiceName",
                        color = JarvisCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (availableVoices.size > 1) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Select Voice:", color = JarvisTextMuted, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableVoices.take(3).forEachIndexed { idx, voice ->
                            val isChosen = selectedVoice?.name == voice.name
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isChosen) JarvisCyan.copy(alpha = 0.2f) else JarvisSurfaceVariant)
                                    .border(1.dp, if (isChosen) JarvisCyan else JarvisBorder, RoundedCornerShape(6.dp))
                                    .clickable { viewModel.voiceManager.selectVoice(voice) }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Voice ${idx + 1}",
                                    color = if (isChosen) JarvisCyan else JarvisTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Voice Profiles", color = JarvisTextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(JarvisSurfaceVariant)
                            .border(1.dp, JarvisCyan, RoundedCornerShape(8.dp))
                            .clickable {
                                voicePitch = 0.86f
                                voiceRate = 0.98f
                                viewModel.voiceManager.speechPitch = 0.86f
                                viewModel.voiceManager.speechRate = 0.98f
                                viewModel.voiceManager.speak("Systems nominal, Sir. The real JARVIS profile is active.")
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("British JARVIS", color = JarvisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(JarvisSurfaceVariant)
                            .border(1.dp, JarvisBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                voicePitch = 1.15f
                                voiceRate = 1.05f
                                viewModel.voiceManager.speechPitch = 1.15f
                                viewModel.voiceManager.speechRate = 1.05f
                                viewModel.voiceManager.speak("F.R.I.D.A.Y. interface activated and online.")
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("FRIDAY AI", color = JarvisSky, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(JarvisSurfaceVariant)
                            .border(1.dp, JarvisBorder, RoundedCornerShape(8.dp))
                            .testTag("test_voice_button")
                            .clickable {
                                viewModel.voiceManager.testVoice()
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔊 Test Voice", color = JarvisTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pitch Slider
                Text("Voice Pitch: ${String.format("%.2f", voicePitch)}", color = JarvisTextSecondary, fontSize = 11.sp)
                Slider(
                    value = voicePitch,
                    onValueChange = {
                        voicePitch = it
                        viewModel.voiceManager.speechPitch = it
                    },
                    valueRange = 0.6f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = JarvisCyan, activeTrackColor = JarvisCyan)
                )

                // Rate Slider
                Text("Voice Speed: ${String.format("%.2f", voiceRate)}x", color = JarvisTextSecondary, fontSize = 11.sp)
                Slider(
                    value = voiceRate,
                    onValueChange = {
                        voiceRate = it
                        viewModel.voiceManager.speechRate = it
                    },
                    valueRange = 0.7f..1.6f,
                    colors = SliderDefaults.colors(thumbColor = JarvisCyan, activeTrackColor = JarvisCyan)
                )
            }
        }

        // --- 5B. VOICE ENGINE DIAGNOSTICS & TELEMETRY ---
        val voiceDiag by viewModel.voiceDiagnostics.collectAsState()
        val voiceMetrics by viewModel.voiceLatencyMetrics.collectAsState()

        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("voice_diagnostics_card")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = "Voice Diagnostics", tint = JarvisCyan)
                    Text(
                        text = "VOICE ENGINE DIAGNOSTICS & TELEMETRY",
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Engine & Locale specs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Recognition Engine:", color = JarvisTextSecondary, fontSize = 11.sp)
                    Text(
                        text = voiceDiag.selectedEnginePackage,
                        color = if (voiceDiag.isEngineAvailable) JarvisCyan else JarvisGold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Requested Locale:", color = JarvisTextSecondary, fontSize = 11.sp)
                    Text(
                        text = "${voiceDiag.requestedLocale} (${if (voiceDiag.requestedLocale.startsWith("en")) "English India" else "Telugu India"})",
                        color = JarvisTextPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Engine Status:", color = JarvisTextSecondary, fontSize = 11.sp)
                    Text(
                        text = voiceDiag.actualRecognitionStatus,
                        color = if (voiceDiag.lastErrorDetails != null) JarvisRed else JarvisGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Confidence Score:", color = JarvisTextSecondary, fontSize = 11.sp)
                    Text(
                        text = String.format("%.2f", voiceDiag.lastConfidenceScore),
                        color = JarvisSky,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (voiceDiag.lastRawTranscript.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(JarvisSurfaceVariant)
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("Last Raw Transcript:", color = JarvisTextMuted, fontSize = 9.sp)
                            Text(
                                text = "\"${voiceDiag.lastRawTranscript}\"",
                                color = JarvisTextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (voiceDiag.lastNormalizedTranscript.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Normalized Command:", color = JarvisTextMuted, fontSize = 9.sp)
                                Text(
                                    text = "\"${voiceDiag.lastNormalizedTranscript}\"",
                                    color = JarvisCyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                if (voiceDiag.lastErrorDetails != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF331111))
                            .border(1.dp, Color(0xFF882222), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Error: ${voiceDiag.lastErrorDetails}",
                            color = Color(0xFFFF8888),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Latency Telemetry", color = JarvisTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(JarvisSurfaceVariant)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Intent", color = JarvisTextMuted, fontSize = 9.sp)
                            Text("${voiceMetrics.intentClassificationMs}ms", color = JarvisCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(JarvisSurfaceVariant)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Execution", color = JarvisTextMuted, fontSize = 9.sp)
                            Text("${voiceMetrics.executionMs}ms", color = JarvisSky, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(JarvisSurfaceVariant)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total", color = JarvisTextMuted, fontSize = 9.sp)
                            Text("${voiceMetrics.totalLatencyMs}ms", color = JarvisGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- 6. SECURITY & PERMISSIONS ---
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = "Security", tint = JarvisGold)
                    Text(
                        text = "SAFETY & PERMISSION PROTOCOLS",
                        color = JarvisGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Require Approval for Consequential Actions", color = JarvisTextPrimary, fontSize = 13.sp)
                        Text("Prompts user before launching OS scripts or locking PC", color = JarvisTextMuted, fontSize = 10.sp)
                    }
                    Switch(
                        checked = requireConfirmation,
                        onCheckedChange = {
                            requireConfirmation = it
                            viewModel.toolsManager.requireConfirmationForDangerousActions = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = JarvisGold)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Haptic Feedback Intensity Selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.TouchApp, contentDescription = "Haptics", tint = JarvisCyan, modifier = Modifier.size(18.dp))
                    Text(
                        text = "HAPTIC FEEDBACK",
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Tactile vibration intensity on AI orb state transitions & interactions:",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                val currentHaptic by viewModel.hapticIntensity.collectAsState()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HapticIntensity.values().forEach { intensity ->
                        val isSelected = currentHaptic == intensity
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) JarvisCyan else JarvisSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) JarvisCyan else JarvisBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.setHapticIntensity(intensity) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = intensity.label,
                                color = if (isSelected) Color(0xFF003036) else JarvisTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Level: ${currentHaptic.description}",
                    color = if (currentHaptic == HapticIntensity.OFF) JarvisTextMuted else JarvisSky,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // --- BACKGROUND CHECKS ---
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("card_background_checks")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = "Background Checks", tint = JarvisCyan, modifier = Modifier.size(18.dp))
                    Text(
                        text = "BACKGROUND CHECKS",
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Authorize services checked automatically upon HOME_ARRIVAL detection:",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Overall service connection summary
                val connectedCount = listOf(bgInstagram, bgTelegram, bgWhatsApp, bgGmail, bgDiscord).count { it }
                val totalServices = 5
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(JarvisDarkBg.copy(alpha = 0.6f))
                        .border(1.dp, JarvisBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("summary_connection_status")
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
                                    .background(if (connectedCount > 0) JarvisGreen else JarvisTextMuted, CircleShape)
                            )
                            Text(
                                text = "MONITORING STATUS",
                                color = JarvisTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "$connectedCount / $totalServices CONNECTED",
                            color = if (connectedCount > 0) JarvisGreen else JarvisRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 1. Instagram
                MessagingServiceSettingRow(
                    title = "Instagram",
                    isAuthorized = bgInstagram,
                    onAuthorizedChange = { viewModel.setBgCheckInstagram(it) },
                    connectedSubtitle = "Permission Granted // Direct Inbox Unread Monitor Active",
                    disconnectedSubtitle = "Permission Denied // Direct Inbox Checks Disabled",
                    statusIndicatorTag = "connection_status_instagram",
                    switchTag = "switch_instagram"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Telegram
                MessagingServiceSettingRow(
                    title = "Telegram",
                    isAuthorized = bgTelegram,
                    onAuthorizedChange = { viewModel.setBgCheckTelegram(it) },
                    connectedSubtitle = "Permission Granted // Cloud Chat Unread Monitor Active",
                    disconnectedSubtitle = "Permission Denied // Cloud Chat Checks Disabled",
                    statusIndicatorTag = "connection_status_telegram",
                    switchTag = "switch_telegram"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3. WhatsApp
                MessagingServiceSettingRow(
                    title = "WhatsApp",
                    isAuthorized = bgWhatsApp,
                    onAuthorizedChange = { viewModel.setBgCheckWhatsApp(it) },
                    connectedSubtitle = "Permission Granted // Background Monitor Standby",
                    disconnectedSubtitle = "Permission Denied // Standby (Future Integration)",
                    statusIndicatorTag = "connection_status_whatsapp",
                    switchTag = "switch_whatsapp"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Gmail
                MessagingServiceSettingRow(
                    title = "Gmail",
                    isAuthorized = bgGmail,
                    onAuthorizedChange = { viewModel.setBgCheckGmail(it) },
                    connectedSubtitle = "Permission Granted // Mail API Monitor Standby",
                    disconnectedSubtitle = "Permission Denied // Standby (Future Integration)",
                    statusIndicatorTag = "connection_status_gmail",
                    switchTag = "switch_gmail"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 5. Discord
                MessagingServiceSettingRow(
                    title = "Discord",
                    isAuthorized = bgDiscord,
                    onAuthorizedChange = { viewModel.setBgCheckDiscord(it) },
                    connectedSubtitle = "Permission Granted // Channel Monitor Standby",
                    disconnectedSubtitle = "Permission Denied // Standby (Future Integration)",
                    statusIndicatorTag = "connection_status_discord",
                    switchTag = "switch_discord"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Simulated Protocol Testing toggle (Clearly labeled as DEMO)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSimulatedMode) JarvisGold.copy(alpha = 0.12f) else JarvisDarkBg.copy(alpha = 0.4f))
                        .border(1.dp, if (isSimulatedMode) JarvisGold.copy(alpha = 0.5f) else JarvisBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "SIMULATED PROTOCOL TESTING",
                                color = if (isSimulatedMode) JarvisGold else JarvisTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSimulatedMode) JarvisGold.copy(alpha = 0.2f) else JarvisTextMuted.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (isSimulatedMode) "DEMO ACTIVE" else "PRODUCTION",
                                    color = if (isSimulatedMode) JarvisGold else JarvisTextMuted,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isSimulatedMode) "Injects test arrival numbers. All data strictly labeled [SIMULATED]." else "Production Mode. Never fabricates message counts.",
                            color = if (isSimulatedMode) JarvisGold.copy(alpha = 0.85f) else JarvisTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = isSimulatedMode,
                        onCheckedChange = { viewModel.setSimulatedTestingMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JarvisGold,
                            checkedTrackColor = JarvisGold.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("switch_simulated_testing")
                    )
                }
            }
        }

        // --- MESSAGE PRIVACY ---
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("card_message_privacy")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = "Message Privacy", tint = JarvisGold, modifier = Modifier.size(18.dp))
                    Text(
                        text = "MESSAGE PRIVACY",
                        color = JarvisGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure data access boundaries for messaging integrations:",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Unread counts [ON]
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Unread counts", color = JarvisTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Check only quantitative notification numbers", color = JarvisTextMuted, fontSize = 10.sp)
                    }
                    Switch(
                        checked = privacyUnread,
                        onCheckedChange = { viewModel.setPrivacyUnreadCounts(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = JarvisGreen, checkedTrackColor = JarvisGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("switch_unread_counts")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Read message contents [OFF]
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Read message contents", color = JarvisTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Access full private body text (Requires explicit permission)", color = JarvisTextMuted, fontSize = 10.sp)
                    }
                    Switch(
                        checked = privacyReadContents,
                        onCheckedChange = { viewModel.setPrivacyReadContents(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = JarvisGold, checkedTrackColor = JarvisGold.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("switch_read_contents")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Sender names [OFF]
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sender names", color = JarvisTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Retrieve identity of message senders during summary", color = JarvisTextMuted, fontSize = 10.sp)
                    }
                    Switch(
                        checked = privacySenderNames,
                        onCheckedChange = { viewModel.setPrivacySenderNames(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = JarvisGold, checkedTrackColor = JarvisGold.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("switch_sender_names")
                    )
                }
            }
        }

        // --- 7. ROOM DATABASE PERSISTENCE TELEMETRY & CONTROLS ---
        val messagesCount by viewModel.messageCount.collectAsState()
        val notesList by viewModel.notes.collectAsState()
        val memoriesList by viewModel.memories.collectAsState()
        var showSettingsClearDialog by remember { mutableStateOf(false) }

        if (showSettingsClearDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsClearDialog = false },
                title = {
                    Text(
                        text = "Purge Room Chat History?",
                        color = JarvisTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                },
                text = {
                    Text(
                        text = "This will erase all $messagesCount conversation records from the on-device Room SQLite database (jarvis_core_db). Notes and personal memories will be preserved.",
                        color = JarvisTextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearConversation()
                            showSettingsClearDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisRed)
                    ) {
                        Text("Purge History", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showSettingsClearDialog = false }) {
                        Text("Cancel", color = JarvisCyan)
                    }
                },
                containerColor = JarvisSurface,
                shape = RoundedCornerShape(12.dp)
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Storage, contentDescription = "Room DB", tint = JarvisCyan, modifier = Modifier.size(18.dp))
                    Text(
                        text = "LOCAL ROOM DATABASE // SQLITE STORAGE",
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "• Conversation History: $messagesCount persistent messages\n" +
                            "• User Facts & Memories: ${memoriesList.size} stored entities\n" +
                            "• User Notes: ${notesList.size} stored documents\n" +
                            "• Architecture: Android Jetpack Room with SQLite (jarvis_core_db)",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 18.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showSettingsClearDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381018)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear", tint = JarvisRed)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Clear Conversation History (Room)", color = JarvisRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Visual indicator badge displaying "Connected" (green) or "Disconnected" (red)
 * based on whether background check permissions have been granted.
 */
@Composable
fun ServiceConnectionStatusBadge(
    isConnected: Boolean,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val statusColor = if (isConnected) JarvisGreen else JarvisRed
    val containerColor = if (isConnected) JarvisGreen.copy(alpha = 0.15f) else JarvisRed.copy(alpha = 0.12f)
    val borderColor = if (isConnected) JarvisGreen.copy(alpha = 0.65f) else JarvisRed.copy(alpha = 0.45f)
    val statusText = if (isConnected) "Connected" else "Disconnected"

    Box(
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(statusColor, CircleShape)
            )
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Setting row for an authorized messaging service displaying the service name,
 * dynamic Connected/Disconnected visual indicator, description subtitle, and permission toggle switch.
 */
@Composable
fun MessagingServiceSettingRow(
    title: String,
    isAuthorized: Boolean,
    onAuthorizedChange: (Boolean) -> Unit,
    connectedSubtitle: String,
    disconnectedSubtitle: String,
    statusIndicatorTag: String,
    switchTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(JarvisDarkBg.copy(alpha = 0.35f))
            .border(1.dp, if (isAuthorized) JarvisGreen.copy(alpha = 0.25f) else JarvisBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    color = JarvisTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                ServiceConnectionStatusBadge(
                    isConnected = isAuthorized,
                    testTag = statusIndicatorTag
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (isAuthorized) connectedSubtitle else disconnectedSubtitle,
                color = if (isAuthorized) JarvisGreen.copy(alpha = 0.85f) else JarvisTextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Switch(
            checked = isAuthorized,
            onCheckedChange = onAuthorizedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (isAuthorized) JarvisGreen else JarvisCyan,
                checkedTrackColor = (if (isAuthorized) JarvisGreen else JarvisCyan).copy(alpha = 0.5f)
            ),
            modifier = Modifier.testTag(switchTag)
        )
    }
}
