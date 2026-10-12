package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JarvisState
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisSky
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class WaveformMode(val label: String) {
    SPECTRUM("Bars"),
    OSCILLOSCOPE("Wave"),
    PULSE_RINGS("Pulse")
}

@Composable
fun AudioPulseWaveform(
    state: JarvisState,
    audioLevel: Float,
    modifier: Modifier = Modifier,
    height: Dp = 100.dp,
    initialMode: WaveformMode = WaveformMode.SPECTRUM,
    showControls: Boolean = true
) {
    var currentMode by remember { mutableStateOf(initialMode) }

    // Smooth audio reactivity dampener
    val smoothedAudio by animateFloatAsState(
        targetValue = audioLevel.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 600f),
        label = "smoothed_audio"
    )

    // Infinite transitions for dynamic harmonic phases
    val infiniteTransition = rememberInfiniteTransition(label = "audio_waveform_infinite")

    // Phase angle for waves & bars
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    JarvisState.LISTENING -> 850
                    JarvisState.THINKING, JarvisState.SEARCHING, JarvisState.ANALYZING, JarvisState.PROCESSING, JarvisState.OPENING_APP -> 600
                    JarvisState.SPEAKING -> 1000
                    JarvisState.STANDBY -> 2200
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    // Travelling scanner beam for processing state
    val scanBeamPos by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_beam_pos"
    )

    // Dynamic state accent colors
    val primaryColor by animateColorAsState(
        targetValue = when (state) {
            JarvisState.STANDBY -> JarvisCyan.copy(alpha = 0.6f)
            JarvisState.LISTENING -> JarvisCyan
            JarvisState.PROCESSING -> JarvisGold
            JarvisState.OPENING_APP -> JarvisSky
            JarvisState.THINKING -> JarvisGold
            JarvisState.SEARCHING -> JarvisCyan
            JarvisState.ANALYZING -> JarvisSky
            JarvisState.SPEAKING -> JarvisSky
        },
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "waveform_primary_color"
    )

    val secondaryColor by animateColorAsState(
        targetValue = when (state) {
            JarvisState.STANDBY -> JarvisBorder
            JarvisState.LISTENING -> Color(0xFF00E5FF)
            JarvisState.PROCESSING -> Color(0xFFFF9800)
            JarvisState.OPENING_APP -> Color(0xFF80D8FF)
            JarvisState.THINKING -> Color(0xFFFF5722)
            JarvisState.SEARCHING -> Color(0xFF00E5FF)
            JarvisState.ANALYZING -> Color(0xFF80D8FF)
            JarvisState.SPEAKING -> Color(0xFF80D8FF)
        },
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "waveform_secondary_color"
    )

    // Calculate approximate decibel level for visual telemetry
    val calculatedDb = remember(smoothedAudio, state) {
        when (state) {
            JarvisState.STANDBY -> 18
            JarvisState.LISTENING -> (32 + (smoothedAudio * 48)).toInt()
            JarvisState.PROCESSING, JarvisState.OPENING_APP -> 46
            JarvisState.THINKING -> (45 + (sin(wavePhase) * 6)).toInt()
            JarvisState.SEARCHING -> (52 + (sin(wavePhase * 2f) * 8)).toInt()
            JarvisState.ANALYZING -> (48 + (cos(wavePhase) * 6)).toInt()
            JarvisState.SPEAKING -> (50 + (smoothedAudio * 35)).toInt()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(JarvisSurface.copy(alpha = 0.85f))
            .border(1.dp, JarvisBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
            .testTag("audio_pulse_waveform_container")
    ) {
        // --- 1. TELEMETRY HEADER ---
        if (showControls) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live status badge with pulsing dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                    )
                    Text(
                        text = when (state) {
                            JarvisState.STANDBY -> "IDLE // READY"
                            JarvisState.LISTENING -> "INGRESS // $calculatedDb dB"
                            JarvisState.PROCESSING -> "PROCESSING // INTENT"
                            JarvisState.OPENING_APP -> "LAUNCHING // NATIVE APP"
                            JarvisState.THINKING -> "PROCESSING // NEURAL"
                            JarvisState.SEARCHING -> "RETRIEVAL // LIVE WEB"
                            JarvisState.ANALYZING -> "ANALYZING // SYNTHESIS"
                            JarvisState.SPEAKING -> "VOCAL SYNTH // ACTIVE"
                        },
                        color = primaryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Mode toggle chips (Bars / Wave / Pulse)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WaveformMode.values().forEach { mode ->
                        val isSelected = currentMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) primaryColor else JarvisSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) primaryColor else JarvisBorder,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { currentMode = mode }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("waveform_mode_${mode.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.label,
                                color = if (isSelected) Color(0xFF00232B) else JarvisTextSecondary,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // --- 2. ANIMATED CANVAS VISUALIZATION ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .testTag("waveform_canvas")
            ) {
                when (currentMode) {
                    WaveformMode.SPECTRUM -> {
                        drawSpectrumBars(
                            state = state,
                            audioLevel = smoothedAudio,
                            phase = wavePhase,
                            scanPos = scanBeamPos,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor
                        )
                    }
                    WaveformMode.OSCILLOSCOPE -> {
                        drawOscilloscopeWave(
                            state = state,
                            audioLevel = smoothedAudio,
                            phase = wavePhase,
                            scanPos = scanBeamPos,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor
                        )
                    }
                    WaveformMode.PULSE_RINGS -> {
                        drawAudioPulseRings(
                            state = state,
                            audioLevel = smoothedAudio,
                            phase = wavePhase,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor
                        )
                    }
                }
            }
        }

        // --- 3. BOTTOM INTENSITY METER STRIP ---
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "SIGNAL",
                color = JarvisTextMuted,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )

            // Dynamic progress meter
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(JarvisSurfaceVariant)
            ) {
                val fillRatio = when (state) {
                    JarvisState.STANDBY -> 0.15f
                    JarvisState.LISTENING -> (0.25f + (smoothedAudio * 0.75f)).coerceIn(0.1f, 1f)
                    JarvisState.PROCESSING -> 0.50f
                    JarvisState.OPENING_APP -> 0.75f
                    JarvisState.THINKING -> (0.45f + (sin(wavePhase) * 0.35f).toFloat().coerceIn(0f, 0.45f))
                    JarvisState.SEARCHING -> 0.65f
                    JarvisState.ANALYZING -> 0.55f
                    JarvisState.SPEAKING -> (0.40f + (smoothedAudio * 0.60f)).coerceIn(0.2f, 1f)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillRatio)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.5f), primaryColor)
                            )
                        )
                )
            }

            Text(
                text = "${(smoothedAudio * 100).toInt()}%",
                color = primaryColor,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -------------------------------------------------------------
// 1. SPECTRUM EQUALIZER BARS WITH FLOATING PEAKS
// -------------------------------------------------------------
private fun DrawScope.drawSpectrumBars(
    state: JarvisState,
    audioLevel: Float,
    phase: Float,
    scanPos: Float,
    primaryColor: Color,
    secondaryColor: Color,
    barCount: Int = 30
) {
    val totalWidth = size.width
    val canvasHeight = size.height
    val centerY = canvasHeight / 2
    val barWidth = (totalWidth / (barCount * 1.55f)).coerceIn(2.5.dp.toPx(), 7.dp.toPx())
    val totalBarOccupied = barWidth * barCount
    val spacing = if (barCount > 1) (totalWidth - totalBarOccupied) / (barCount - 1) else 0f

    val baseActivity = when (state) {
        JarvisState.STANDBY -> 0.14f
        JarvisState.LISTENING -> 0.30f + (audioLevel * 0.70f)
        JarvisState.PROCESSING -> 0.50f
        JarvisState.OPENING_APP -> 0.60f
        JarvisState.THINKING -> 0.55f
        JarvisState.SEARCHING -> 0.65f
        JarvisState.ANALYZING -> 0.50f
        JarvisState.SPEAKING -> 0.45f + (audioLevel * 0.55f)
    }

    for (i in 0 until barCount) {
        val normalizedIdx = i.toFloat() / barCount
        // Bell curve envelope so edges smoothly taper
        val bellFactor = sin(normalizedIdx * Math.PI).toFloat().coerceIn(0.25f, 1f)

        // Multi-frequency harmonic combination
        val harmonic1 = sin(phase + (i * 0.38f))
        val harmonic2 = cos((phase * 1.4f) + (i * 0.22f))
        val waveAmplitude = abs((harmonic1 * 0.65f) + (harmonic2 * 0.35f))

        // Extra dynamic spike if scanner passes this bar (thinking mode)
        val scannerDistance = abs(normalizedIdx - scanPos)
        val scanBoost = if (state == JarvisState.THINKING && scannerDistance < 0.12f) {
            (1f - (scannerDistance / 0.12f)) * 0.5f
        } else 0f

        val calculatedHeight = ((canvasHeight * 0.82f * (baseActivity + scanBoost) * waveAmplitude * bellFactor) + 4.dp.toPx())
            .coerceIn(4.dp.toPx(), canvasHeight * 0.95f)

        val x = i * (barWidth + spacing)
        val y = centerY - (calculatedHeight / 2)

        // Draw bar column with vertical glowing gradient
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = 0.95f),
                    primaryColor.copy(alpha = 0.85f),
                    primaryColor.copy(alpha = 0.25f)
                ),
                startY = y,
                endY = y + calculatedHeight
            ),
            topLeft = Offset(x, y),
            size = Size(barWidth, calculatedHeight),
            cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
        )

        // Draw floating peak caps above active bars
        if (state != JarvisState.STANDBY && waveAmplitude > 0.4f) {
            val peakOffset = 4.dp.toPx()
            val peakTop = (y - peakOffset).coerceAtLeast(1.dp.toPx())
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(x, peakTop),
                size = Size(barWidth, 2.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }
}

// -------------------------------------------------------------
// 2. OSCILLOSCOPE CONTINUOUS SINE WAVE WITH SCANNING BEAM
// -------------------------------------------------------------
private fun DrawScope.drawOscilloscopeWave(
    state: JarvisState,
    audioLevel: Float,
    phase: Float,
    scanPos: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    val totalWidth = size.width
    val canvasHeight = size.height
    val centerY = canvasHeight / 2

    val maxAmplitude = when (state) {
        JarvisState.STANDBY -> canvasHeight * 0.12f
        JarvisState.LISTENING -> (canvasHeight * 0.20f) + (audioLevel * canvasHeight * 0.38f)
        JarvisState.PROCESSING, JarvisState.OPENING_APP -> canvasHeight * 0.32f
        JarvisState.THINKING, JarvisState.ANALYZING -> canvasHeight * 0.34f
        JarvisState.SEARCHING -> canvasHeight * 0.38f
        JarvisState.SPEAKING -> (canvasHeight * 0.22f) + (audioLevel * canvasHeight * 0.32f)
    }

    // 1. Primary Smooth Waveform Path
    val primaryPath = Path()
    val fillPath = Path()
    val points = 80
    val stepX = totalWidth / points

    fillPath.moveTo(0f, canvasHeight)

    for (p in 0..points) {
        val x = p * stepX
        val normalizedX = p.toFloat() / points
        val envelope = sin(normalizedX * Math.PI).toFloat()

        // Wave combining fundamental + harmonics
        val yOffset = when (state) {
            JarvisState.THINKING, JarvisState.ANALYZING, JarvisState.SEARCHING -> {
                // High-speed neural / search ripple
                (sin(phase * 2f + normalizedX * 12f) * 0.7f + cos(phase * 3f + normalizedX * 24f) * 0.3f) * maxAmplitude * envelope
            }
            JarvisState.LISTENING -> {
                // Audio reactive harmonic
                (sin(phase + normalizedX * 8f) * 0.8f + cos(phase * 2f + normalizedX * 16f) * (0.2f + audioLevel * 0.4f)) * maxAmplitude * envelope
            }
            else -> {
                sin(phase + normalizedX * 6f) * maxAmplitude * envelope
            }
        }

        val y = centerY + yOffset.toFloat()

        if (p == 0) {
            primaryPath.moveTo(x, y)
            fillPath.lineTo(x, y)
        } else {
            primaryPath.lineTo(x, y)
            fillPath.lineTo(x, y)
        }
    }

    fillPath.lineTo(totalWidth, canvasHeight)
    fillPath.close()

    // Draw ambient luminous underglow under the curve
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.22f),
                Color.Transparent
            ),
            startY = centerY - maxAmplitude,
            endY = canvasHeight
        )
    )

    // Draw main glowing stroke
    drawPath(
        path = primaryPath,
        brush = Brush.horizontalGradient(
            colors = listOf(
                secondaryColor.copy(alpha = 0.4f),
                primaryColor,
                secondaryColor.copy(alpha = 0.4f)
            )
        ),
        style = Stroke(
            width = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    )

    // 2. Secondary Phase Shifted Ghost Line
    val secondaryPath = Path()
    for (p in 0..points) {
        val x = p * stepX
        val normalizedX = p.toFloat() / points
        val envelope = sin(normalizedX * Math.PI).toFloat()
        val yOffset = cos(phase * 1.5f + normalizedX * 8f) * (maxAmplitude * 0.55f) * envelope
        val y = centerY + yOffset.toFloat()

        if (p == 0) secondaryPath.moveTo(x, y) else secondaryPath.lineTo(x, y)
    }

    drawPath(
        path = secondaryPath,
        color = secondaryColor.copy(alpha = 0.35f),
        style = Stroke(
            width = 1.2.dp.toPx(),
            cap = StrokeCap.Round
        )
    )

    // 3. Travelling Neural Scanning Bead during Thinking/Listening
    if (state == JarvisState.THINKING || (state == JarvisState.LISTENING && audioLevel > 0.15f)) {
        val beadX = scanPos * totalWidth
        val envelope = sin(scanPos * Math.PI).toFloat()
        val beadY = centerY + (sin(phase * 2f + scanPos * 12f) * maxAmplitude * envelope).toFloat()

        drawCircle(
            color = primaryColor.copy(alpha = 0.35f),
            radius = 10.dp.toPx(),
            center = Offset(beadX, beadY)
        )
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = Offset(beadX, beadY)
        )
    }
}

// -------------------------------------------------------------
// 3. RADIAL CONCENTRIC AUDIO PULSE RINGS (SONIC RADAR)
// -------------------------------------------------------------
private fun DrawScope.drawAudioPulseRings(
    state: JarvisState,
    audioLevel: Float,
    phase: Float,
    primaryColor: Color,
    secondaryColor: Color,
    ringCount: Int = 4
) {
    val center = Offset(size.width / 2, size.height / 2)
    val maxRadius = (size.height.coerceAtMost(size.width) / 2) * 0.95f

    // Core pulsing orb in center
    val coreRadius = (10.dp.toPx() + (audioLevel * 14.dp.toPx())).coerceIn(8.dp.toPx(), maxRadius * 0.45f)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, primaryColor, primaryColor.copy(alpha = 0.2f)),
            center = center,
            radius = coreRadius * 1.4f
        ),
        radius = coreRadius,
        center = center
    )

    // Concentric expanding acoustic pulse rings
    for (i in 0 until ringCount) {
        val ringPhase = (phase / (2 * Math.PI).toFloat() + (i.toFloat() / ringCount)) % 1f
        val currentRadius = coreRadius + (ringPhase * (maxRadius - coreRadius))
        val alpha = ((1f - ringPhase) * when (state) {
            JarvisState.STANDBY -> 0.25f
            JarvisState.LISTENING -> 0.40f + (audioLevel * 0.60f)
            JarvisState.PROCESSING, JarvisState.OPENING_APP -> 0.65f
            JarvisState.THINKING, JarvisState.ANALYZING -> 0.70f
            JarvisState.SEARCHING -> 0.85f
            JarvisState.SPEAKING -> 0.50f + (audioLevel * 0.40f)
        }).coerceIn(0f, 1f)

        drawCircle(
            color = if (i % 2 == 0) primaryColor.copy(alpha = alpha) else secondaryColor.copy(alpha = alpha),
            radius = currentRadius,
            center = center,
            style = Stroke(
                width = if (i == 0) 2.dp.toPx() else 1.2.dp.toPx()
            )
        )
    }

    // High-tech circular tick marks around edge
    val tickCount = 24
    for (t in 0 until tickCount) {
        val angle = (t.toFloat() / tickCount) * 2 * Math.PI
        val tickStartRadius = maxRadius - 4.dp.toPx()
        val tickEndRadius = maxRadius

        val startX = center.x + (cos(angle) * tickStartRadius).toFloat()
        val startY = center.y + (sin(angle) * tickStartRadius).toFloat()
        val endX = center.x + (cos(angle) * tickEndRadius).toFloat()
        val endY = center.y + (sin(angle) * tickEndRadius).toFloat()

        drawLine(
            color = primaryColor.copy(alpha = 0.35f),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 1.dp.toPx()
        )
    }
}
