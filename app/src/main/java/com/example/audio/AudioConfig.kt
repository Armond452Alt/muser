package com.example.audio

enum class AudioOutputRoute(val title: String, val subtitle: String) {
    AUX_CABLE("Console AUX Out", "Audio routed to 3.5mm AUX / Controller Mic in"),
    PHONE_SPEAKER("Phone Speakers", "Audio plays out loud on phone speakers"),
    DUAL_OUTPUT("Dual Monitor", "Streams to AUX cable and monitors on Phone Speaker")
}

enum class LatencyMode(val title: String, val targetBufferFrames: Int, val description: String) {
    ULTRA_LOW("Ultra-Low Latency", 128, "~3–6 ms buffer. Ideal for fast twitch party chat"),
    BALANCED("Balanced", 256, "~10–14 ms buffer. Great compatibility & low CPU"),
    STABLE("Safe / High-Stability", 512, "~20–25 ms buffer. Zero chance of pops/glitches")
}

enum class VoiceProfile(val title: String, val description: String) {
    NATURAL("Clean & Natural", "Unfiltered crystal-clear voice passthrough"),
    CONSOLE_PRO("Console Pro Comms", "Enhanced speech clarity, cuts 80Hz rumble and boosts 3kHz"),
    TACTICAL_RADIO("Tactical Comms", "Walkie-talkie radio filter with comms crunch"),
    DEEP_COMMANDER("Deep Commander", "Rich low-end warmth and broadcast presence"),
    MEGAPHONE("Megaphone / PA", "Compressed high-gain megaphone projection")
}

enum class NoiseGateMode(val title: String, val thresholdDb: Float) {
    OFF("Gate Off", -100f),
    LOW("Gentle (-48 dB)", -48f),
    MEDIUM("Medium (-38 dB)", -38f),
    HIGH("Aggressive (-28 dB)", -28f)
}

enum class PttMode(val title: String, val description: String) {
    ALWAYS_ON("Open Mic", "Continuously streams voice when unmuted"),
    PUSH_TO_TALK("Push-To-Talk (Hold)", "Hold down button while speaking"),
    TOGGLE_MUTE("Toggle Transmit", "Tap button to start or pause transmission")
}

data class AudioEngineState(
    val isRunning: Boolean = false,
    val isMuted: Boolean = false,
    val isTransmitting: Boolean = true, // for PTT
    val outputRoute: AudioOutputRoute = AudioOutputRoute.AUX_CABLE,
    val latencyMode: LatencyMode = LatencyMode.BALANCED,
    val voiceProfile: VoiceProfile = VoiceProfile.NATURAL,
    val gainDb: Float = 6f, // +6dB default for console controllers
    val noiseGateMode: NoiseGateMode = NoiseGateMode.MEDIUM,
    val pttMode: PttMode = PttMode.ALWAYS_ON,
    val isAuxPlugged: Boolean = false,
    val currentDb: Float = -60f,
    val peakDb: Float = -60f,
    val estimatedLatencyMs: Int = 8,
    val pttSoundChimeEnabled: Boolean = true,
    val sampleRate: Int = 48000
)
