package com.pocketpet.core.domain.voice

import com.google.common.truth.Truth.assertThat
import com.pocketpet.core.model.VoiceCommand
import org.junit.Test

class VoiceCommandParserTest {

    private val parser = VoiceCommandParser()

    @Test
    fun `recognizes torch on and off`() {
        assertThat(parser.parse("torch on")).isEqualTo(VoiceCommand.TorchOn)
        assertThat(parser.parse("Torch Off")).isEqualTo(VoiceCommand.TorchOff)
        assertThat(parser.parse("flashlight on please")).isEqualTo(VoiceCommand.TorchOn)
    }

    @Test
    fun `recognizes camera, wifi, and bluetooth commands`() {
        assertThat(parser.parse("open camera")).isEqualTo(VoiceCommand.OpenCamera)
        assertThat(parser.parse("open wifi")).isEqualTo(VoiceCommand.OpenWifi)
        assertThat(parser.parse("open wi-fi settings")).isEqualTo(VoiceCommand.OpenWifi)
        assertThat(parser.parse("open bluetooth")).isEqualTo(VoiceCommand.OpenBluetooth)
    }

    @Test
    fun `recognizes battery, time, and volume commands`() {
        assertThat(parser.parse("battery")).isEqualTo(VoiceCommand.Battery)
        assertThat(parser.parse("check battery")).isEqualTo(VoiceCommand.Battery)
        assertThat(parser.parse("time")).isEqualTo(VoiceCommand.Time)
        assertThat(parser.parse("what time is it")).isEqualTo(VoiceCommand.Time)
        assertThat(parser.parse("volume up")).isEqualTo(VoiceCommand.VolumeUp)
        assertThat(parser.parse("volume down")).isEqualTo(VoiceCommand.VolumeDown)
    }

    @Test
    fun `parses open app commands with the spoken name`() {
        val command = parser.parse("open Spotify")
        assertThat(command).isInstanceOf(VoiceCommand.OpenApp::class.java)
        assertThat((command as VoiceCommand.OpenApp).spokenName).isEqualTo("spotify")
    }

    @Test
    fun `is case-insensitive and trims whitespace`() {
        assertThat(parser.parse("   BATTERY   ")).isEqualTo(VoiceCommand.Battery)
    }

    @Test
    fun `unmatched text becomes Unrecognized rather than a guess`() {
        val command = parser.parse("tell me a joke")
        assertThat(command).isInstanceOf(VoiceCommand.Unrecognized::class.java)
        assertThat((command as VoiceCommand.Unrecognized).heardText).isEqualTo("tell me a joke")
    }

    @Test
    fun `empty text becomes Unrecognized`() {
        assertThat(parser.parse("")).isInstanceOf(VoiceCommand.Unrecognized::class.java)
        assertThat(parser.parse("   ")).isInstanceOf(VoiceCommand.Unrecognized::class.java)
    }
}
