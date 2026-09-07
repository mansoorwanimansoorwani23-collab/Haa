package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AssistantState
import com.example.ui.theme.QuantumCyanAccent
import com.example.ui.theme.QuantumGreenBright
import com.example.ui.theme.QuantumGreenDim
import com.example.ui.theme.QuantumGreenPrimary
import com.example.ui.theme.QuantumRedAlert
import com.example.ui.theme.QuantumSurfaceBorder
import com.example.ui.theme.QuantumSurfaceCard
import com.example.ui.theme.QuantumTextMuted
import com.example.ui.theme.QuantumTextPrimary
import com.example.ui.theme.QuantumTextSecondary
import kotlin.math.sin

@Composable
fun TelemetryHeader(
    state: AssistantState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "TelemetryTransition")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BlinkAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Quantum Status Blinker
                val statusColor = when (state) {
                    AssistantState.ERROR -> QuantumRedAlert
                    AssistantState.THINKING -> QuantumCyanAccent
                    AssistantState.LISTENING -> QuantumGreenBright
                    AssistantState.SPEAKING -> QuantumGreenBright
                    AssistantState.IDLE -> QuantumGreenPrimary
                }

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = blinkAlpha))
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "JARVIS // QUANTUM CORE",
                        color = QuantumGreenPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "ARCHITECT: RAUF",
                        color = QuantumCyanAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Status Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(QuantumSurfaceCard)
                    .border(1.dp, QuantumSurfaceBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = when (state) {
                        AssistantState.IDLE -> "ONLINE // IDLE"
                        AssistantState.LISTENING -> "AUDIO CAPTURE"
                        AssistantState.THINKING -> "QUANTUM COMPUTING"
                        AssistantState.SPEAKING -> "TRANSMITTING"
                        AssistantState.ERROR -> "SYSTEM ALERT"
                    },
                    color = when (state) {
                        AssistantState.ERROR -> QuantumRedAlert
                        AssistantState.THINKING -> QuantumCyanAccent
                        else -> QuantumGreenBright
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Cyber telemetry line
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ENTANGLEMENT: 99.8% SYNCHRONIZED",
                color = QuantumTextMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "ACCESS: GRANTED",
                color = QuantumGreenPrimary.copy(alpha = 0.8f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun AudioWaveformVisualizer(
    amplitude: Float,
    state: AssistantState,
    modifier: Modifier = Modifier
) {
    val barCount = 18
    val isActive = state == AssistantState.LISTENING || state == AssistantState.SPEAKING

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val waveOffset = (sin(i * 0.45) * 0.5 + 0.5).toFloat()
            val targetHeight = if (isActive) {
                (4.dp + (28.dp * amplitude * waveOffset)).coerceIn(4.dp, 32.dp)
            } else {
                3.dp
            }

            val barColor = when (state) {
                AssistantState.SPEAKING -> QuantumGreenBright
                AssistantState.LISTENING -> QuantumCyanAccent
                AssistantState.THINKING -> QuantumGreenDim
                AssistantState.ERROR -> QuantumRedAlert
                AssistantState.IDLE -> QuantumGreenDim.copy(alpha = 0.35f)
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(targetHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}
