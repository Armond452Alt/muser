package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.TextMuted
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    waveform: FloatArray,
    isActive: Boolean,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_idle_anim")
    val idlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(ObsidianDark, RoundedCornerShape(14.dp))
            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(14.dp))
            .padding(8.dp)
            .testTag("waveform_visualizer_container")
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .testTag("waveform_canvas")
        ) {
            val width = size.width
            val height = size.height
            val midY = height / 2f

            // Draw center grid line
            drawLine(
                color = Color.White.copy(alpha = 0.07f),
                start = Offset(0f, midY),
                end = Offset(width, midY),
                strokeWidth = 1.dp.toPx()
            )

            // If not active or muted, draw calm resting wave
            if (!isActive || isMuted || waveform.isEmpty()) {
                val idlePath = Path()
                val steps = 60
                val amp = if (!isActive) 2.dp.toPx() else 4.dp.toPx()
                for (s in 0..steps) {
                    val x = (s.toFloat() / steps) * width
                    val y = midY + sin(s * 0.15f + idlePhase) * amp
                    if (s == 0) idlePath.moveTo(x, y) else idlePath.lineTo(x, y)
                }
                drawPath(
                    path = idlePath,
                    color = CyberCyan.copy(alpha = 0.25f),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
                return@Canvas
            }

            // Draw live active audio waveform safely
            val points = waveform.size
            if (points < 2) return@Canvas

            val path = Path()
            val stepX = width / (points - 1)

            for (i in 0 until points) {
                val x = i * stepX
                val rawSample = waveform[i]
                val sample = if (rawSample.isNaN() || rawSample.isInfinite()) 0f else rawSample.coerceIn(-1.0f, 1.0f)
                val y = midY - (sample * (height * 0.42f))
                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    val prevX = (i - 1) * stepX
                    val prevRaw = waveform[i - 1]
                    val prevSample = if (prevRaw.isNaN() || prevRaw.isInfinite()) 0f else prevRaw.coerceIn(-1.0f, 1.0f)
                    val prevY = midY - (prevSample * (height * 0.42f))
                    val cx = (prevX + x) / 2f
                    val cy = (prevY + y) / 2f
                    path.quadraticTo(prevX, prevY, cx, cy)
                }
            }

            // Outer soft glow stroke
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(listOf(CyberCyan.copy(alpha = 0.3f), NeonGreen.copy(alpha = 0.3f))),
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Sharp core wave line
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(listOf(CyberCyan, NeonGreen, CyberCyan)),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Live oscilloscope label
        Text(
            text = "LIVE OSCILLOSCOPE",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        )
    }
}
