package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.JarvisState
import com.example.ui.theme.JarvisBlue
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisSky
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JarvisOrb(
    state: JarvisState,
    audioLevel: Float,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp
) {
    val transition = rememberInfiniteTransition(label = "jarvis_orb_transition")

    // Smooth color transitions between states
    val targetGlowColor = when (state) {
        JarvisState.STANDBY -> JarvisCyan.copy(alpha = 0.6f)
        JarvisState.LISTENING -> JarvisCyan
        JarvisState.PROCESSING -> JarvisGold
        JarvisState.OPENING_APP -> Color(0xFF00E676)
        JarvisState.THINKING -> JarvisGold
        JarvisState.SEARCHING -> JarvisCyan
        JarvisState.ANALYZING -> JarvisSky
        JarvisState.SPEAKING -> JarvisSky
    }
    val animatedGlowColor by animateColorAsState(
        targetValue = targetGlowColor,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "orb_glow_color"
    )

    val targetSecondaryGlow = when (state) {
        JarvisState.STANDBY -> JarvisBlue
        JarvisState.LISTENING -> Color(0xFF00E5FF)
        JarvisState.PROCESSING -> Color(0xFFFF9800)
        JarvisState.OPENING_APP -> Color(0xFF00E676)
        JarvisState.THINKING -> Color(0xFFFF5722)
        JarvisState.SEARCHING -> Color(0xFF00E5FF)
        JarvisState.ANALYZING -> JarvisSky
        JarvisState.SPEAKING -> JarvisCyan
    }
    val animatedSecondaryGlow by animateColorAsState(
        targetValue = targetSecondaryGlow,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "orb_sec_glow"
    )

    val targetCoreColor = when (state) {
        JarvisState.STANDBY -> Color(0xFF00B4D8)
        JarvisState.LISTENING -> Color(0xFFE0FFFF)
        JarvisState.PROCESSING -> Color(0xFFFFE082)
        JarvisState.OPENING_APP -> Color(0xFFB9F6CA)
        JarvisState.THINKING -> Color(0xFFFFE082)
        JarvisState.SEARCHING -> Color(0xFFE0FFFF)
        JarvisState.ANALYZING -> Color(0xFFBAE6FD)
        JarvisState.SPEAKING -> Color(0xFFBAE6FD)
    }
    val animatedCoreColor by animateColorAsState(
        targetValue = targetCoreColor,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "orb_core_color"
    )

    // Smooth state-based scale expansion
    val targetScale = when (state) {
        JarvisState.STANDBY -> 1.0f
        JarvisState.LISTENING -> 1.14f
        JarvisState.PROCESSING -> 0.98f
        JarvisState.OPENING_APP -> 1.12f
        JarvisState.THINKING -> 0.94f
        JarvisState.SEARCHING -> 1.05f
        JarvisState.ANALYZING -> 1.02f
        JarvisState.SPEAKING -> 1.08f
    }
    val animatedStateScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "orb_scale_spring"
    )

    // Dynamic rotation multiplier
    val targetRotSpeed = when (state) {
        JarvisState.STANDBY -> 1.0f
        JarvisState.LISTENING -> 1.3f
        JarvisState.PROCESSING -> 3.0f
        JarvisState.OPENING_APP -> 3.8f
        JarvisState.THINKING -> 4.2f
        JarvisState.SEARCHING -> 3.5f
        JarvisState.ANALYZING -> 2.8f
        JarvisState.SPEAKING -> 2.2f
    }
    val animatedRotSpeed by animateFloatAsState(
        targetValue = targetRotSpeed,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "orb_rot_speed"
    )

    // Base continuous angle animation
    val baseAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "base_angle"
    )

    // Ripple / Radar pulse animation
    val pulseProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    JarvisState.LISTENING -> 900
                    JarvisState.THINKING, JarvisState.SEARCHING, JarvisState.ANALYZING, JarvisState.PROCESSING, JarvisState.OPENING_APP -> 700
                    JarvisState.SPEAKING -> 600
                    JarvisState.STANDBY -> 2200
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_progress"
    )

    // Breathing pulse for natural organic rhythm
    val breathingPulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    JarvisState.LISTENING -> 700
                    JarvisState.THINKING, JarvisState.SEARCHING, JarvisState.ANALYZING, JarvisState.PROCESSING, JarvisState.OPENING_APP -> 500
                    JarvisState.SPEAKING -> 400
                    JarvisState.STANDBY -> 2000
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_pulse"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val maxR = (this.size.minDimension / 2) * 0.95f

            // Dynamic reactive radius
            val effectiveRadius = (maxR * animatedStateScale * breathingPulse + (audioLevel * 16.dp.toPx()))
                .coerceIn(maxR * 0.6f, maxR * 1.05f)

            // 1. Ambient Background Halo Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedGlowColor.copy(alpha = 0.35f),
                        animatedSecondaryGlow.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = effectiveRadius * 1.15f
                ),
                radius = effectiveRadius * 1.15f,
                center = center
            )

            // 2. State-Specific Expanding Pulse Waves
            if (state == JarvisState.LISTENING || state == JarvisState.SPEAKING) {
                val rippleR = effectiveRadius * (0.6f + (pulseProgress * 0.45f))
                val rippleAlpha = (1f - pulseProgress).coerceIn(0f, 1f) * 0.6f
                drawCircle(
                    color = animatedGlowColor.copy(alpha = rippleAlpha),
                    radius = rippleR,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Secondary delayed ripple
                val rippleR2 = effectiveRadius * (0.6f + (((pulseProgress + 0.5f) % 1f) * 0.45f))
                val rippleAlpha2 = (1f - ((pulseProgress + 0.5f) % 1f)).coerceIn(0f, 1f) * 0.4f
                drawCircle(
                    color = animatedSecondaryGlow.copy(alpha = rippleAlpha2),
                    radius = rippleR2,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }

            // 3. Outer Segmented Tech Ring with Rotation
            val isHighActivity = state == JarvisState.THINKING || state == JarvisState.SEARCHING ||
                    state == JarvisState.ANALYZING || state == JarvisState.PROCESSING || state == JarvisState.OPENING_APP
            val currentBaseRot = (baseAngle * animatedRotSpeed) % 360f
            rotate(currentBaseRot, pivot = center) {
                val outerR = effectiveRadius * 0.88f
                val arcCount = if (isHighActivity) 6 else 4
                val sweep = if (isHighActivity) 42f else 70f
                val step = 360f / arcCount

                for (i in 0 until arcCount) {
                    val startAngle = i * step + 8f
                    drawArc(
                        color = animatedGlowColor.copy(alpha = 0.85f),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - outerR, center.y - outerR),
                        size = Size(outerR * 2, outerR * 2),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Peripheral Tech Notches
                val notchCount = if (isHighActivity) 24 else 12
                val notchStep = 360 / notchCount
                for (angle in 0 until 360 step notchStep) {
                    val rad = Math.toRadians(angle.toDouble())
                    val tickStart = outerR + 2.dp.toPx()
                    val tickLen = if (angle % (notchStep * 2) == 0) 6.dp.toPx() else 3.5.dp.toPx()
                    val start = Offset(
                        (center.x + tickStart * cos(rad)).toFloat(),
                        (center.y + tickStart * sin(rad)).toFloat()
                    )
                    val end = Offset(
                        (center.x + (tickStart + tickLen) * cos(rad)).toFloat(),
                        (center.y + (tickStart + tickLen) * sin(rad)).toFloat()
                    )
                    drawLine(
                        color = animatedSecondaryGlow.copy(alpha = 0.65f),
                        start = start,
                        end = end,
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }

            // 4. Middle Counter-Rotating Gyro Blades
            val counterRot = (-baseAngle * animatedRotSpeed * 1.4f) % 360f
            rotate(counterRot, pivot = center) {
                val midR = effectiveRadius * 0.66f
                val bladeCount = if (isHighActivity) 4 else 3
                val sweep = 80f
                val step = 360f / bladeCount

                for (i in 0 until bladeCount) {
                    val startAngle = i * step + 12f
                    drawArc(
                        color = animatedSecondaryGlow.copy(alpha = 0.9f),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - midR, center.y - midR),
                        size = Size(midR * 2, midR * 2),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // 5. Orbiting Satellites / Energy Nodes (Accelerate during Thinking/Speaking)
            val satelliteCount = if (isHighActivity) 6 else 4
            val satRadius = effectiveRadius * 0.77f
            for (i in 0 until satelliteCount) {
                val satAngle = Math.toRadians((currentBaseRot * 1.5 + (i * (360.0 / satelliteCount))) % 360.0)
                val nodePos = Offset(
                    (center.x + satRadius * cos(satAngle)).toFloat(),
                    (center.y + satRadius * sin(satAngle)).toFloat()
                )
                // Draw satellite node
                drawCircle(
                    color = Color.White,
                    radius = (2.2.dp.toPx() + (audioLevel * 2.dp.toPx())),
                    center = nodePos
                )
                drawCircle(
                    color = animatedGlowColor.copy(alpha = 0.7f),
                    radius = (4.5.dp.toPx() + (audioLevel * 3.dp.toPx())),
                    center = nodePos
                )
            }

            // 6. Inner Reactor Ring with Radial Nodes
            val innerR = effectiveRadius * 0.44f
            drawCircle(
                color = animatedGlowColor.copy(alpha = 0.4f),
                radius = innerR,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 7. Central Radiant Energy Iris Core
            val coreR = (effectiveRadius * 0.28f + (audioLevel * 14.dp.toPx()))
                .coerceAtMost(effectiveRadius * 0.5f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        animatedCoreColor,
                        animatedGlowColor,
                        animatedSecondaryGlow,
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreR
                ),
                radius = coreR,
                center = center
            )

            // Central White-Hot Singularity Point
            drawCircle(
                color = Color.White,
                radius = (coreR * 0.25f).coerceAtLeast(3.dp.toPx()),
                center = center
            )
        }
    }
}
