package com.example.data.messaging

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.example.data.model.MessagingServiceType
import com.example.data.model.ServiceUnreadReport

/**
 * Manages authorized messaging services for the HOME_ARRIVAL assistant protocol.
 * Enforces strict permission, authorization, truthfulness, and privacy constraints:
 * - Checks ONLY authorized services
 * - NEVER fabricates message counts in production responses
 * - If service is not connected, clearly reports "[Service] is not connected, sir."
 * - If an integration fails to retrieve real data, reports "I couldn't check [Service] right now, sir."
 * - Clearly labels simulated/demo data as SIMULATED
 * - Retrieves unread counts separately from message content
 * - Does not scrape private pages or bypass security
 */
class HomeArrivalManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "HomeArrivalManager"
    }

    /**
     * Checks if a target application package is installed on the device.
     */
    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            context.packageManager.getLaunchIntentForPackage(packageName) != null
        } catch (e: Exception) {
            Log.w(TAG, "Error checking package $packageName: ${e.message}")
            false
        }
    }

    /**
     * Checks a single messaging service under strict zero-fabrication privacy rules.
     */
    fun checkSingleService(
        service: MessagingServiceType,
        isAuthorized: Boolean,
        isSimulatedMode: Boolean = false
    ): ServiceUnreadReport {
        if (!isAuthorized) {
            return ServiceUnreadReport(
                service = service,
                isAuthorized = false,
                isAvailable = false,
                unreadCount = 0,
                unavailableReason = "${service.displayName} is not connected, sir.",
                isSimulated = false
            )
        }

        if (isSimulatedMode) {
            // Explicit demo testing simulation mode
            val count = when (service) {
                MessagingServiceType.INSTAGRAM -> 2
                MessagingServiceType.TELEGRAM -> 3
                else -> 0
            }
            return ServiceUnreadReport(
                service = service,
                isAuthorized = true,
                isAvailable = true,
                unreadCount = count,
                unavailableReason = null,
                isSimulated = true
            )
        }

        // Production Mode: NEVER fabricate message counts.
        // Third-party applications on Android cannot be queried for private unread counts
        // without an active official account API or NotificationListener permission.
        // When integration cannot retrieve real data from the OS, report failure honestly.
        return ServiceUnreadReport(
            service = service,
            isAuthorized = true,
            isAvailable = false,
            unreadCount = 0,
            unavailableReason = "I couldn't check ${service.displayName} right now, sir.",
            isSimulated = false
        )
    }

    /**
     * Inspects authorized services according to user configurations and privacy settings.
     * When isSimulatedMode is false (production), NEVER fabricates message counts.
     */
    fun checkAuthorizedServices(
        isInstagramAuthorized: Boolean,
        isTelegramAuthorized: Boolean,
        isWhatsAppAuthorized: Boolean,
        isGmailAuthorized: Boolean,
        isDiscordAuthorized: Boolean = false,
        unreadCountsEnabled: Boolean = true,
        readContentsEnabled: Boolean = false,
        senderNamesEnabled: Boolean = false,
        isSimulatedMode: Boolean = false
    ): List<ServiceUnreadReport> {
        if (!unreadCountsEnabled) {
            return emptyList()
        }

        val reports = mutableListOf<ServiceUnreadReport>()
        if (isInstagramAuthorized) {
            reports.add(checkSingleService(MessagingServiceType.INSTAGRAM, true, isSimulatedMode))
        }
        if (isTelegramAuthorized) {
            reports.add(checkSingleService(MessagingServiceType.TELEGRAM, true, isSimulatedMode))
        }
        if (isWhatsAppAuthorized) {
            reports.add(checkSingleService(MessagingServiceType.WHATSAPP, true, isSimulatedMode))
        }
        if (isGmailAuthorized) {
            reports.add(checkSingleService(MessagingServiceType.GMAIL, true, isSimulatedMode))
        }
        if (isDiscordAuthorized) {
            reports.add(checkSingleService(MessagingServiceType.DISCORD, true, isSimulatedMode))
        }

        return reports
    }

    /**
     * Safely opens an authorized service.
     * Respects Android OS security restrictions, launch intents, and privacy controls.
     */
    fun openService(service: MessagingServiceType): Pair<Boolean, String> {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(service.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                Pair(true, "Opening ${service.displayName}, Sir.")
            } else {
                // Fallback to browser web interface without bypassing security
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(service.webFallbackUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                Pair(true, "Launching ${service.displayName} web inbox, Sir.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch service ${service.displayName}", e)
            Pair(false, "Could not open ${service.displayName}: ${e.message}")
        }
    }
}
