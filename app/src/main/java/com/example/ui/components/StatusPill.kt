package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JarvisState
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisSky
import com.example.ui.theme.JarvisSurface

@Composable
fun StatusPill(
    state: JarvisState,
    modifier: Modifier = Modifier
) {
    val (statusText, statusColor) = when (state) {
        JarvisState.STANDBY -> "STANDBY // NOMINAL" to JarvisSky
        JarvisState.LISTENING -> "LISTENING // AUDIO INPUT" to JarvisCyan
        JarvisState.PROCESSING -> "UNDERSTANDING // PROCESSING" to JarvisGold
        JarvisState.OPENING_APP -> "OPENING APP // NATIVE LAUNCH" to JarvisGreen
        JarvisState.THINKING -> "THINKING // NEURAL MATRIX" to JarvisGold
        JarvisState.SEARCHING -> "SEARCHING // LIVE WEB" to JarvisCyan
        JarvisState.ANALYZING -> "ANALYZING // SYNTHESIS" to JarvisSky
        JarvisState.SPEAKING -> "SPEAKING // VOCAL CORE" to JarvisGreen
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(JarvisSurface.copy(alpha = 0.85f))
            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Glowing status dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(statusColor)
        )

        AnimatedContent(
            targetState = statusText,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "status_text_anim"
        ) { text ->
            Text(
                text = text,
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.2.sp
            )
        }
    }
}
