package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.AudioOutputRoute
import com.example.audio.SoundboardClip
import com.example.ui.MuserViewModel
import com.example.ui.components.CableStatusBanner
import com.example.ui.components.PttButton
import com.example.ui.components.SoundboardSection
import com.example.ui.components.VuMeter
import com.example.ui.components.WaveformVisualizer
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMicScreen(
    viewModel: MuserViewModel,
    onNavigateToGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsState()
    val waveform by viewModel.waveform.collectAsState()

    var showTuningSheet by remember { mutableStateOf(false) }

    // Audio recording permission launcher
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        hasAudioPermission = recordAudioGranted
        if (recordAudioGranted) {
            val started = viewModel.startAudio()
            if (!started) {
                scope.launch {
                    snackbarHostState.showSnackbar("Unable to initialize mic. Please verify no other app is capturing audio.")
                }
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Microphone permission is required to stream audio.")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(CyberCyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .border(1.dp, CyberCyan, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "MUSER",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Console AUX Mic & Party Comms",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToGuide,
                        modifier = Modifier.testTag("open_guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = "Console Setup Guide",
                            tint = CyberCyan
                        )
                    }

                    IconButton(
                        onClick = { showTuningSheet = true },
                        modifier = Modifier.testTag("open_tuning_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Audio Tuning",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianDark
                )
            )
        },
        containerColor = ObsidianDark,
        modifier = modifier.fillMaxSize().testTag("main_mic_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Banner if not granted
            if (!hasAudioPermission) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, WarningAmber),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("permission_banner")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Microphone Permission Required",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )
                        Text(
                            text = "Muser needs microphone access to capture low-latency party chat voice and output via AUX to your console controller.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                        Button(
                            onClick = {
                                val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                permissionLauncher.launch(perms.toTypedArray())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("grant_permission_button")
                        ) {
                            Text(text = "Grant Mic Access", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Cable Status & Quick Routing Banner
            CableStatusBanner(
                state = uiState,
                onSwitchToSpeaker = {
                    viewModel.updateOutputRoute(AudioOutputRoute.PHONE_SPEAKER)
                },
                onOpenGuide = onNavigateToGuide
            )

            // Live VU Decibel Meter
            VuMeter(
                currentDb = uiState.currentDb,
                peakDb = uiState.peakDb,
                gateThresholdDb = uiState.noiseGateMode.thresholdDb,
                isMuted = uiState.isMuted
            )

            // Live Waveform Oscilloscope
            WaveformVisualizer(
                waveform = waveform,
                isActive = uiState.isRunning,
                isMuted = uiState.isMuted
            )

            // Primary Push-To-Talk / Transmit Button
            PttButton(
                state = uiState,
                onStartEngine = {
                    if (hasAudioPermission) {
                        val started = viewModel.startAudio()
                        if (!started) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Unable to start microphone. Please check if another app is using the mic."
                                )
                            }
                        }
                    } else {
                        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionLauncher.launch(perms.toTypedArray())
                    }
                },
                onStopEngine = {
                    viewModel.stopAudio()
                },
                onToggleMute = {
                    viewModel.toggleMute()
                },
                onPttPressChange = { transmitting ->
                    viewModel.setPttTransmitting(transmitting)
                }
            )

            // Hardware Latency & Voice Profile Summary Card
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.dp, ObsidianCardBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("stats_summary_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ESTIMATED LATENCY",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "~${uiState.estimatedLatencyMs} ms (${uiState.latencyMode.title})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.estimatedLatencyMs < 20) NeonGreen else CyberCyan
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "ACTIVE PROFILE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = uiState.voiceProfile.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Quick Soundboard Section
            SoundboardSection(
                onPlayClip = { clip ->
                    viewModel.playSoundboardClip(clip)
                }
            )

            // Footer info
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Muser Engine v1.0 • Realtime PCM Audio Passthrough",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }

    if (showTuningSheet) {
        AudioTuningSheet(
            state = uiState,
            onDismiss = { showTuningSheet = false },
            onUpdateGain = { viewModel.updateGain(it) },
            onUpdateRoute = { viewModel.updateOutputRoute(it) },
            onUpdateGate = { viewModel.updateNoiseGate(it) },
            onUpdateLatency = { viewModel.updateLatencyMode(it) },
            onUpdateProfile = { viewModel.updateVoiceProfile(it) },
            onUpdatePttMode = { viewModel.updatePttMode(it) },
            onUpdatePttChime = { viewModel.updatePttChime(it) }
        )
    }
}
