package com.livetranslator.app.speech

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * SpeechToTextEngine processes PCM audio buffers captured from system audio playback
 * and emits recognized transcriptions.
 */
class SpeechToTextEngine(private val context: Context) {

    private val _transcriptFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val transcriptFlow: SharedFlow<String> = _transcriptFlow.asSharedFlow()

    private val audioBuffer = mutableListOf<Byte>()
    private val bufferSizeThreshold = 16000 * 2 * 3 // approx 3 seconds of 16kHz 16-bit mono audio

    /**
     * Process raw audio chunk received from AudioPlaybackCapture (AudioRecord)
     */
    suspend fun processAudioChunk(buffer: ByteArray, bytesRead: Int) {
        if (bytesRead <= 0) return

        for (i in 0 until bytesRead) {
            audioBuffer.add(buffer[i])
        }

        if (audioBuffer.size >= bufferSizeThreshold) {
            val chunkToProcess = audioBuffer.toByteArray()
            audioBuffer.clear()
            
            // Transcribe chunk using local or remote STT (e.g., Whisper API or local VAD + STT)
            val transcribedText = sendToSpeechRecognitionApi(chunkToProcess)
            if (transcribedText.isNotBlank()) {
                _transcriptFlow.emit(transcribedText)
            }
        }
    }

    private suspend fun sendToSpeechRecognitionApi(audioBytes: ByteArray): String {
        // Prototype simulation / Cloud API endpoint implementation site:
        // In full production, send WAV/PCM payload to OpenAI Whisper API, Groq Whisper API, or local whisper.cpp native binding.
        return ""
    }

    /**
     * Emit text directly (used when feeding transcribed speech manually or testing)
     */
    suspend fun emitSimulatedSpeech(text: String) {
        _transcriptFlow.emit(text)
    }
}
