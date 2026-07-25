package com.shiji.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Voice recognition states emitted while listening.
 */
sealed interface VoiceState {
    data object Ready : VoiceState
    data object Listening : VoiceState
    data object Processing : VoiceState
    data class Volume(val level: Float) : VoiceState          // 0.0 ~ 1.0
    data class Partial(val text: String) : VoiceState
    data class Final(val text: String) : VoiceState
    data class Error(val message: String) : VoiceState
}

/**
 * Flow-based wrapper around Android's native SpeechRecognizer (zh-CN).
 * All SpeechRecognizer calls are marshalled to the main thread, as required.
 */
class SpeechRecognizerManager(private val context: Context) {

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun listen(): Flow<VoiceState> = callbackFlow {
        val mainHandler = Handler(Looper.getMainLooper())
        var recognizer: SpeechRecognizer? = null

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { trySend(VoiceState.Ready) }
            override fun onBeginningOfSpeech() { trySend(VoiceState.Listening) }
            override fun onRmsChanged(rmsdB: Float) {
                trySend(VoiceState.Volume(((rmsdB + 2f) / 12f).coerceIn(0f, 1f)))
            }
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.let { trySend(VoiceState.Partial(it)) }
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty()
                trySend(VoiceState.Final(text))
                close()
            }
            override fun onError(error: Int) {
                trySend(VoiceState.Error(errorMessage(error)))
                close()
            }
            override fun onEndOfSpeech() { trySend(VoiceState.Processing) }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        mainHandler.post {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(listener)
                startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                })
            }
        }

        awaitClose {
            mainHandler.post {
                runCatching { recognizer?.stopListening() }
                runCatching { recognizer?.destroy() }
                recognizer = null
            }
        }
    }

    private fun errorMessage(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "录音出错，请重试"
        SpeechRecognizer.ERROR_CLIENT -> "识别服务出错，请重试"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "缺少录音权限"
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络异常，语音识别需要联网"
        SpeechRecognizer.ERROR_NO_MATCH -> "没有听清，请再说一次"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别服务忙，请稍候"
        SpeechRecognizer.ERROR_SERVER -> "识别服务不可用，请稍后再试"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没有检测到说话，请按住后说话"
        else -> "语音识别失败 ($code)"
    }
}
