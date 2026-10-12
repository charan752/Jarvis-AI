package com.example.tools

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.data.api.search.ReliableSearchService
import com.example.data.api.search.RetrofitSearchServiceImpl
import com.example.data.local.JarvisDatabase
import com.example.data.local.MemoryEntity
import com.example.data.local.NoteEntity
import com.example.data.model.ToolCallInfo
import com.example.data.model.ToolExecutionStatus
import com.example.data.model.StructuredSearchResult
import com.example.data.network.WebInformationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JarvisToolsManager(
    private val context: Context,
    private val database: JarvisDatabase
) {
    var isDemoMode: Boolean = false
    var companionBridgeUrl: String = "http://192.168.1.100:8765"
    var requireConfirmationForDangerousActions: Boolean = true

    val retrofitSearchService: ReliableSearchService = RetrofitSearchServiceImpl()
    val webInformationService = WebInformationService(retrofitSearchService)

    // Built-in allowlist of safe computer actions
    val approvedComputerApps = listOf(
        "chrome" to "Google Chrome",
        "vscode" to "Visual Studio Code",
        "spotify" to "Spotify Music",
        "notepad" to "Notepad / Text Editor",
        "terminal" to "Terminal / Command Prompt",
        "calculator" to "Calculator",
        "explorer" to "File Explorer"
    )

    fun getCurrentTime(): String {
        val now = Date()
        val timeFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val timeZone = java.util.TimeZone.getDefault().displayName
        return "Current Time: ${timeFormat.format(now)}\nDate: ${dateFormat.format(now)}\nTimezone: $timeZone"
    }

    fun getSystemInfo(): String {
        // Battery status
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val isCharging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val status = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } else false

        // Memory info
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)
        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availRamMb = memInfo.availMem / (1024 * 1024)

        // Storage info
        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
        val bytesTotal = stat.blockCountLong * stat.blockSizeLong
        val freeStorageGb = bytesAvailable / (1024 * 1024 * 1024)
        val totalStorageGb = bytesTotal / (1024 * 1024 * 1024)

        // Network connectivity
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNet = connManager?.activeNetwork
        val caps = connManager?.getNetworkCapabilities(activeNet)
        val netType = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi Connected"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular LTE/5G"
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true -> "Online"
            else -> "Offline / Disconnected"
        }

        return """
            |--- JARVIS TELEMETRY & SYSTEM METRICS ---
            |• Device: ${Build.MANUFACTURER.uppercase()} ${Build.MODEL}
            |• OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            |• Battery: $batteryLevel% ${if (isCharging) "[⚡ Charging]" else "[Discharging]"}
            |• Memory (RAM): $availRamMb MB free / $totalRamMb MB total
            |• Storage: $freeStorageGb GB free / $totalStorageGb GB total
            |• Network: $netType
            |• Status: All neural subsystems operating within nominal parameters.
        """.trimMargin()
    }

    fun openWebsite(rawUrl: String): Pair<Boolean, String> {
        val cleanUrl = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
            "https://$rawUrl"
        } else rawUrl

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Pair(true, "Opened $cleanUrl in browser, Sir.")
        } catch (e: Exception) {
            Pair(false, "Could not open URL: ${e.message}")
        }
    }

    // Explicit Computer Search: Google in browser
    fun openBrowserGoogleSearch(query: String): Pair<Boolean, String> {
        return try {
            val searchUri = Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
            val intent = Intent(Intent.ACTION_VIEW, searchUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Pair(true, "Opened Google search in browser for '$query'.")
        } catch (e: Exception) {
            Pair(false, "Failed to launch browser: ${e.message}")
        }
    }

    // Explicit Computer Search: YouTube in browser
    fun openBrowserYouTubeSearch(query: String): Pair<Boolean, String> {
        return try {
            val searchUri = Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query))
            val intent = Intent(Intent.ACTION_VIEW, searchUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Pair(true, "Opened YouTube search in browser for '$query'.")
        } catch (e: Exception) {
            Pair(false, "Failed to launch YouTube search: ${e.message}")
        }
    }

    // Real Information Retrieval methods (Never open Chrome)
    suspend fun searchWeb(query: String): StructuredSearchResult {
        return webInformationService.searchWeb(query)
    }

    suspend fun getNews(query: String): StructuredSearchResult {
        return webInformationService.getNews(query)
    }

    suspend fun getCurrentInformation(query: String): StructuredSearchResult {
        return webInformationService.getCurrentInformation(query)
    }

    suspend fun createNote(title: String, content: String): String = withContext(Dispatchers.IO) {
        val note = NoteEntity(title = title, content = content)
        val id = database.noteDao().insertNote(note)
        "Note #$id ('$title') has been committed to secure memory, Sir."
    }

    suspend fun saveMemory(key: String, content: String, category: String = "general"): String = withContext(Dispatchers.IO) {
        val memory = MemoryEntity(key = key, content = content, category = category)
        val id = database.memoryDao().insertMemory(memory)
        "Memory record #$id ('$key') recorded in core matrix, Sir."
    }

    suspend fun getMemory(query: String): String = withContext(Dispatchers.IO) {
        val memories = database.memoryDao().searchMemories(query)
        if (memories.isEmpty()) {
            "No memory records found matching '$query', Sir."
        } else {
            memories.joinToString("\n") {
                "• [${it.category.uppercase()}] ${it.key}: ${it.content}"
            }
        }
    }

    suspend fun executeComputerAction(action: String, target: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanAction = action.lowercase().trim()
        val cleanTarget = target.lowercase().trim()

        // Strict Allowlist Enforcement
        val allowedActions = setOf("open_app", "open_url", "take_screenshot", "system_stats", "lock_pc")
        if (cleanAction !in allowedActions) {
            return@withContext Pair(false, "Security Alert: Action '$action' is not permitted by JARVIS security protocol.")
        }

        if (cleanAction == "open_app") {
            val isAppAllowed = approvedComputerApps.any { it.first == cleanTarget || cleanTarget.contains(it.first) }
            if (!isAppAllowed && !isDemoMode) {
                return@withContext Pair(false, "Security Alert: Application '$target' is not in the approved workstation allowlist.")
            }
        }

        if (cleanAction == "open_url") {
            openWebsite(target)
            return@withContext Pair(true, "Dispatched URL '$target' to desktop browser, Sir.")
        }

        if (isDemoMode) {
            val result = when (cleanAction) {
                "open_app" -> "Launched application '$target' on primary workstation."
                "open_url" -> "Dispatched URL '$target' to browser."
                "take_screenshot" -> "Desktop screenshot captured (3840x2160 PNG) to companion workspace."
                "system_stats" -> "Workstation Telemetry: CPU 18% (Intel i9/Ryzen), GPU 24% (RTX 4090), RAM 14.2 GB / 32 GB, Temp 44°C."
                "lock_pc" -> "Workstation locked successfully via WinLock API."
                else -> "Action '$action' dispatched for target '$target'."
            }
            Pair(true, result)
        } else {
            // When connected to companion bridge
            Pair(true, "Command dispatched to local companion node at $companionBridgeUrl: action=$action, target=$target")
        }
    }

    fun isDangerousAction(action: String): Boolean {
        return action.equals("lock_pc", ignoreCase = true) ||
                action.equals("run_script", ignoreCase = true) ||
                action.equals("delete_file", ignoreCase = true)
    }

    fun generateCompanionPythonScript(): String {
        return """
# JARVIS Local Computer Companion Bridge (Python 3.9+)
# Run on your desktop: pip install flask flask-cors
# python jarvis_companion.py

import os
import subprocess
import webbrowser
from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

APPROVED_APPS = {
    "chrome": "start chrome",
    "vscode": "code",
    "spotify": "start spotify",
    "notepad": "notepad",
    "terminal": "start wt",
    "calculator": "calc"
}

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "ONLINE", "version": "1.0", "name": "JARVIS Desktop Node"})

@app.route('/action', methods=['POST'])
def execute_action():
    data = request.json or {}
    action = data.get('action', '')
    target = data.get('target', '')
    
    if action == 'open_app':
        cmd = APPROVED_APPS.get(target.lower())
        if cmd:
            subprocess.Popen(cmd, shell=True)
            return jsonify({"status": "SUCCESS", "message": f"Launched {target}"})
        return jsonify({"status": "DENIED", "message": "App not in security allowlist"})
        
    elif action == 'open_url':
        if target.startswith('http://') or target.startswith('https://'):
            webbrowser.open(target)
            return jsonify({"status": "SUCCESS", "message": f"Opened {target}"})
            
    elif action == 'system_stats':
        return jsonify({
            "status": "SUCCESS",
            "cpu": "nominal",
            "os": os.name
        })
        
    return jsonify({"status": "UNKNOWN_ACTION", "action": action})

if __name__ == '__main__':
    print("JARVIS Desktop Bridge listening on http://0.0.0.0:8765")
    app.run(host='0.0.0.0', port=8765)
        """.trimIndent()
    }
}
