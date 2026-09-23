package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngineState
import com.example.audio.PttMode
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PttButton(
    state: AudioEngineState,
    onStartEngine: () -> Unit,
    onStopEngine: () -> Unit,
    onToggleMute: () -> Unit,
    onPttPressChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val isTransmitting = state.isRunning && !state.isMuted && state.isTransmitting
    val infiniteTransition = rememberInfiniteTransition(label = "ptt_pulse_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val buttonBgColor by animateColorAsState(
        targetValue = when {
            !state.isRunning -> Color(0xFF1E293B)
            state.isMuted -> Color(0xFF450A0A)
            isTransmitting -> Color(0xFF003822)
            else -> Color(0xFF0F172A)
        },
        label = "btn_bg_color"
    )

    val ringColor by animateColorAsState(
        targetValue = when {
            !state.isRunning -> ObsidianCardBorder
            state.isMuted -> AlertRed
            isTransmitting -> NeonGreen
            else -> CyberCyan
        },
        label = "btn_ring_color"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Circular Push-to-Talk / Mic Live control
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(200.dp)
                .testTag("ptt_button_container")
        ) {
            // Animated outer pulsing halo when transmitting
            if (isTransmitting) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    NeonGreen.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Outer ring
            Box(
                modifier = Modifier
                    .size(174.dp)
                    .clip(CircleShape)
                    .border(3.dp, ringColor, CircleShape)
                    .background(buttonBgColor)
                    .pointerInput(state.pttMode, state.isRunning) {
                        detectTapGestures(
                            onPress = {
                                try {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } catch (ignored: Throwable) {}

                                if (!state.isRunning) {
                                    onStartEngine()
                                } else {
                                    when (state.pttMode) {
                                        PttMode.ALWAYS_ON -> {
                                            onToggleMute()
                                        }
                                        PttMode.PUSH_TO_TALK -> {
                                            onPttPressChange(true)
                                            tryAwaitRelease()
                                            try {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            } catch (ignored: Throwable) {}
                                            onPttPressChange(false)
                                        }
                                        PttMode.TOGGLE_MUTE -> {
                                            onPttPressChange(!state.isTransmitting)
                                        }
                                    }
                                }
                            }
                        )
                    }
                    .testTag("ptt_circle_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        imageVector = when {
                            !state.isRunning -> Icons.Default.PlayArrow
                            state.isMuted -> Icons.Default.MicOff
                            isTransmitting -> Icons.Default.Mic
                            else -> Icons.Default.MicOff
                        },
                        contentDescription = "Microphone Status",
                        tint = when {
                            !state.isRunning -> TextSecondary
                            state.isMuted -> AlertRed
                            isTransmitting -> NeonGreen
                            else -> CyberCyan
                        },
                        modifier = Modifier.size(54.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = when {
                            !state.isRunning -> "START MIC"
                            state.isMuted -> "MUTED"
                            state.pttMode == PttMode.PUSH_TO_TALK -> if (isTransmitting) "TRANSMITTING" else "HOLD TO TALK"
                            state.pttMode == PttMode.TOGGLE_MUTE -> if (isTransmitting) "ON AIR" else "STANDBY"
                            else -> "MIC LIVE"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = when {
                            !state.isRunning -> "Tap to connect"
                            state.isMuted -> "Tap to unmute"
                            state.pttMode == PttMode.PUSH_TO_TALK -> if (isTransmitting) "Release to cut" else "Press & hold"
                            state.pttMode == PttMode.TOGGLE_MUTE -> "Tap to switch"
                            else -> "Tap to mute"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Action controls bar: Stop button & Mute button
        if (state.isRunning) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Mute / Unmute pill
                Surface(
                    onClick = {
                        try {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } catch (ignored: Throwable) {}
                        onToggleMute()
                    },
                    shape = RoundedCornerShape(24.dp),
                    color = if (state.isMuted) AlertRed.copy(alpha = 0.2f) else ObsidianSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (state.isMuted) AlertRed else ObsidianCardBorder
                    ),
                    modifier = Modifier.testTag("quick_mute_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (state.isMuted) AlertRed else NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (state.isMuted) "UNMUTE MIC" else "MUTE MIC",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isMuted) AlertRed else TextPrimary
                        )
                    }
                }

                // Disconnect / Stop button
                Surface(
                    onClick = {
                        try {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } catch (ignored: Throwable) {}
                        onStopEngine()
                    },
                    shape = RoundedCornerShape(24.dp),
                    color = ObsidianSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier.testTag("power_off_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "STOP",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
