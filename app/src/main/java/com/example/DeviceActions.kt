package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import java.net.URLEncoder

object DeviceActions {

    fun openApp(context: Context, packageName: String): Boolean {
        return try {
            val intent = context.packageManager
                .getLaunchIntentForPackage(packageName)
                ?: return false

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openChrome(context: Context): Boolean {
        return openApp(context, "com.android.chrome")
    }

    fun searchGoogle(context: Context, query: String): Boolean {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=$encoded")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun setAlarm(
        context: Context,
        hour: Int,
        minute: Int,
        message: String = "JARVIS Alarm"
    ): Boolean {
        if (hour !in 0..23 || minute !in 0..59) return false

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openDialer(context: Context, number: String): Boolean {
        return try {
            val intent = Intent(
                Intent.ACTION_DIAL,
                Uri.parse("tel:${Uri.encode(number)}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun prepareMessage(
        context: Context,
        number: String,
        message: String
    ): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(number)}")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
