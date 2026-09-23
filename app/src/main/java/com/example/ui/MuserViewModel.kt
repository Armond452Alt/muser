package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.audio.AudioEngine
import com.example.audio.AudioEngineState
import com.example.audio.AudioOutputRoute
import com.example.audio.LatencyMode
import com.example.audio.NoiseGateMode
import com.example.audio.PttMode
import com.example.audio.SoundboardClip
import com.example.audio.VoiceProfile
import com.example.data.UserSettingsRepository
import com.example.service.AudioRoutingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "MuserViewModel"

class MuserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserSettingsRepository(application)
    private val engine: AudioEngine = AudioEngine.getInstance(application)

    private val _uiState = MutableStateFlow(
        AudioEngineState(
            gainDb = repository.gainDb,
            outputRoute = repository.outputRoute,
            latencyMode = repository.latencyMode,
            voiceProfile = repository.voiceProfile,
            noiseGateMode = repository.noiseGateMode,
            pttMode = repository.pttMode,
            pttSoundChimeEnabled = repository.pttSoundChimeEnabled
        )
    )
    val uiState: StateFlow<AudioEngineState> = _uiState.asStateFlow()

    private val _waveform = MutableStateFlow(FloatArray(32))
    val waveform: StateFlow<FloatArray> = _waveform.asStateFlow()

    private var audioService: AudioRoutingService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? AudioRoutingService.LocalBinder
            audioService = binder?.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            isBound = false
        }
    }

    init {
        try {
            val intent = Intent(application, AudioRoutingService::class.java)
            application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (t: Throwable) {
            Log.w(TAG, "Could not bind AudioRoutingService", t)
        }

        setupEngineCallbacks()
    }

    private fun setupEngineCallbacks() {
        engine.updateConfig(
            gainDb = repository.gainDb,
            noiseGateMode = repository.noiseGateMode,
            voiceProfile = repository.voiceProfile,
            latencyMode = repository.latencyMode,
            outputRoute = repository.outputRoute,
            pttMode = repository.pttMode,
            pttSoundChimeEnabled = repository.pttSoundChimeEnabled
        )

        engine.onMetricsListener = { currentDb, peakDb, wave, latencyMs ->
            _waveform.value = wave
            _uiState.value = _uiState.value.copy(
                currentDb = currentDb,
                peakDb = peakDb,
                estimatedLatencyMs = latencyMs,
                isAuxPlugged = engine.state.isAuxPlugged
            )
        }
    }

    fun startAudio(): Boolean {
        return try {
            val started = engine.start()
            if (started) {
                _uiState.value = engine.state
                try {
                    audioService?.startForegroundWithNotification()
                } catch (t: Throwable) {
                    Log.w(TAG, "Foreground service notification could not be shown (audio continues)", t)
                }
            }
            started
        } catch (t: Throwable) {
            Log.e(TAG, "Error starting audio session", t)
            false
        }
    }

    fun stopAudio() {
        try {
            engine.stop()
            _uiState.value = engine.state
            try {
                audioService?.stopForegroundNotification()
            } catch (t: Throwable) {
                Log.w(TAG, "Error stopping foreground notification", t)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error stopping audio session", t)
        }
    }

    fun toggleMute() {
        try {
            val newMute = !engine.state.isMuted
            engine.setMuted(newMute)
            _uiState.value = engine.state
            try {
                audioService?.updateNotification()
            } catch (ignored: Throwable) {}
        } catch (t: Throwable) {
            Log.e(TAG, "Error toggling mute", t)
        }
    }

    fun setPttTransmitting(transmitting: Boolean) {
        try {
            engine.setTransmitting(transmitting)
            _uiState.value = engine.state
        } catch (t: Throwable) {
            Log.e(TAG, "Error updating PTT transmission state", t)
        }
    }

    fun updateGain(gainDb: Float) {
        repository.gainDb = gainDb
        engine.updateConfig(gainDb = gainDb)
        _uiState.value = engine.state
    }

    fun updateOutputRoute(route: AudioOutputRoute) {
        repository.outputRoute = route
        engine.updateConfig(outputRoute = route)
        _uiState.value = engine.state
        try {
            audioService?.updateNotification()
        } catch (ignored: Throwable) {}
    }

    fun updateNoiseGate(gate: NoiseGateMode) {
        repository.noiseGateMode = gate
        engine.updateConfig(noiseGateMode = gate)
        _uiState.value = engine.state
    }

    fun updateLatencyMode(mode: LatencyMode) {
        repository.latencyMode = mode
        engine.updateConfig(latencyMode = mode)
        _uiState.value = engine.state
    }

    fun updateVoiceProfile(profile: VoiceProfile) {
        repository.voiceProfile = profile
        engine.updateConfig(voiceProfile = profile)
        _uiState.value = engine.state
    }

    fun updatePttMode(pttMode: PttMode) {
        repository.pttMode = pttMode
        engine.updateConfig(pttMode = pttMode)
        _uiState.value = engine.state
    }

    fun updatePttChime(enabled: Boolean) {
        repository.pttSoundChimeEnabled = enabled
        engine.updateConfig(pttSoundChimeEnabled = enabled)
        _uiState.value = engine.state
    }

    fun playSoundboardClip(clip: SoundboardClip) {
        try {
            engine.playSoundboardClip(clip)
        } catch (t: Throwable) {
            Log.w(TAG, "Error playing soundboard clip", t)
        }
    }

    override fun onCleared() {
        if (isBound) {
            try {
                getApplication<Application>().unbindService(serviceConnection)
            } catch (ignored: Throwable) {}
            isBound = false
        }
        super.onCleared()
    }
}
