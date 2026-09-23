package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VuGreen
import com.example.ui.theme.VuRed
import com.example.ui.theme.VuYellow
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun VuMeter(
    currentDb: Float,
    peakDb: Float,
    gateThresholdDb: Float,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedDb by animateFloatAsState(
        targetValue = if (isMuted) -60f else currentDb,
        animationSpec = tween(durationMillis = 60),
        label = "vu_animated_db"
    )

    val animatedPeak by animateFloatAsState(
        targetValue = if (isMuted) -60f else peakDb,
        animationSpec = tween(durationMillis = 100),
        label = "vu_animated_peak"
    )

    // Normalize from -60 dB to 0 dB -> 0.0 to 1.0
    val normalizedDb = ((animatedDb + 60f) / 60f).coerceIn(0f, 1f)
    val normalizedPeak = ((animatedPeak + 60f) / 60f).coerceIn(0f, 1f)
    val normalizedGate = ((gateThresholdDb + 60f) / 60f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(ObsidianSurface, RoundedCornerShape(14.dp))
            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("vu_meter_container")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header: dB readout and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "INPUT LEVEL",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    if (isMuted) {
                        Text(
                            text = "[MUTED]",
                            style = MaterialTheme.typography.labelSmall,
                            color = AlertRed,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (animatedDb > gateThresholdDb) {
                        Text(
                            text = "● GATE OPEN",
                            style = MaterialTheme.typography.labelSmall,
                            color = VuGreen,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "○ GATE CLOSED",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Text(
                    text = if (isMuted) "-∞ dB" else "${String.format(Locale.US, "%.1f", animatedDb)} dB",
                    style = MaterialTheme.typography.titleSmall,
                    color = when {
                        isMuted -> TextMuted
                        animatedDb > -6f -> AlertRed
                        animatedDb > -18f -> VuYellow
                        else -> CyberCyan
                    },
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // LED Segmented Bar Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .testTag("vu_meter_canvas")
            ) {
                val totalWidth = size.width
                val totalHeight = size.height
                val segmentCount = 36
                val gap = 3.dp.toPx()
                val segmentWidth = (totalWidth - (segmentCount - 1) * gap) / segmentCount

                for (i in 0 until segmentCount) {
                    val segmentProgress = (i + 1).toFloat() / segmentCount
                    val isLit = segmentProgress <= normalizedDb

                    // Color gradient based on decibel level
                    val segmentColor = when {
                        segmentProgress > 0.88f -> VuRed // -7dB to 0dB (Clip / Hot)
                        segmentProgress > 0.65f -> VuYellow // -21dB to -7dB (Optimal console speech)
                        else -> VuGreen // -60dB to -21dB (Presence)
                    }

                    val drawColor = if (isLit && !isMuted) {
                        segmentColor
                    } else {
                        segmentColor.copy(alpha = 0.15f)
                    }

                    val xOffset = i * (segmentWidth + gap)
                    drawRoundRect(
                        color = drawColor,
                        topLeft = Offset(xOffset, 0f),
                        size = Size(segmentWidth, totalHeight),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }

                // Peak hold indicator line
                if (normalizedPeak > 0.05f && !isMuted) {
                    val peakX = (normalizedPeak * totalWidth).coerceIn(0f, totalWidth - 2.dp.toPx())
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(peakX, 0f),
                        size = Size(3.dp.toPx(), totalHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }

                // Gate threshold marker line
                if (gateThresholdDb > -90f) {
                    val gateX = (normalizedGate * totalWidth).coerceIn(0f, totalWidth)
                    drawLine(
                        color = CyberCyan.copy(alpha = 0.8f),
                        start = Offset(gateX, -2.dp.toPx()),
                        end = Offset(gateX, totalHeight + 2.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            // Scale labels (-60, -36, -18, -6, 0 dB)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "-60", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 10.sp)
                Text(text = "-36", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 10.sp)
                Text(text = "-18 (Good)", style = MaterialTheme.typography.bodySmall, color = VuGreen.copy(alpha = 0.8f), fontSize = 10.sp)
                Text(text = "-6", style = MaterialTheme.typography.bodySmall, color = VuYellow, fontSize = 10.sp)
                Text(text = "0 dB (Clip)", style = MaterialTheme.typography.bodySmall, color = VuRed, fontSize = 10.sp)
            }
        }
    }
}
