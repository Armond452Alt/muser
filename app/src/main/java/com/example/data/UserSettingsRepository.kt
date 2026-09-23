package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.audio.AudioOutputRoute
import com.example.audio.LatencyMode
import com.example.audio.NoiseGateMode
import com.example.audio.PttMode
import com.example.audio.VoiceProfile

class UserSettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("muser_settings", Context.MODE_PRIVATE)

    var gainDb: Float
        get() = prefs.getFloat("gain_db", 6.0f)
        set(value) = prefs.edit().putFloat("gain_db", value).apply()

    var outputRoute: AudioOutputRoute
        get() {
            val name = prefs.getString("output_route", AudioOutputRoute.AUX_CABLE.name) ?: AudioOutputRoute.AUX_CABLE.name
            return try {
                AudioOutputRoute.valueOf(name)
            } catch (e: Exception) {
                AudioOutputRoute.AUX_CABLE
            }
        }
        set(value) = prefs.edit().putString("output_route", value.name).apply()

    var latencyMode: LatencyMode
        get() {
            val name = prefs.getString("latency_mode", LatencyMode.BALANCED.name) ?: LatencyMode.BALANCED.name
            return try {
                LatencyMode.valueOf(name)
            } catch (e: Exception) {
                LatencyMode.BALANCED
            }
        }
        set(value) = prefs.edit().putString("latency_mode", value.name).apply()

    var voiceProfile: VoiceProfile
        get() {
            val name = prefs.getString("voice_profile", VoiceProfile.NATURAL.name) ?: VoiceProfile.NATURAL.name
            return try {
                VoiceProfile.valueOf(name)
            } catch (e: Exception) {
                VoiceProfile.NATURAL
            }
        }
        set(value) = prefs.edit().putString("voice_profile", value.name).apply()

    var noiseGateMode: NoiseGateMode
        get() {
            val name = prefs.getString("noise_gate_mode", NoiseGateMode.MEDIUM.name) ?: NoiseGateMode.MEDIUM.name
            return try {
                NoiseGateMode.valueOf(name)
            } catch (e: Exception) {
                NoiseGateMode.MEDIUM
            }
        }
        set(value) = prefs.edit().putString("noise_gate_mode", value.name).apply()

    var pttMode: PttMode
        get() {
            val name = prefs.getString("ptt_mode", PttMode.ALWAYS_ON.name) ?: PttMode.ALWAYS_ON.name
            return try {
                PttMode.valueOf(name)
            } catch (e: Exception) {
                PttMode.ALWAYS_ON
            }
        }
        set(value) = prefs.edit().putString("ptt_mode", value.name).apply()

    var pttSoundChimeEnabled: Boolean
        get() = prefs.getBoolean("ptt_chime_enabled", true)
        set(value) = prefs.edit().putBoolean("ptt_chime_enabled", value).apply()
}
