package com.pocketpet.core.system.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class VoiceListeningState {
    data object Idle : VoiceListeningState()
    data object Listening : VoiceListeningState()
    data class Recognized(val heardText: String) : VoiceListeningState()
    data class Error(val message: String) : VoiceListeningState()
}

/**
 * A push-to-talk wrapper around [SpeechRecognizer]. Listening only ever starts from an explicit
 * [startListening] call triggered by a user tap — there is no continuous or background
 * recognition anywhere in this class, and no raw audio is ever written to disk; only the final
 * recognized text (delivered by the OS) is exposed, and even that never leaves the device.
 */
class VoiceCommandController(private val appContext: Context) {

    private val _state = MutableStateFlow<VoiceListeningState>(VoiceListeningState.Idle)
    val state: StateFlow<VoiceListeningState> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun startListening() {
        if (!isAvailable()) {
            _state.value = VoiceListeningState.Error("Speech recognition isn't available on this device.")
            return
        }
        releaseRecognizer()
        val newRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
        recognizer = newRecognizer
        newRecognizer.setRecognitionListener(listener)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        _state.value = VoiceListeningState.Listening
        newRecognizer.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
    }

    fun cancel() {
        releaseRecognizer()
        _state.value = VoiceListeningState.Idle
    }

    private fun releaseRecognizer() {
        recognizer?.setRecognitionListener(null)
        recognizer?.destroy()
        recognizer = null
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = VoiceListeningState.Listening
        }

        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            _state.value = VoiceListeningState.Error(errorMessageFor(error))
        }

        override fun onResults(results: Bundle?) {
            val heard = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            _state.value = if (heard != null) {
                VoiceListeningState.Recognized(heard)
            } else {
                VoiceListeningState.Error("Didn't catch that.")
            }
        }

        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun errorMessageFor(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy, try again."
        SpeechRecognizer.ERROR_AUDIO -> "Microphone error."
        else -> "Couldn't recognize speech."
    }
}
