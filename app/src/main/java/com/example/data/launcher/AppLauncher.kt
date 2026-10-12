package com.example.data.launcher

/**
 * Metadata for an authorized application on the JARVIS allowlist.
 */
data class AllowedApp(
    val id: String,
    val displayName: String,
    val packageName: String,
    val officialWebsiteUrl: String,
    val aliases: List<String> = emptyList()
)

/**
 * Result of an application launch attempt enforcing strict priority:
 * 1. Native installed application
 * 2. Secure Folder instance (if accessible, otherwise prompt user to unlock)
 * 3. Official website prompt if not installed
 */
sealed class AppLaunchResult {
    data class Success(
        val appId: String,
        val displayName: String,
        val message: String,
        val isSecureFolder: Boolean = false
    ) : AppLaunchResult()

    data class SecureFolderLocked(
        val appId: String,
        val displayName: String,
        val message: String
    ) : AppLaunchResult()

    data class NotInstalled(
        val appId: String,
        val displayName: String,
        val websiteUrl: String,
        val message: String
    ) : AppLaunchResult()

    data class NotAllowed(
        val appId: String,
        val message: String
    ) : AppLaunchResult()

    data class Failure(
        val appId: String,
        val displayName: String,
        val errorMessage: String
    ) : AppLaunchResult()
}

/**
 * Platform-specific AppLauncher abstraction:
 * - launchApp(appId)
 * - isAppInstalled(appId)
 * - isAppAvailableInSecureFolder(appId)
 */
interface AppLauncher {
    fun launchApp(appId: String): AppLaunchResult
    fun isAppInstalled(appId: String): Boolean
    fun isAppAvailableInSecureFolder(appId: String): Boolean
    fun getAppMetadata(appId: String): AllowedApp?
    fun getAllowedApps(): List<AllowedApp>
    fun launchWebsiteFallback(appId: String): Pair<Boolean, String>
}
