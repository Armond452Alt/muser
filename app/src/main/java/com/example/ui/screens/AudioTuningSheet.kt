package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngineState
import com.example.audio.AudioOutputRoute
import com.example.audio.LatencyMode
import com.example.audio.NoiseGateMode
import com.example.audio.PttMode
import com.example.audio.VoiceProfile
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTuningSheet(
    state: AudioEngineState,
    onDismiss: () -> Unit,
    onUpdateGain: (Float) -> Unit,
    onUpdateRoute: (AudioOutputRoute) -> Unit,
    onUpdateGate: (NoiseGateMode) -> Unit,
    onUpdateLatency: (LatencyMode) -> Unit,
    onUpdateProfile: (VoiceProfile) -> Unit,
    onUpdatePttMode: (PttMode) -> Unit,
    onUpdatePttChime: (Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianDark,
        dragHandle = null,
        modifier = Modifier.testTag("audio_tuning_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Audio & Hardware Routing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianSurfaceVariant
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            HorizontalDivider(color = ObsidianCardBorder)

            // Section 1: Audio Destination (Console AUX vs Phone Speakers)
            SectionHeader(
                icon = Icons.Default.Cable,
                title = "AUDIO OUTPUT ROUTING",
                subtitle = "Select where microphone audio is sent"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AudioOutputRoute.values().forEach { route ->
                    val isSelected = state.outputRoute == route
                    OptionCard(
                        title = route.title,
                        subtitle = route.subtitle,
                        isSelected = isSelected,
                        onClick = { onUpdateRoute(route) },
                        testTag = "route_option_${route.name.lowercase()}"
                    )
                }
            }

            HorizontalDivider(color = ObsidianCardBorder)

            // Section 2: Mic Preamp Gain / Volume Boost
            SectionHeader(
                icon = Icons.Default.VolumeUp,
                title = "MIC PREAMP GAIN (+dB)",
                subtitle = "Boost phone microphone level to match console headset input"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, ObsidianCardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gain Boost",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "${if (state.gainDb >= 0) "+" else ""}${String.format(Locale.US, "%.1f", state.gainDb)} dB",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            state.gainDb > 16f -> AlertRed
                            state.gainDb > 8f -> WarningAmber
                            else -> CyberCyan
                        }
                    )
                }

                Slider(
                    value = state.gainDb,
                    onValueChange = onUpdateGain,
                    valueRange = -12f..24f,
                    steps = 35,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberCyan,
                        inactiveTrackColor = ObsidianSurfaceVariant
                    ),
                    modifier = Modifier.testTag("gain_slider")
                )

                // Quick preset buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0f to "0dB (Flat)", 6f to "+6dB (PS5/Xbox)", 12f to "+12dB (Boost)", 18f to "+18dB (Max)").forEach { (db, label) ->
                        val isCurrent = (state.gainDb.roundToInt() == db.roundToInt())
                        Surface(
                            onClick = { onUpdateGain(db) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCurrent) CyberCyan.copy(alpha = 0.2f) else ObsidianSurfaceVariant,
                            border = BorderStroke(1.dp, if (isCurrent) CyberCyan else ObsidianCardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCurrent) CyberCyan else TextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                maxLines = 1,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = ObsidianCardBorder)

            // Section 3: Smart Noise Gate
            SectionHeader(
                icon = Icons.Default.Hearing,
                title = "SMART NOISE GATE",
                subtitle = "Blocks controller button clicks, thumbstick snaps & fan noise"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NoiseGateMode.values().forEach { gate ->
                    val isSelected = state.noiseGateMode == gate
                    val desc = when (gate) {
                        NoiseGateMode.OFF -> "Open mic at all times with no gate cutoff"
                        NoiseGateMode.LOW -> "Cuts faint room hum and gentle breathing (-48 dB)"
                        NoiseGateMode.MEDIUM -> "Recommended: Cuts loud DualSense/Xbox thumbsticks & fan (-38 dB)"
                        NoiseGateMode.HIGH -> "Aggressive: Cuts loud room chatter and heavy clicking (-28 dB)"
                    }
                    OptionCard(
                        title = gate.title,
                        subtitle = desc,
                        isSelected = isSelected,
                        onClick = { onUpdateGate(gate) },
                        testTag = "gate_option_${gate.name.lowercase()}"
                    )
                }
            }

            HorizontalDivider(color = ObsidianCardBorder)

            // Section 4: Voice Profile Equalizer
            SectionHeader(
                icon = Icons.Default.GraphicEq,
                title = "VOICE EQUALIZER PROFILE",
                subtitle = "Hardware DSP filters tailored for gaming communication"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VoiceProfile.values().forEach { profile ->
                    val isSelected = state.voiceProfile == profile
                    OptionCard(
                        title = profile.title,
                        subtitle = profile.description,
                        isSelected = isSelected,
                        onClick = { onUpdateProfile(profile) },
                        testTag = "profile_option_${profile.name.lowercase()}"
                    )
                }
            }

            HorizontalDivider(color = ObsidianCardBorder)

            // Section 5: Latency Mode
            SectionHeader(
                icon = Icons.Default.Speed,
                title = "AUDIO BUFFER & LATENCY MODE",
                subtitle = "Optimizes buffer size between Android audio pipeline and console"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LatencyMode.values().forEach { mode ->
                    val isSelected = state.latencyMode == mode
                    OptionCard(
                        title = mode.title,
                        subtitle = mode.description,
                        isSelected = isSelected,
                        onClick = { onUpdateLatency(mode) },
                        testTag = "latency_option_${mode.name.lowercase()}"
                    )
                }
            }

            HorizontalDivider(color = ObsidianCardBorder)

            // Section 6: Push-To-Talk Options
            SectionHeader(
                icon = Icons.Default.Mic,
                title = "TRANSMIT MODE (PTT)",
                subtitle = "Configure mic trigger behavior"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PttMode.values().forEach { mode ->
                    val isSelected = state.pttMode == mode
                    OptionCard(
                        title = mode.title,
                        subtitle = mode.description,
                        isSelected = isSelected,
                        onClick = { onUpdatePttMode(mode) },
                        testTag = "ptt_mode_option_${mode.name.lowercase()}"
                    )
                }

                // Radio chime toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ObsidianSurface, RoundedCornerShape(12.dp))
                        .border(1.dp, ObsidianCardBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tactical Comms Chime",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Plays radio transmit beep on start and stop",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = state.pttSoundChimeEnabled,
                        onCheckedChange = onUpdatePttChime,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberCyan,
                            checkedTrackColor = Color(0xFF00363D)
                        ),
                        modifier = Modifier.testTag("ptt_chime_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = CyberCyan,
                letterSpacing = 1.sp
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun OptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF002730) else ObsidianSurface,
        border = BorderStroke(1.dp, if (isSelected) CyberCyan else ObsidianCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(
                        if (isSelected) CyberCyan else Color.Transparent,
                        RoundedCornerShape(9.dp)
                    )
                    .border(
                        2.dp,
                        if (isSelected) CyberCyan else TextMuted,
                        RoundedCornerShape(9.dp)
                    )
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) CyberCyan else TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
