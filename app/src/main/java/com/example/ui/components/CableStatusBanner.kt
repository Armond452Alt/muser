package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngineState
import com.example.audio.AudioOutputRoute
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun CableStatusBanner(
    state: AudioEngineState,
    onSwitchToSpeaker: () -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Status Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when {
                        state.outputRoute == AudioOutputRoute.PHONE_SPEAKER -> CyberCyan.copy(alpha = 0.12f)
                        state.isAuxPlugged -> NeonGreen.copy(alpha = 0.12f)
                        else -> WarningAmber.copy(alpha = 0.14f)
                    },
                    RoundedCornerShape(12.dp)
                )
                .border(
                    1.dp,
                    when {
                        state.outputRoute == AudioOutputRoute.PHONE_SPEAKER -> CyberCyan.copy(alpha = 0.4f)
                        state.isAuxPlugged -> NeonGreen.copy(alpha = 0.4f)
                        else -> WarningAmber.copy(alpha = 0.4f)
                    },
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .testTag("cable_status_banner")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = when {
                        state.outputRoute == AudioOutputRoute.PHONE_SPEAKER -> Icons.Default.VolumeUp
                        state.isAuxPlugged -> Icons.Default.CheckCircle
                        else -> Icons.Default.Cable
                    },
                    contentDescription = null,
                    tint = when {
                        state.outputRoute == AudioOutputRoute.PHONE_SPEAKER -> CyberCyan
                        state.isAuxPlugged -> NeonGreen
                        else -> WarningAmber
                    },
                    modifier = Modifier.size(24.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            state.outputRoute == AudioOutputRoute.PHONE_SPEAKER -> "Phone Speaker Output Active"
                            state.isAuxPlugged -> "AUX Cable Connected to Console"
                            else -> "No AUX Cable Detected"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = when {
                            state.outputRoute == AudioOutputRoute.PHONE_SPEAKER -> "Voice audio is played through the built-in phone speaker"
                            state.isAuxPlugged -> "Sending low-latency mic line-in to PS3/4/5 or Xbox controller"
                            else -> "Plug 3.5mm cable into PS/Xbox controller or switch to Phone Speakers"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                if (state.outputRoute == AudioOutputRoute.AUX_CABLE && !state.isAuxPlugged) {
                    Surface(
                        onClick = onSwitchToSpeaker,
                        shape = RoundedCornerShape(16.dp),
                        color = ObsidianSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("switch_to_speaker_button")
                    ) {
                        Text(
                            text = "Speaker",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Live Audio Specs Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpecChip(
                icon = Icons.Default.Speed,
                label = "Latency",
                value = "${state.estimatedLatencyMs}ms",
                tint = NeonGreen,
                modifier = Modifier.weight(1f)
            )
            SpecChip(
                icon = Icons.Default.GraphicEq,
                label = "Profile",
                value = state.voiceProfile.title.split(" ").first(),
                tint = CyberCyan,
                modifier = Modifier.weight(1f)
            )
            SpecChip(
                icon = Icons.Default.VolumeUp,
                label = "Boost",
                value = "+${state.gainDb.toInt()}dB",
                tint = WarningAmber,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SpecChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = ObsidianSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                Text(text = value, style = MaterialTheme.typography.labelMedium, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}
