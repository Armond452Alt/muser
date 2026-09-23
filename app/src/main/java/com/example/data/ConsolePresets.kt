package com.example.data

data class ConsoleGuide(
    val id: String,
    val consoleName: String,
    val generationBadge: String,
    val connectionType: String,
    val steps: List<String>,
    val controllerPortImageHint: String,
    val recommendedGainDb: Float,
    val recommendedGate: String,
    val tips: List<String>
)

object ConsolePresets {

    val guides = listOf(
        ConsoleGuide(
            id = "ps5",
            consoleName = "PlayStation 5",
            generationBadge = "DualSense",
            connectionType = "3.5mm AUX Cable to Controller Jack",
            steps = listOf(
                "Plug one end of standard 3.5mm AUX male-to-male cable into your phone's headphone jack (or USB-C to 3.5mm adapter).",
                "Plug the other end into the 3.5mm headset jack at the bottom of your PS5 DualSense controller.",
                "On PS5, press the PS button -> Select the [Mic] icon in the Quick Menu.",
                "Set Input Device to 'Controller Headset' (or USB Headset).",
                "Select 'Adjust Microphone Level' and talk into your phone until the PS5 meter hits the Good / Target range."
            ),
            controllerPortImageHint = "Bottom port between L/R thumbsticks",
            recommendedGainDb = 6f,
            recommendedGate = "Medium (-38dB)",
            tips = listOf(
                "Keep PS5 controller mic mute button unlit (lit orange means hardware muted).",
                "If using phone while charging, use a ground loop isolator to eliminate hum."
            )
        ),
        ConsoleGuide(
            id = "ps4",
            consoleName = "PlayStation 4",
            generationBadge = "DualShock 4",
            connectionType = "3.5mm AUX Cable to DualShock 4",
            steps = listOf(
                "Connect the 3.5mm AUX cable between your phone and the DualShock 4 headset port (next to EXT).",
                "Hold PS Button -> Go to [Sound/Devices] -> [Output to Headphones].",
                "Select 'Chat Audio' if you only want party chat, or 'All Audio'.",
                "Go to Settings -> Devices -> Audio Devices -> 'Adjust Microphone Level'.",
                "Speak into phone mic and calibrate slider until audio peaks in the clear zone."
            ),
            controllerPortImageHint = "Bottom 3.5mm jack beside EXT connector",
            recommendedGainDb = 8f,
            recommendedGate = "Medium (-38dB)",
            tips = listOf(
                "Set output to 'Chat Audio' so game audio continues playing through your TV or Soundbar.",
                "Ensure Party Chat priority is set to 'Party Audio'."
            )
        ),
        ConsoleGuide(
            id = "xbox_series_one",
            consoleName = "Xbox Series X|S & Xbox One",
            generationBadge = "Xbox Wireless Controller",
            connectionType = "3.5mm AUX to Xbox Controller",
            steps = listOf(
                "Plug the 3.5mm AUX cable from your phone into the 3.5mm port on the bottom edge of your Xbox controller.",
                "Press the Xbox guide button on controller -> Navigate to bottom right [Audio & Music] icon.",
                "Verify 'Headset mic' toggle is turned ON.",
                "Adjust 'Headset volume' and 'Mic monitoring' sliders as desired.",
                "Open Xbox Party Chat and speak to verify your gamer gamertag circle lights up with speech."
            ),
            controllerPortImageHint = "Center bottom 3.5mm circular jack",
            recommendedGainDb = 6f,
            recommendedGate = "Medium (-38dB)",
            tips = listOf(
                "Older 1st gen Xbox One controllers without 3.5mm port require the Xbox One Stereo Headset Adapter.",
                "Turn off 'Mic monitoring' on Xbox if you hear your own voice echoing back."
            )
        ),
        ConsoleGuide(
            id = "ps3",
            consoleName = "PlayStation 3",
            generationBadge = "PS3 USB Adapter",
            connectionType = "USB to 3.5mm Audio Card / Dongle",
            steps = listOf(
                "PS3 does not have a 3.5mm jack on DualShock 3. Plug an inexpensive USB-to-3.5mm audio adapter into any front USB port on the PS3.",
                "Connect the 3.5mm AUX cable from phone to the Pink (Mic in) jack of the USB dongle.",
                "On PS3 XMB menu, go to [Settings] -> [Accessory Settings] -> [Audio Device Settings].",
                "Set Input Device to 'USB PnP Sound Device' (or your adapter name).",
                "Set Microphone Level to 3 or 4, talk into phone and verify voice bar bounces."
            ),
            controllerPortImageHint = "Front USB port on PS3 console with USB Audio Dongle",
            recommendedGainDb = 10f,
            recommendedGate = "Low (-48dB)",
            tips = listOf(
                "Output Device can remain 'System Default' so game audio plays on TV/HDMI while voice mic routes via USB."
            )
        ),
        ConsoleGuide(
            id = "xbox360",
            consoleName = "Xbox 360",
            generationBadge = "Xbox 360 Controller",
            connectionType = "2.5mm to 3.5mm Adapter to Controller",
            steps = listOf(
                "The Xbox 360 wireless controller features a 2.5mm sub-mini headset jack.",
                "Attach a 2.5mm male to 3.5mm female adapter to the Xbox 360 controller.",
                "Connect the 3.5mm AUX cable from your phone into the 3.5mm adapter.",
                "Open Xbox 360 Guide -> Settings -> Preferences -> Voice.",
                "Set Voice Through to 'Play Through Speakers' or 'Play Through Headset'.",
                "Join Xbox LIVE party and verify comms transmission."
            ),
            controllerPortImageHint = "Bottom center 2.5mm port between controller grips",
            recommendedGainDb = 8f,
            recommendedGate = "Medium (-38dB)",
            tips = listOf(
                "Make sure your 2.5mm to 3.5mm adapter supports microphone pinout (4-pole TRRS or 3-pole mono mic)."
            )
        ),
        ConsoleGuide(
            id = "pc_switch",
            consoleName = "PC / Mac / Nintendo Switch",
            generationBadge = "Multiplatform",
            connectionType = "3.5mm Combo Jack / Mic In",
            steps = listOf(
                "Connect 3.5mm AUX cable from phone to the PC/Laptop 3.5mm Mic In (pink jack) or combo headset jack.",
                "On Windows, go to Sound Settings -> Input -> Choose 'Microphone / Line In'.",
                "On Nintendo Switch (in supported games like Fortnite), plug AUX cable directly into top 3.5mm headphone jack.",
                "Set Party Voice Chat to Open Mic."
            ),
            controllerPortImageHint = "3.5mm Mic In port or Combo Headset jack",
            recommendedGainDb = 4f,
            recommendedGate = "Low (-48dB)",
            tips = listOf(
                "On PC, ensure 'Listen to this device' is unchecked in Windows sound control panel to avoid feedback."
            )
        )
    )

    val hardwareTips = listOf(
        "TRRS vs TRS Cables: Most console controllers use the 4-pole CTIA standard (Tip = Left, Ring1 = Right, Ring2 = Ground, Sleeve = Mic). If using a 3-pole TRS cable directly, use a CTIA Y-splitter cable to properly route into the Mic line.",
        "Ground Loop Buzz Elimination: If you charge your phone via wall adapter or console USB while the AUX cable is connected, you may hear a 60Hz hum. A $5 Ground Loop Isolator completely eliminates all hum!",
        "Zero Latency: Phone hardware DSP processes audio in under 6ms. For competitive gaming (Call of Duty, Halo, Fortnite), set Latency Mode to 'Ultra-Low' in Tuning settings."
    )
}
