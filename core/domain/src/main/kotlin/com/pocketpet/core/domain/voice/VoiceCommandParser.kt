package com.pocketpet.core.domain.voice

import com.pocketpet.core.model.VoiceCommand

/**
 * Turns recognized speech text into a [VoiceCommand] using fixed keyword rules — deterministic,
 * offline, and fully unit-testable. This is a command grammar, not a language model: unmatched
 * text always becomes [VoiceCommand.Unrecognized], never a guess.
 */
class VoiceCommandParser {

    fun parse(heardText: String): VoiceCommand {
        val normalized = heardText.trim().lowercase()
        if (normalized.isEmpty()) return VoiceCommand.Unrecognized(heardText)

        return when {
            normalized.contains("torch on") || normalized.contains("flashlight on") ||
                normalized.contains("light on") ->
                VoiceCommand.TorchOn

            normalized.contains("torch off") || normalized.contains("flashlight off") ||
                normalized.contains("light off") ->
                VoiceCommand.TorchOff

            normalized.contains("open camera") || normalized == "camera" ->
                VoiceCommand.OpenCamera

            normalized.contains("open wifi") || normalized.contains("open wi-fi") ||
                normalized.contains("open wi fi") ->
                VoiceCommand.OpenWifi

            normalized.contains("open bluetooth") || normalized == "bluetooth" ->
                VoiceCommand.OpenBluetooth

            normalized == "battery" || normalized.contains("battery level") ||
                normalized.contains("check battery") ->
                VoiceCommand.Battery

            normalized == "time" || normalized.contains("what time") ||
                normalized.contains("show time") ->
                VoiceCommand.Time

            normalized.contains("volume up") || normalized.contains("louder") ->
                VoiceCommand.VolumeUp

            normalized.contains("volume down") || normalized.contains("quieter") ->
                VoiceCommand.VolumeDown

            normalized.startsWith("open ") ->
                VoiceCommand.OpenApp(spokenName = normalized.removePrefix("open ").trim())

            else -> VoiceCommand.Unrecognized(heardText)
        }
    }
}
