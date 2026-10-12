package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.JarvisTab
import com.example.ui.JarvisViewModel
import com.example.ui.components.ToolConfirmationDialog
import com.example.ui.screens.ComputerControlScreen
import com.example.ui.screens.CoreChatScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudyModeScreen
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDarkBg
import com.example.ui.theme.JarvisSky
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                JarvisApp()
            }
        }
    }
}

@Composable
fun JarvisApp(viewModel: JarvisViewModel = viewModel()) {
    val context = LocalContext.current
    val activeTab by viewModel.activeTab.collectAsState()
    val pendingTool by viewModel.pendingConfirmation.collectAsState()
    val snackbarMsg by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Audio permission request
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            viewModel.snackbarMessage.value = "Microphone permission is required for voice control."
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Snackbar notification handler
    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.snackbarMessage.value = null
        }
    }

    // Back handling: return to Core tab if on a sub-screen
    BackHandler(enabled = activeTab != JarvisTab.CORE) {
        viewModel.activeTab.value = JarvisTab.CORE
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = JarvisDarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = JarvisSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .border(
                        androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(JarvisSurfaceVariant)
                                .border(1.dp, JarvisCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Core Reactor",
                                tint = JarvisCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "J.A.R.V.I.S.",
                                color = JarvisCyan,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "AI PERSONAL ASSISTANT",
                                color = JarvisTextSecondary,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Telemetry indicator with Real Internet & Live Mode state
                    val isLiveMode by viewModel.isLiveMode.collectAsState()
                    val isOnline by viewModel.isOnline.collectAsState()
                    val lastLatency by viewModel.lastLatencyMs.collectAsState()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                when {
                                    !isOnline -> Color(0xFF2B0C0C)
                                    isLiveMode -> Color(0xFF0C2B16)
                                    else -> Color(0xFF06101E)
                                }
                            )
                            .border(
                                1.dp,
                                when {
                                    !isOnline -> com.example.ui.theme.JarvisRed
                                    isLiveMode -> com.example.ui.theme.JarvisGreen
                                    else -> JarvisBorder
                                },
                                CircleShape
                            )
                            .clickable { viewModel.runNetworkDiagnostics() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        !isOnline -> com.example.ui.theme.JarvisRed
                                        isLiveMode -> com.example.ui.theme.JarvisGreen
                                        else -> JarvisCyan
                                    }
                                )
                        )
                        Text(
                            text = when {
                                !isOnline -> "NO INTERNET"
                                isLiveMode -> "LIVE // ON"
                                lastLatency != null -> "ONLINE // ${lastLatency}ms"
                                else -> "ONLINE"
                            },
                            color = when {
                                !isOnline -> com.example.ui.theme.JarvisRed
                                isLiveMode -> com.example.ui.theme.JarvisGreen
                                else -> JarvisSky
                            },
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = JarvisSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.border(
                    androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder)
                )
            ) {
                JarvisTab.values().forEach { tab ->
                    val isSelected = activeTab == tab
                    val (icon, testTag) = when (tab) {
                        JarvisTab.CORE -> Icons.Default.Bolt to "tab_core"
                        JarvisTab.MEMORY -> Icons.Default.Psychology to "tab_memory"
                        JarvisTab.TOOLS -> Icons.Default.Computer to "tab_tools"
                        JarvisTab.STUDY -> Icons.Default.School to "tab_study"
                        JarvisTab.SETTINGS -> Icons.Default.Settings to "tab_settings"
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.activeTab.value = tab },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF003036),
                            selectedTextColor = JarvisCyan,
                            indicatorColor = JarvisCyan,
                            unselectedIconColor = JarvisTextSecondary,
                            unselectedTextColor = JarvisTextSecondary
                        ),
                        modifier = Modifier.testTag(testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                JarvisTab.CORE -> CoreChatScreen(viewModel = viewModel)
                JarvisTab.MEMORY -> MemoryScreen(viewModel = viewModel)
                JarvisTab.TOOLS -> ComputerControlScreen(viewModel = viewModel)
                JarvisTab.STUDY -> StudyModeScreen(viewModel = viewModel)
                JarvisTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }

            // Security Tool Confirmation Modal Dialog
            pendingTool?.let { toolCall ->
                ToolConfirmationDialog(
                    toolCall = toolCall,
                    onConfirm = { viewModel.confirmPendingTool(approved = true) },
                    onDismiss = { viewModel.confirmPendingTool(approved = false) }
                )
            }
        }
    }
}
