package com.example.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class SoundboardClip(val title: String, val icon: String, val category: String) {
    AIRHORN("MLG Airhorn", "🎺", "Fun"),
    GG_FANFARE("Victory / GG", "🏆", "Gaming"),
    LEVEL_UP("Level Up", "⭐", "Gaming"),
    BUZZER("Fail Buzzer", "❌", "Fun"),
    LASER("Laser Blast", "⚡", "FX"),
    WHISTLE("Tactical Whistle", "📢", "Alert"),
    DRUM_HIT("Ba-Dum Tss", "🥁", "Fun"),
    CALIBRATION_1KHZ("1kHz Test Tone", "🔊", "Utility")
}

object SoundGenerator {

    /**
     * Generates PCM 16-bit mono short array for a given sound effect at the requested sample rate.
     */
    fun generateClip(clip: SoundboardClip, sampleRate: Int = 48000): ShortArray {
        return when (clip) {
            SoundboardClip.AIRHORN -> generateAirhorn(sampleRate)
            SoundboardClip.GG_FANFARE -> generateGgFanfare(sampleRate)
            SoundboardClip.LEVEL_UP -> generateLevelUp(sampleRate)
            SoundboardClip.BUZZER -> generateBuzzer(sampleRate)
            SoundboardClip.LASER -> generateLaser(sampleRate)
            SoundboardClip.WHISTLE -> generateWhistle(sampleRate)
            SoundboardClip.DRUM_HIT -> generateDrumHit(sampleRate)
            SoundboardClip.CALIBRATION_1KHZ -> generateSineTone(1000.0, 1.0, 0.6, sampleRate)
        }
    }

    /**
     * Short radio chirp for PTT start
     */
    fun generatePttChime(isOpening: Boolean, sampleRate: Int = 48000): ShortArray {
        val duration = 0.06 // 60ms
        val totalSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(totalSamples)
        val startFreq = if (isOpening) 800.0 else 1800.0
        val endFreq = if (isOpening) 1800.0 else 800.0

        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val freq = startFreq + (endFreq - startFreq) * progress
            val envelope = sin(progress * PI)
            val sample = sin(2 * PI * freq * i / sampleRate) * envelope * 0.4
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateSineTone(freq: Double, durationSec: Double, amplitude: Double, sampleRate: Int): ShortArray {
        val totalSamples = (durationSec * sampleRate).toInt()
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            // Envelope with slight 5ms fade in/out to avoid clicking
            val fadeSamples = (0.01 * sampleRate).toInt()
            val env = when {
                i < fadeSamples -> i.toDouble() / fadeSamples
                i > totalSamples - fadeSamples -> (totalSamples - i).toDouble() / fadeSamples
                else -> 1.0
            }
            val sample = sin(2 * PI * freq * i / sampleRate) * amplitude * env
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateAirhorn(sampleRate: Int): ShortArray {
        // Classic airhorn: fundamental ~470 Hz, 580 Hz, 700 Hz disharmony with rapid pulses
        val duration = 0.75
        val totalSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(totalSamples)

        // Three quick bursts
        val pulseLength = totalSamples / 3
        for (i in 0 until totalSamples) {
            val pulseIndex = i % pulseLength
            val pulseProgress = pulseIndex.toDouble() / pulseLength
            if (pulseProgress > 0.85) {
                buffer[i] = 0
                continue
            }
            val env = sin(pulseProgress / 0.85 * PI)
            val t = i.toDouble() / sampleRate
            val tone1 = sin(2 * PI * 466.16 * t) // Bb4
            val tone2 = sin(2 * PI * 587.33 * t) // D5
            val tone3 = sin(2 * PI * 698.46 * t) // F5
            val mixed = (tone1 * 0.4 + tone2 * 0.35 + tone3 * 0.25) * env * 0.75
            buffer[i] = (mixed * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateGgFanfare(sampleRate: Int): ShortArray {
        // Notes: C5 (523), E5 (659), G5 (784), C6 (1046)
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        val noteDur = 0.12
        val finalNoteDur = 0.35
        val totalSamples = ((3 * noteDur + finalNoteDur) * sampleRate).toInt()
        val buffer = ShortArray(totalSamples)

        var sampleOffset = 0
        for (idx in notes.indices) {
            val dur = if (idx == notes.size - 1) finalNoteDur else noteDur
            val count = (dur * sampleRate).toInt()
            val freq = notes[idx]
            for (i in 0 until count) {
                val progress = i.toDouble() / count
                val env = 1.0 - (progress * 0.6)
                val sample = sin(2 * PI * freq * i / sampleRate) * env * 0.65
                if (sampleOffset + i < totalSamples) {
                    buffer[sampleOffset + i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
            sampleOffset += count
        }
        return buffer
    }

    private fun generateLevelUp(sampleRate: Int): ShortArray {
        val notes = doubleArrayOf(440.0, 554.37, 659.25, 880.0)
        val noteDur = 0.09
        val totalSamples = (notes.size * noteDur * sampleRate).toInt()
        val buffer = ShortArray(totalSamples)

        var offset = 0
        for (freq in notes) {
            val count = (noteDur * sampleRate).toInt()
            for (i in 0 until count) {
                val progress = i.toDouble() / count
                val env = sin(progress * PI)
                val sample = (sin(2 * PI * freq * i / sampleRate) + 0.3 * sin(4 * PI * freq * i / sampleRate)) * env * 0.6
                if (offset + i < totalSamples) {
                    buffer[offset + i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
            offset += count
        }
        return buffer
    }

    private fun generateBuzzer(sampleRate: Int): ShortArray {
        val duration = 0.4
        val totalSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(totalSamples)
        val freq = 130.81 // Low C3 square wave buzz
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val env = if (progress < 0.9) 1.0 else (1.0 - progress) * 10
            val phase = (i * freq / sampleRate) % 1.0
            val square = if (phase < 0.5) 0.5 else -0.5
            val sample = square * env * 0.7
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateLaser(sampleRate: Int): ShortArray {
        val duration = 0.25
        val totalSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val freq = 2400.0 * exp(- progress * 3.5) + 200.0
            val env = 1.0 - progress
            val sample = sin(2 * PI * freq * i / sampleRate) * env * 0.6
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateWhistle(sampleRate: Int): ShortArray {
        val duration = 0.35
        val totalSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val freq = if (progress < 0.5) 2600.0 else 2200.0
            val env = sin(progress * PI)
            val tremolo = 1.0 + 0.2 * sin(2 * PI * 35.0 * i / sampleRate)
            val sample = sin(2 * PI * freq * i / sampleRate) * env * tremolo * 0.55
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateDrumHit(sampleRate: Int): ShortArray {
        val duration = 0.5
        val totalSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(totalSamples)
        // Snare pop followed by short crash
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / totalSamples
            val noise = (Math.random() * 2.0 - 1.0)
            val tone = sin(2 * PI * 180.0 * exp(-t * 15.0) * i / sampleRate)
            val env = exp(-progress * 6.0)
            val sample = (tone * 0.5 + noise * 0.5) * env * 0.7
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }
}
