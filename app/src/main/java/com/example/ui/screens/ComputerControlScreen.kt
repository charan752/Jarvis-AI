package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JarvisViewModel
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComputerControlScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isDemoMode by remember { mutableStateOf(viewModel.toolsManager.isDemoMode) }
    var bridgeUrl by remember { mutableStateOf(viewModel.toolsManager.companionBridgeUrl) }
    var showCompanionScript by remember { mutableStateOf(false) }

    // Log of dispatched actions
    val actionLogs = remember {
        mutableStateListOf(
            "[SYSTEM] Computer Control Architecture initialized in Simulation Mode.",
            "[SECURITY] Shell commands disabled. Strict app allowlist enforced."
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. TITLE & SUBTITLE ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Computer,
                contentDescription = "Computer Tools",
                tint = JarvisCyan,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "COMPUTER TOOLS // BRIDGE",
                    color = JarvisCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Decoupled Architecture: Phone Client ➔ PC Companion Daemon",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // --- 2. ARCHITECTURE EXPLANATION CARD ---
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "THREE-TIER DECOUPLED ARCHITECTURE",
                    color = JarvisSky,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "A. Intelligence Layer: Gemini Multimodal Core\nB. Controller Interface: JARVIS Android Application\nC. Execution Agent: Local Python Companion on Desktop",
                    color = JarvisTextPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                )
            }
        }

        // --- 3. DEMO MODE / SIMULATION STATUS ---
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isDemoMode) Color(0xFF131D12) else JarvisSurface
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDemoMode) JarvisGreen else JarvisBorder
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isDemoMode) "DEMO MODE (SIMULATED)" else "LIVE COMPANION BRIDGE",
                            color = if (isDemoMode) JarvisGreen else JarvisCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = if (isDemoMode) "Simulates computer automation safely without requiring PC daemon" else "Commands sent to desktop WebSocket/HTTP agent",
                        color = JarvisTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Switch(
                    checked = isDemoMode,
                    onCheckedChange = {
                        isDemoMode = it
                        viewModel.toolsManager.isDemoMode = it
                        actionLogs.add(0, "[CONFIG] Simulation Demo Mode toggled to: $it")
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = JarvisGreen,
                        checkedTrackColor = Color(0xFF1E381A)
                    ),
                    modifier = Modifier.testTag("demo_mode_switch")
                )
            }
        }

        // --- 4. COMPANION PYTHON SCRIPT GENERATOR ---
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant),
            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Code, contentDescription = "Code", tint = JarvisCyan)
                        Text(
                            text = "DESKTOP COMPANION DAEMON",
                            color = JarvisCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    OutlinedButton(
                        onClick = { showCompanionScript = !showCompanionScript },
                        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCyan)
                    ) {
                        Text(
                            text = if (showCompanionScript) "Hide Code" else "View Python Script",
                            color = JarvisCyan,
                            fontSize = 11.sp
                        )
                    }
                }

                AnimatedVisibility(visible = showCompanionScript) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            text = "Save as 'jarvis_companion.py' on your Windows/Mac/Linux PC:",
                            color = JarvisTextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF04070D))
                                .border(1.dp, Color(0xFF1A2B45), RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = viewModel.toolsManager.generateCompanionPythonScript(),
                                color = JarvisTextPrimary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText("JARVIS Companion Script", viewModel.toolsManager.generateCompanionPythonScript())
                                )
                                Toast.makeText(context, "Python companion script copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF003036))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Copy jarvis_companion.py to Clipboard", color = Color(0xFF003036), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // --- 5. APPROVED ACTIONS CONTROL GRID ---
        Text(
            text = "APPROVED COMPUTER ACTION SUITE",
            color = JarvisSky,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2
        ) {
            val actions = listOf(
                Triple("Launch Chrome", "open_app", "chrome"),
                Triple("Launch VS Code", "open_app", "vscode"),
                Triple("Launch Spotify", "open_app", "spotify"),
                Triple("Desktop Screenshot", "take_screenshot", "primary_display"),
                Triple("Fetch Hardware Specs", "system_stats", "full"),
                Triple("Lock Workstation", "lock_pc", "windows_session")
            )

            actions.forEach { (label, actionType, target) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = JarvisSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            coroutineScope.launch {
                                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                                val (_, msg) = viewModel.toolsManager.executeComputerAction(actionType, target)
                                actionLogs.add(0, "[$timeStr] $msg")
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when (actionType) {
                                "take_screenshot" -> Icons.Default.PhotoCamera
                                "system_stats" -> Icons.Default.Speed
                                "lock_pc" -> Icons.Default.Lock
                                else -> Icons.Default.OpenInBrowser
                            },
                            contentDescription = label,
                            tint = if (actionType == "lock_pc") JarvisGold else JarvisCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = label,
                            color = JarvisTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // --- 6. LIVE CONSOLE OUTPUT LOG ---
        Text(
            text = "COMPANION TELEMETRY LOG",
            color = JarvisSky,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF03060C))
                .border(1.dp, JarvisBorder, RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                actionLogs.forEach { log ->
                    Text(
                        text = log,
                        color = if (log.contains("SIMULATION")) JarvisGold else if (log.contains("SYSTEM")) JarvisCyan else JarvisTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
