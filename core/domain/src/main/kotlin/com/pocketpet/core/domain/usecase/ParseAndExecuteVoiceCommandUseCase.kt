package com.pocketpet.core.domain.usecase

import com.pocketpet.core.domain.action.InstalledAppResolver
import com.pocketpet.core.domain.action.PetActionExecutor
import com.pocketpet.core.domain.voice.VoiceCommandParser
import com.pocketpet.core.model.ActionResult
import com.pocketpet.core.model.SystemAction
import com.pocketpet.core.model.VoiceCommand

/**
 * Turns one recognized speech result into a [VoiceCommand] via the deterministic
 * [VoiceCommandParser], then executes it. "Open [app]" is resolved to an installed app through
 * [InstalledAppResolver]; every other command maps 1:1 to a [SystemAction].
 */
class ParseAndExecuteVoiceCommandUseCase(
    private val parser: VoiceCommandParser,
    private val executor: PetActionExecutor,
    private val appResolver: InstalledAppResolver,
) {
    suspend operator fun invoke(heardText: String): ActionResult {
        return when (val command = parser.parse(heardText)) {
            VoiceCommand.TorchOn -> executor.execute(SystemAction.TorchOn)
            VoiceCommand.TorchOff -> executor.execute(SystemAction.TorchOff)
            VoiceCommand.OpenCamera -> executor.execute(SystemAction.OpenCamera)
            VoiceCommand.OpenWifi -> executor.execute(SystemAction.OpenWifiSettings)
            VoiceCommand.OpenBluetooth -> executor.execute(SystemAction.OpenBluetoothSettings)
            VoiceCommand.Battery -> executor.execute(SystemAction.ShowBattery)
            VoiceCommand.Time -> executor.execute(SystemAction.ShowTime)
            VoiceCommand.VolumeUp -> executor.execute(SystemAction.VolumeUp)
            VoiceCommand.VolumeDown -> executor.execute(SystemAction.VolumeDown)
            is VoiceCommand.OpenApp -> {
                val resolved = appResolver.resolveByName(command.spokenName)
                if (resolved != null) {
                    executor.execute(SystemAction.LaunchSelectedApp, resolved)
                } else {
                    ActionResult.NoCompatibleApplication(
                        "I couldn't find an app called \"${command.spokenName}\".",
                    )
                }
            }
            is VoiceCommand.Unrecognized -> ActionResult.Unsupported(
                "I didn't understand that command.",
            )
        }
    }
}
