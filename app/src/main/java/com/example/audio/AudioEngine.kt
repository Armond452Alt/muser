package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.Process
import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tanh

private const val TAG = "MuserAudioEngine"

class AudioEngine private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var audioThread: Thread? = null

    private val isRunning = AtomicBoolean(false)
    private val isMuted = AtomicBoolean(false)
    private val isTransmitting = AtomicBoolean(true)

    var state = AudioEngineState()
        private set

    // Soundboard queue
    private val soundQueue = ConcurrentLinkedQueue<ShortArray>()
    private var currentSoundBuffer: ShortArray? = null
    private var currentSoundIndex = 0

    // Callback for live UI meter & oscilloscope
    var onMetricsListener: ((currentDb: Float, peakDb: Float, waveform: FloatArray, latencyMs: Int) -> Unit)? = null

    // Native device properties
    private val nativeSampleRate: Int = run {
        try {
            val rateStr = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            rateStr?.toIntOrNull() ?: 48000
        } catch (e: Exception) {
            48000
        }
    }

    private val nativeFramesPerBuffer: Int = run {
        try {
            val framesStr = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
            framesStr?.toIntOrNull() ?: 256
        } catch (e: Exception) {
            256
        }
    }

    init {
        state = state.copy(
            sampleRate = nativeSampleRate,
            estimatedLatencyMs = calculateLatencyMs(state.latencyMode, nativeSampleRate)
        )
    }

    companion object {
        @Volatile
        private var instance: AudioEngine? = null

        fun getInstance(context: Context): AudioEngine {
            return instance ?: synchronized(this) {
                instance ?: AudioEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    fun updateConfig(
        gainDb: Float = state.gainDb,
        noiseGateMode: NoiseGateMode = state.noiseGateMode,
        voiceProfile: VoiceProfile = state.voiceProfile,
        latencyMode: LatencyMode = state.latencyMode,
        outputRoute: AudioOutputRoute = state.outputRoute,
        pttMode: PttMode = state.pttMode,
        pttSoundChimeEnabled: Boolean = state.pttSoundChimeEnabled
    ) {
        val needsRestart = latencyMode != state.latencyMode || outputRoute != state.outputRoute

        state = state.copy(
            gainDb = gainDb,
            noiseGateMode = noiseGateMode,
            voiceProfile = voiceProfile,
            latencyMode = latencyMode,
            outputRoute = outputRoute,
            pttMode = pttMode,
            pttSoundChimeEnabled = pttSoundChimeEnabled,
            estimatedLatencyMs = calculateLatencyMs(latencyMode, state.sampleRate)
        )

        if (needsRestart && isRunning.get()) {
            restart()
        } else {
            applyRouting(state.outputRoute)
        }
    }

    fun setMuted(muted: Boolean) {
        isMuted.set(muted)
        state = state.copy(isMuted = muted)
    }

    fun setTransmitting(transmitting: Boolean) {
        val wasTransmitting = isTransmitting.getAndSet(transmitting)
        state = state.copy(isTransmitting = transmitting)

        if (state.pttSoundChimeEnabled && wasTransmitting != transmitting) {
            playPttChime(transmitting)
        }
    }

    fun setAuxPlugged(plugged: Boolean) {
        state = state.copy(isAuxPlugged = plugged)
    }

    fun playSoundboardClip(clip: SoundboardClip) {
        try {
            val pcm = SoundGenerator.generateClip(clip, state.sampleRate)
            soundQueue.offer(pcm)
        } catch (e: Exception) {
            Log.w(TAG, "Error queuing soundboard clip", e)
        }
    }

    private fun playPttChime(isOpening: Boolean) {
        try {
            val pcm = SoundGenerator.generatePttChime(isOpening, state.sampleRate)
            soundQueue.offer(pcm)
        } catch (e: Exception) {
            Log.w(TAG, "Error queuing ptt chime", e)
        }
    }

    @Synchronized
    fun start(): Boolean {
        if (isRunning.get()) return true

        // Clean up any stale handles before starting
        stop()

        val candidateSampleRates = listOf(nativeSampleRate, 48000, 44100, 16000).distinct()
        var initializedRecord: AudioRecord? = null
        var initializedTrack: AudioTrack? = null
        var chosenSampleRate = nativeSampleRate
        var chosenBufferFrames = 256

        for (candidateRate in candidateSampleRates) {
            val minRecordSize = AudioRecord.getMinBufferSize(
                candidateRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val minTrackSize = AudioTrack.getMinBufferSize(
                candidateRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            if (minRecordSize <= 0 || minTrackSize <= 0) {
                continue
            }

            val framesPerBuffer = when (state.latencyMode) {
                LatencyMode.ULTRA_LOW -> max(128, nativeFramesPerBuffer)
                LatencyMode.BALANCED -> max(256, nativeFramesPerBuffer * 2)
                LatencyMode.STABLE -> max(512, nativeFramesPerBuffer * 4)
            }

            val recordBufferSize = max(minRecordSize * 2, framesPerBuffer * 4)
            val trackBufferSize = max(minTrackSize * 2, framesPerBuffer * 4)

            // Try building AudioRecord
            val recordSources = listOf(
                MediaRecorder.AudioSource.MIC,
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                MediaRecorder.AudioSource.DEFAULT
            )

            var testRecord: AudioRecord? = null
            for (source in recordSources) {
                try {
                    val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        AudioRecord.Builder()
                            .setAudioSource(source)
                            .setAudioFormat(
                                AudioFormat.Builder()
                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                    .setSampleRate(candidateRate)
                                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                                    .build()
                            )
                            .setBufferSizeInBytes(recordBufferSize)
                            .build()
                    } else {
                        @Suppress("DEPRECATION")
                        AudioRecord(
                            source,
                            candidateRate,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            recordBufferSize
                        )
                    }

                    if (r.state == AudioRecord.STATE_INITIALIZED) {
                        testRecord = r
                        break
                    } else {
                        try { r.release() } catch (ignored: Throwable) {}
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Failed source $source on rate $candidateRate", e)
                }
            }

            if (testRecord == null) continue

            // Try building AudioTrack
            var testTrack: AudioTrack? = null
            try {
                val usage = when (state.outputRoute) {
                    AudioOutputRoute.PHONE_SPEAKER -> AudioAttributes.USAGE_MEDIA
                    AudioOutputRoute.AUX_CABLE -> AudioAttributes.USAGE_MEDIA
                    AudioOutputRoute.DUAL_OUTPUT -> AudioAttributes.USAGE_MEDIA
                }

                val attributes = AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                val t = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
                        .setAudioAttributes(attributes)
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(candidateRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(trackBufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        candidateRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        trackBufferSize,
                        AudioTrack.MODE_STREAM
                    )
                }

                if (t.state == AudioTrack.STATE_INITIALIZED) {
                    testTrack = t
                } else {
                    try { t.release() } catch (ignored: Throwable) {}
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed AudioTrack on rate $candidateRate", e)
            }

            if (testTrack == null) {
                try { testRecord.release() } catch (ignored: Throwable) {}
                continue
            }

            // Both initialized successfully!
            initializedRecord = testRecord
            initializedTrack = testTrack
            chosenSampleRate = candidateRate
            chosenBufferFrames = framesPerBuffer
            break
        }

        if (initializedRecord == null || initializedTrack == null) {
            Log.e(TAG, "Could not initialize AudioRecord and AudioTrack with any candidate sample rate")
            return false
        }

        try {
            applyRouting(state.outputRoute)

            initializedRecord.startRecording()
            initializedTrack.play()

            audioRecord = initializedRecord
            audioTrack = initializedTrack
            isRunning.set(true)
            state = state.copy(
                isRunning = true,
                sampleRate = chosenSampleRate,
                estimatedLatencyMs = calculateLatencyMs(state.latencyMode, chosenSampleRate)
            )

            startProcessingLoop(chosenBufferFrames)
            return true
        } catch (e: Throwable) {
            Log.e(TAG, "Error launching audio streaming loop", e)
            try { initializedRecord.stop() } catch (ignored: Throwable) {}
            try { initializedRecord.release() } catch (ignored: Throwable) {}
            try { initializedTrack.stop() } catch (ignored: Throwable) {}
            try { initializedTrack.release() } catch (ignored: Throwable) {}
            stop()
            return false
        }
    }

    private fun applyRouting(route: AudioOutputRoute) {
        try {
            when (route) {
                AudioOutputRoute.PHONE_SPEAKER -> {
                    audioManager.mode = AudioManager.MODE_NORMAL
                    audioManager.isSpeakerphoneOn = true
                }
                AudioOutputRoute.AUX_CABLE -> {
                    audioManager.mode = AudioManager.MODE_NORMAL
                    audioManager.isSpeakerphoneOn = false
                }
                AudioOutputRoute.DUAL_OUTPUT -> {
                    audioManager.mode = AudioManager.MODE_NORMAL
                    audioManager.isSpeakerphoneOn = true
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to apply audio routing", e)
        }
    }

    private fun startProcessingLoop(chunkFrames: Int) {
        val thread = Thread({
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            } catch (ignored: Throwable) {}

            val record = audioRecord ?: return@Thread
            val track = audioTrack ?: return@Thread

            val safeChunk = max(64, chunkFrames)
            val buffer = ShortArray(safeChunk)
            val waveformSamples = FloatArray(32)

            var prevSample = 0.0
            var bandpassY1 = 0.0
            var bandpassY2 = 0.0
            var gateSmoothedGain = 1.0f
            var peakHold = -60f
            var lastMetricsTime = 0L

            while (isRunning.get()) {
                val samplesRead = try {
                    record.read(buffer, 0, buffer.size)
                } catch (e: Throwable) {
                    -1
                }
                if (samplesRead <= 0) continue

                // 1. Calculate input RMS and Peak dB safely
                var sumSquares = 0.0
                var currentPeak = 0
                for (i in 0 until samplesRead) {
                    val sample = buffer[i].toInt()
                    sumSquares += sample * sample
                    val absVal = abs(sample)
                    if (absVal > currentPeak) {
                        currentPeak = absVal
                    }
                }
                val rms = sqrt(sumSquares / samplesRead)
                val currentDb = safeDb(rms)
                val peakDb = safeDb(currentPeak.toDouble())

                // Smooth peak hold decay
                peakHold = max(peakDb, peakHold - 0.4f)

                // 2. Check Noise Gate
                val gateThreshold = state.noiseGateMode.thresholdDb
                val targetGateGain = if (currentDb < gateThreshold) 0.0f else 1.0f
                val gateCoeff = if (targetGateGain > gateSmoothedGain) 0.35f else 0.05f
                gateSmoothedGain += (targetGateGain - gateSmoothedGain) * gateCoeff

                // 3. Mute & PTT check
                val isSilenced = isMuted.get() || !isTransmitting.get()
                val masterGain = if (isSilenced) 0.0f else gateSmoothedGain

                // 4. Preamp Gain calculation
                val linearGain = 10.0.pow(state.gainDb.toDouble() / 20.0)

                // 5. Soundboard mixing
                if (currentSoundBuffer == null && !soundQueue.isEmpty()) {
                    currentSoundBuffer = soundQueue.poll()
                    currentSoundIndex = 0
                }

                // 6. Process each sample with DSP + Filters
                for (i in 0 until samplesRead) {
                    var input = buffer[i].toDouble()

                    // Apply Voice Profile DSP safely
                    when (state.voiceProfile) {
                        VoiceProfile.NATURAL -> {}
                        VoiceProfile.CONSOLE_PRO -> {
                            val hp = input - prevSample * 0.95
                            prevSample = input
                            input = hp * 1.25
                        }
                        VoiceProfile.TACTICAL_RADIO -> {
                            val filtered = 0.5 * (input - bandpassY2)
                            bandpassY2 = bandpassY1
                            bandpassY1 = filtered
                            input = tanh(filtered / 12000.0) * 16000.0
                        }
                        VoiceProfile.DEEP_COMMANDER -> {
                            val bass = (input + prevSample) * 0.5
                            prevSample = input
                            input = input * 0.8 + bass * 0.6
                        }
                        VoiceProfile.MEGAPHONE -> {
                            input = tanh(input / 6000.0) * 22000.0
                        }
                    }

                    if (input.isNaN() || input.isInfinite()) input = 0.0

                    val amplified = input * linearGain * masterGain

                    var soundSample = 0.0
                    val sBuf = currentSoundBuffer
                    if (sBuf != null) {
                        if (currentSoundIndex < sBuf.size) {
                            soundSample = sBuf[currentSoundIndex].toDouble()
                            currentSoundIndex++
                        } else {
                            currentSoundBuffer = null
                            currentSoundIndex = 0
                        }
                    }

                    val total = amplified + soundSample

                    // Soft saturation limiter
                    val normalized = total / Short.MAX_VALUE
                    val limited = if (abs(normalized) > 0.8) {
                        tanh(normalized) * Short.MAX_VALUE
                    } else {
                        total
                    }

                    val safeLimited = if (limited.isNaN() || limited.isInfinite()) 0.0 else limited
                    buffer[i] = safeLimited.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                // 7. Write to output track
                try {
                    track.write(buffer, 0, samplesRead)
                } catch (e: Throwable) {
                    Log.w(TAG, "AudioTrack write error", e)
                }

                // 8. Extract waveform downsampled for UI (32 points)
                val now = System.currentTimeMillis()
                if (now - lastMetricsTime > 30) {
                    lastMetricsTime = now
                    val step = max(1, samplesRead / 32)
                    for (w in 0 until 32) {
                        val idx = (w * step).coerceIn(0, samplesRead - 1)
                        val sampleF = buffer[idx].toFloat() / Short.MAX_VALUE
                        waveformSamples[w] = if (sampleF.isNaN() || sampleF.isInfinite()) 0f else sampleF.coerceIn(-1.0f, 1.0f)
                    }

                    val latency = calculateLatencyMs(state.latencyMode, state.sampleRate)
                    onMetricsListener?.invoke(currentDb, peakHold, waveformSamples.clone(), latency)
                }
            }
        }, "MuserAudioThread")

        audioThread = thread
        thread.start()
    }

    private fun safeDb(value: Double): Float {
        if (value <= 0.0001 || value.isNaN() || value.isInfinite()) return -60f
        val db = (20.0 * log10(value / Short.MAX_VALUE.toDouble())).toFloat()
        return if (db.isNaN() || db.isInfinite()) -60f else db.coerceIn(-60f, 0f)
    }

    @Synchronized
    fun stop() {
        isRunning.set(false)
        state = state.copy(isRunning = false, currentDb = -60f, peakDb = -60f)

        try {
            audioThread?.join(200)
        } catch (ignored: Throwable) {}
        audioThread = null

        try {
            audioRecord?.stop()
        } catch (ignored: Throwable) {}
        try {
            audioRecord?.release()
        } catch (ignored: Throwable) {}
        audioRecord = null

        try {
            audioTrack?.stop()
        } catch (ignored: Throwable) {}
        try {
            audioTrack?.release()
        } catch (ignored: Throwable) {}
        audioTrack = null

        try {
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
        } catch (ignored: Throwable) {}
    }

    fun restart() {
        stop()
        start()
    }

    private fun calculateLatencyMs(mode: LatencyMode, sampleRate: Int): Int {
        val frames = mode.targetBufferFrames
        val rate = if (sampleRate > 0) sampleRate else 48000
        val ms = (frames.toDouble() / rate * 1000.0).toInt()
        return ms + 4
    }
}
