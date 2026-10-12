package com.example.data.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.os.UserManager
import android.util.Log

/**
 * Platform-specific Android implementation of [AppLauncher].
 * Enforces the strict JARVIS App Launching Rule:
 *
 * Priority:
 * 1. Installed native app
 * 2. Installed app inside Secure Folder, when accessible
 * 3. Official website only if the native app is unavailable (with explicit user confirmation prompt)
 *
 * SECURE FOLDER:
 * - Treats Secure Folder as a separate protected Android profile
 * - Does not attempt to bypass Secure Folder authentication or access controls
 * - If locked or directly unlaunchable, tells user:
 *   "[App] is available in Secure Folder, but I can't launch that protected instance directly. Please unlock Secure Folder."
 *
 * APP ALLOWLIST:
 * - Strictly maintains an allowlist of permitted applications
 * - Rejects arbitrary intents, shell commands, or unauthorized package names
 */
class AndroidAppLauncher(
    private val context: Context
) : AppLauncher {

    companion object {
        private const val TAG = "AndroidAppLauncher"

        val ALLOWLIST = listOf(
            AllowedApp(
                id = "instagram",
                displayName = "Instagram",
                packageName = "com.instagram.android",
                officialWebsiteUrl = "https://instagram.com",
                aliases = listOf("insta")
            ),
            AllowedApp(
                id = "telegram",
                displayName = "Telegram",
                packageName = "org.telegram.messenger",
                officialWebsiteUrl = "https://web.telegram.org",
                aliases = listOf("tg")
            ),
            AllowedApp(
                id = "youtube",
                displayName = "YouTube",
                packageName = "com.google.android.youtube",
                officialWebsiteUrl = "https://youtube.com",
                aliases = listOf("yt")
            ),
            AllowedApp(
                id = "chrome",
                displayName = "Google Chrome",
                packageName = "com.android.chrome",
                officialWebsiteUrl = "https://google.com",
                aliases = listOf("browser", "google chrome")
            ),
            AllowedApp(
                id = "spotify",
                displayName = "Spotify",
                packageName = "com.spotify.music",
                officialWebsiteUrl = "https://open.spotify.com",
                aliases = listOf("music")
            ),
            AllowedApp(
                id = "whatsapp",
                displayName = "WhatsApp",
                packageName = "com.whatsapp",
                officialWebsiteUrl = "https://web.whatsapp.com",
                aliases = listOf("wa")
            ),
            AllowedApp(
                id = "gmail",
                displayName = "Gmail",
                packageName = "com.google.android.gm",
                officialWebsiteUrl = "https://mail.google.com",
                aliases = listOf("email", "mail")
            ),
            AllowedApp(
                id = "discord",
                displayName = "Discord",
                packageName = "com.discord",
                officialWebsiteUrl = "https://discord.com"
            ),
            AllowedApp(
                id = "calculator",
                displayName = "Calculator",
                packageName = "com.google.android.calculator",
                officialWebsiteUrl = "https://www.google.com/search?q=calculator",
                aliases = listOf("calc")
            ),
            AllowedApp(
                id = "notepad",
                displayName = "Notepad",
                packageName = "com.google.android.keep",
                officialWebsiteUrl = "https://keep.google.com",
                aliases = listOf("notes", "keep", "text editor")
            ),
            AllowedApp(
                id = "maps",
                displayName = "Google Maps",
                packageName = "com.google.android.apps.maps",
                officialWebsiteUrl = "https://maps.google.com",
                aliases = listOf("navigation")
            )
        )
    }

    override fun getAllowedApps(): List<AllowedApp> = ALLOWLIST

    override fun getAppMetadata(appId: String): AllowedApp? {
        val clean = appId.lowercase().trim()
        return ALLOWLIST.find { app ->
            app.id == clean ||
                    app.displayName.lowercase() == clean ||
                    app.packageName == clean ||
                    app.aliases.any { it.equals(clean, ignoreCase = true) }
        }
    }

    override fun isAppInstalled(appId: String): Boolean {
        val app = getAppMetadata(appId) ?: return false
        return try {
            val pm = context.packageManager
            pm.getPackageInfo(app.packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            context.packageManager.getLaunchIntentForPackage(app.packageName) != null
        } catch (e: Exception) {
            Log.w(TAG, "Error checking package ${app.packageName}: ${e.message}")
            false
        }
    }

    override fun isAppAvailableInSecureFolder(appId: String): Boolean {
        val app = getAppMetadata(appId) ?: return false
        return try {
            val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            val myUserHandle = Process.myUserHandle()
            val otherProfiles = userManager?.userProfiles?.filter { it != myUserHandle } ?: emptyList()

            // Check if app is installed in any non-primary profile (e.g. Samsung Secure Folder / Work Profile)
            if (launcherApps != null && otherProfiles.isNotEmpty()) {
                otherProfiles.any { profile ->
                    try {
                        launcherApps.isPackageEnabled(app.packageName, profile) ||
                                launcherApps.getActivityList(app.packageName, profile).isNotEmpty()
                    } catch (_: Exception) {
                        false
                    }
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking Secure Folder for ${app.id}: ${e.message}")
            false
        }
    }

    override fun launchApp(appId: String): AppLaunchResult {
        val app = getAppMetadata(appId)
        if (app == null) {
            return AppLaunchResult.NotAllowed(
                appId = appId,
                message = "I am not authorized to launch '$appId', sir."
            )
        }

        // Priority 1: Installed native app
        if (isAppInstalled(app.id)) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null) {
                return try {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    AppLaunchResult.Success(
                        appId = app.id,
                        displayName = app.displayName,
                        message = "Opening ${app.displayName}, Sir."
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start activity for ${app.displayName}", e)
                    AppLaunchResult.Failure(
                        appId = app.id,
                        displayName = app.displayName,
                        errorMessage = "Failed to launch ${app.displayName}: ${e.message}"
                    )
                }
            }
        }

        // Priority 2: Installed app inside Secure Folder, when accessible
        if (isAppAvailableInSecureFolder(app.id)) {
            // Treat Secure Folder as a separate protected Android profile.
            // Do not attempt to bypass Secure Folder authentication, security restrictions, or Samsung's access controls.
            val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            val myUserHandle = Process.myUserHandle()
            val otherProfiles = userManager?.userProfiles?.filter { it != myUserHandle } ?: emptyList()

            for (profile in otherProfiles) {
                // If quiet mode is enabled or profile is locked, it cannot be launched directly
                val isQuiet = userManager?.isQuietModeEnabled(profile) == true
                if (isQuiet) {
                    return AppLaunchResult.SecureFolderLocked(
                        appId = app.id,
                        displayName = app.displayName,
                        message = "${app.displayName} is available in Secure Folder, but I can't launch that protected instance directly. Please unlock Secure Folder."
                    )
                }

                if (launcherApps != null) {
                    val activities = launcherApps.getActivityList(app.packageName, profile)
                    if (activities.isNotEmpty()) {
                        return try {
                            launcherApps.startMainActivity(activities.first().componentName, profile, null, null)
                            AppLaunchResult.Success(
                                appId = app.id,
                                displayName = app.displayName,
                                message = "Opening ${app.displayName} in Secure Folder, Sir.",
                                isSecureFolder = true
                            )
                        } catch (e: SecurityException) {
                            Log.w(TAG, "SecurityException launching Secure Folder instance", e)
                            AppLaunchResult.SecureFolderLocked(
                                appId = app.id,
                                displayName = app.displayName,
                                message = "${app.displayName} is available in Secure Folder, but I can't launch that protected instance directly. Please unlock Secure Folder."
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error launching Secure Folder instance", e)
                            AppLaunchResult.SecureFolderLocked(
                                appId = app.id,
                                displayName = app.displayName,
                                message = "${app.displayName} is available in Secure Folder, but I can't launch that protected instance directly. Please unlock Secure Folder."
                            )
                        }
                    }
                }
            }

            return AppLaunchResult.SecureFolderLocked(
                appId = app.id,
                displayName = app.displayName,
                message = "${app.displayName} is available in Secure Folder, but I can't launch that protected instance directly. Please unlock Secure Folder."
            )
        }

        // Priority 3: Native app is unavailable.
        // Rule: NEVER automatically open the website when a native app is available.
        // And when native app is NOT installed:
        // Say: "[App] isn't installed, sir. Would you like me to open the official website?"
        return AppLaunchResult.NotInstalled(
            appId = app.id,
            displayName = app.displayName,
            websiteUrl = app.officialWebsiteUrl,
            message = "${app.displayName} isn't installed, sir. Would you like me to open the official website?"
        )
    }

    override fun launchWebsiteFallback(appId: String): Pair<Boolean, String> {
        val app = getAppMetadata(appId)
            ?: return Pair(false, "Unknown application '$appId', Sir.")
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(app.officialWebsiteUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Opening official website for ${app.displayName}, Sir.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch website for ${app.displayName}", e)
            Pair(false, "Could not open website for ${app.displayName}: ${e.message}")
        }
    }
}
