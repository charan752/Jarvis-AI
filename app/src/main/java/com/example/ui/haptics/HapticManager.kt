package com.example.ui.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

enum class HapticIntensity(val label: String, val description: String) {
    OFF("Off", "No physical vibration"),
    LIGHT("Light", "Subtle micro-ticks and soft feedback"),
    NORMAL("Normal", "Balanced standard tactile response"),
    STRONG("Strong", "Crisp, pronounced sensory feedback")
}

class HapticManager(private val context: Context) {
    companion object {
        private const val TAG = "HapticManager"
    }

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not initialize Vibrator", e)
            null
        }
    }

    var intensity: HapticIntensity = HapticIntensity.NORMAL

    val isHapticsEnabled: Boolean
        get() = intensity != HapticIntensity.OFF

    fun onListeningTransition() {
        if (intensity == HapticIntensity.OFF) return
        try {
            when (intensity) {
                HapticIntensity.OFF -> return
                HapticIntensity.LIGHT -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(10, 60))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(10)
                    }
                }
                HapticIntensity.NORMAL -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(20, 140))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(20)
                    }
                }
                HapticIntensity.STRONG -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(35, 255))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(35)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Haptic listening failed", e)
        }
    }

    fun onThinkingTransition() {
        if (intensity == HapticIntensity.OFF) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val (timings, amplitudes) = when (intensity) {
                    HapticIntensity.OFF -> return
                    HapticIntensity.LIGHT -> longArrayOf(0, 10, 40, 10) to intArrayOf(0, 50, 0, 70)
                    HapticIntensity.NORMAL -> longArrayOf(0, 16, 45, 16) to intArrayOf(0, 100, 0, 130)
                    HapticIntensity.STRONG -> longArrayOf(0, 24, 45, 24) to intArrayOf(0, 190, 0, 255)
                }
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                val duration = when (intensity) {
                    HapticIntensity.LIGHT -> 15L
                    HapticIntensity.NORMAL -> 30L
                    HapticIntensity.STRONG -> 50L
                    HapticIntensity.OFF -> 0L
                }
                @Suppress("DEPRECATION")
                if (duration > 0) vibrator?.vibrate(duration)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Haptic thinking failed", e)
        }
    }

    fun onSpeakingTransition() {
        if (intensity == HapticIntensity.OFF) return
        try {
            when (intensity) {
                HapticIntensity.OFF -> return
                HapticIntensity.LIGHT -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(14, 50))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(14)
                    }
                }
                HapticIntensity.NORMAL -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(25, 90))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(25)
                    }
                }
                HapticIntensity.STRONG -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(40, 210))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(40)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Haptic speaking failed", e)
        }
    }

    fun onStandbyTransition() {
        if (intensity == HapticIntensity.OFF) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val (dur, amp) = when (intensity) {
                    HapticIntensity.OFF -> return
                    HapticIntensity.LIGHT -> 8L to 35
                    HapticIntensity.NORMAL -> 12L to 60
                    HapticIntensity.STRONG -> 20L to 140
                }
                vibrator?.vibrate(VibrationEffect.createOneShot(dur, amp))
            }
        } catch (_: Exception) {}
    }

    fun testSample(targetIntensity: HapticIntensity) {
        val original = intensity
        intensity = targetIntensity
        onListeningTransition()
        intensity = original
    }
}
