package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.ui.AssistantState
import com.example.ui.theme.QuantumCyanAccent
import com.example.ui.theme.QuantumGreenBright
import com.example.ui.theme.QuantumGreenGlow
import com.example.ui.theme.QuantumGreenPrimary
import com.example.ui.theme.QuantumRedAlert
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun QuantumOrb(
    state: AssistantState,
    amplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "QuantumOrbAnimation")

    // Continuous slow orbit rotation for idle/ambient
    val baseRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == AssistantState.THINKING) 2500 else 14000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "BaseRotation"
    )

    // Breathing pulse for core
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == AssistantState.SPEAKING) 400 else 1800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingPulse"
    )

    // Radar scan angle
    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarAngle"
    )

    val primaryColor = when (state) {
        AssistantState.ERROR -> QuantumRedAlert
        AssistantState.THINKING -> QuantumCyanAccent
        AssistantState.LISTENING -> QuantumGreenBright
        AssistantState.SPEAKING -> QuantumGreenBright
        AssistantState.IDLE -> QuantumGreenPrimary
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(260.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 130.dp),
                onClick = onClick
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = (size.minDimension / 2f) - 16.dp.toPx()

            // Dynamic amplitude expansion factor
            val dynamicBoost = when (state) {
                AssistantState.LISTENING, AssistantState.SPEAKING -> amplitude * 0.45f
                AssistantState.THINKING -> 0.15f
                else -> 0f
            }
            val coreScale = (breathingPulse + dynamicBoost).coerceIn(0.7f, 1.6f)

            // 1. Outermost telemetry ring with ticks
            val outerRadius = maxRadius * 0.95f
            drawCircle(
                color = primaryColor.copy(alpha = 0.25f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Radar sweep arc for listening & thinking
            if (state == AssistantState.LISTENING || state == AssistantState.THINKING) {
                rotate(degrees = radarAngle, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0.0f to Color.Transparent,
                            0.8f to primaryColor.copy(alpha = 0.05f),
                            1.0f to primaryColor.copy(alpha = 0.5f),
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = true,
                        topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                        size = Size(outerRadius * 2, outerRadius * 2)
                    )
                }
            }

            // Outer tick marks
            val tickCount = 24
            for (i in 0 until tickCount) {
                val angle = (i * (360f / tickCount)) * (PI.toFloat() / 180f)
                val isMajor = i % 6 == 0
                val tickLen = if (isMajor) 10.dp.toPx() else 4.dp.toPx()
                val r1 = outerRadius - tickLen
                val r2 = outerRadius
                val start = Offset(center.x + r1 * cos(angle), center.y + r1 * sin(angle))
                val end = Offset(center.x + r2 * cos(angle), center.y + r2 * sin(angle))
                drawLine(
                    color = primaryColor.copy(alpha = if (isMajor) 0.6f else 0.25f),
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                )
            }

            // 2. Quantum Orbital Rings (Inspired by Quantum Hacker atom logo in photo!)
            val orbitRadiusX = maxRadius * 0.78f
            val orbitRadiusY = maxRadius * 0.28f
            val orbitalAngles = listOf(0f, 60f, 120f)

            for ((idx, orbitBaseAngle) in orbitalAngles.withIndex()) {
                val rot = baseRotation * (if (idx % 2 == 0) 1f else -1f) + orbitBaseAngle
                rotate(degrees = rot, pivot = center) {
                    // Draw elliptical orbital path
                    val path = Path().apply {
                        addOval(
                            androidx.compose.ui.geometry.Rect(
                                center.x - orbitRadiusX,
                                center.y - orbitRadiusY,
                                center.x + orbitRadiusX,
                                center.y + orbitRadiusY
                            )
                        )
                    }
                    drawPath(
                        path = path,
                        color = primaryColor.copy(alpha = 0.45f),
                        style = Stroke(width = 1.8.dp.toPx())
                    )

                    // Draw orbiting quantum particle / electron
                    val electronPhase = (baseRotation * 2.5f + idx * 120f) * (PI.toFloat() / 180f)
                    val ex = center.x + orbitRadiusX * cos(electronPhase)
                    val ey = center.y + orbitRadiusY * sin(electronPhase)

                    // Electron glow
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.35f),
                        radius = 8.dp.toPx(),
                        center = Offset(ex, ey)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = Offset(ex, ey)
                    )
                }
            }

            // 3. Dynamic Waveform Ring (Visualizer for LISTENING and SPEAKING)
            if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) {
                val waveRadius = maxRadius * 0.55f * coreScale
                val wavePoints = 48
                val wavePath = Path()

                for (i in 0..wavePoints) {
                    val angle = (i * (360f / wavePoints)) * (PI.toFloat() / 180f)
                    // Periodic harmonic wave modulated by audio amplitude
                    val waveMod = sin((i * 4 + baseRotation * 0.1f)) * (amplitude * 18.dp.toPx())
                    val r = waveRadius + waveMod
                    val px = center.x + r * cos(angle)
                    val py = center.y + r * sin(angle)
                    if (i == 0) wavePath.moveTo(px, py) else wavePath.lineTo(px, py)
                }
                wavePath.close()

                drawPath(
                    path = wavePath,
                    color = primaryColor.copy(alpha = 0.7f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 4. Central Glowing Quantum Nucleus
            val nucleusRadius = 32.dp.toPx() * coreScale

            // Outer diffuse glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.65f),
                        primaryColor.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = nucleusRadius * 2.2f
                ),
                radius = nucleusRadius * 2.2f,
                center = center
            )

            // Inner solid core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryColor,
                        primaryColor.copy(alpha = 0.8f)
                    ),
                    center = center,
                    radius = nucleusRadius
                ),
                radius = nucleusRadius,
                center = center
            )

            // Inner quantum concentric ring
            drawCircle(
                color = Color.White.copy(alpha = 0.75f),
                radius = nucleusRadius * 0.45f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}
