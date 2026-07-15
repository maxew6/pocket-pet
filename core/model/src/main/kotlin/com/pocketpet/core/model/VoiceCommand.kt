package com.pocketpet.core.model

/**
 * The closed set of commands [VoiceCommandParser][com.pocketpet.core.domain.voice.VoiceCommandParser]
 * can produce from recognized speech. Push-to-talk only, always parsed locally, never sent
 * anywhere — this is a command grammar, not a conversational assistant.
 */
sealed class VoiceCommand {
    data object TorchOn : VoiceCommand()
    data object TorchOff : VoiceCommand()
    data object OpenCamera : VoiceCommand()
    data object OpenWifi : VoiceCommand()
    data object OpenBluetooth : VoiceCommand()
    data object Battery : VoiceCommand()
    data object Time : VoiceCommand()
    data object VolumeUp : VoiceCommand()
    data object VolumeDown : VoiceCommand()
    data class OpenApp(val spokenName: String) : VoiceCommand()
    data class Unrecognized(val heardText: String) : VoiceCommand()
}
