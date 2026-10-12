package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.JarvisState
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisSky
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    state: JarvisState,
    audioLevel: Float,
    modifier: Modifier = Modifier,
    height: Dp = 36.dp,
    barCount: Int = 24
) {
    val transition = rememberInfiniteTransition(label = "waveform_transition")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveform_phase"
    )

    val activeColor = when (state) {
        JarvisState.STANDBY -> JarvisSky.copy(alpha = 0.5f)
        JarvisState.LISTENING -> JarvisCyan
        JarvisState.PROCESSING -> JarvisGold
        JarvisState.OPENING_APP -> JarvisCyan
        JarvisState.THINKING -> JarvisGold
        JarvisState.SEARCHING -> JarvisCyan
        JarvisState.ANALYZING -> JarvisSky
        JarvisState.SPEAKING -> JarvisCyan
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2
        val barWidth = (totalWidth / (barCount * 1.6f)).coerceIn(2.dp.toPx(), 6.dp.toPx())
        val spacing = (totalWidth - (barWidth * barCount)) / (barCount - 1)

        val baseActivity = when (state) {
            JarvisState.STANDBY -> 0.12f
            JarvisState.LISTENING -> 0.35f + (audioLevel * 0.65f)
            JarvisState.PROCESSING -> 0.45f
            JarvisState.OPENING_APP -> 0.50f
            JarvisState.THINKING -> 0.45f
            JarvisState.SEARCHING -> 0.55f
            JarvisState.ANALYZING -> 0.50f
            JarvisState.SPEAKING -> 0.40f + (audioLevel * 0.60f)
        }

        for (i in 0 until barCount) {
            val normalizedIdx = i.toFloat() / barCount
            // Bell curve window so edges are lower than center
            val bellFactor = sin(normalizedIdx * Math.PI).toFloat()

            // Dynamic sine wave height calculation
            val wave = abs(sin(phase + (i * 0.45f)))
            val barHeight = ((canvasHeight * 0.85f * baseActivity * wave * bellFactor) + 4.dp.toPx())
                .coerceIn(3.dp.toPx(), canvasHeight)

            val x = i * (barWidth + spacing)
            val y = centerY - (barHeight / 2)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        activeColor.copy(alpha = 0.9f),
                        activeColor.copy(alpha = 0.3f)
                    ),
                    startY = y,
                    endY = y + barHeight
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
