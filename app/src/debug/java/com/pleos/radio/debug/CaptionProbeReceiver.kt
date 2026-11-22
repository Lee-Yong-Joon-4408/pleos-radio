package com.pleos.radio.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.pleos.radio.data.translate.OnDeviceTranslator
import com.pleos.radio.data.translate.WhisperNative
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Debug-only probe: `adb shell am broadcast -n com.pleos.radio.debug/com.pleos.radio.debug.CaptionProbeReceiver
 *   --es text "..." --es from en --es to ko` translates on-device and logs the result (tag CaptionProbe).
 */
class CaptionProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        intentSeconds = intent.getIntExtra("seconds", 0)
        intentThreads = intent.getIntExtra("threads", 0)
        intent.getStringExtra("whisper")?.let { model -> benchWhisper(context, model, intent.getBooleanExtra("translate", false)); return }
        val text = intent.getStringExtra("text") ?: "Good evening, this is the news from London."
        val from = intent.getStringExtra("from") ?: "en"
        val to = intent.getStringExtra("to") ?: "ko"
        // Return immediately (model downloads can take a minute); the app must be in the foreground.
        scope.launch {
            val started = System.currentTimeMillis()
            try {
                val out = translator.translate(text, from, to)
                Log.i(TAG, "OK $from→$to in ${System.currentTimeMillis() - started} ms: $out")
            } catch (e: Exception) {
                Log.e(TAG, "FAILED $from→$to: ${e.javaClass.simpleName}: ${e.message}", e)
            }
        }
    }

    /** `--es whisper ggml-base-q5_1.bin [--ez translate true]`: transcribes files/models/jfk.wav and logs timing. */
    private fun benchWhisper(context: Context, model: String, translate: Boolean) {
        scope.launch(Dispatchers.Default) {
            val dir = java.io.File(context.filesDir, "models")
            val t0 = System.currentTimeMillis()
            val handle = WhisperNative.init(java.io.File(dir, model).absolutePath)
            val t1 = System.currentTimeMillis()
            if (handle == 0L) { Log.e(TAG, "whisper load failed: $model"); return@launch }
            val wav = java.io.File(dir, "jfk.wav").readBytes()
            val pcm = java.nio.ByteBuffer.wrap(wav, 44, wav.size - 44).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            val all = FloatArray(pcm.remaining()) { pcm.get(it) / 32768f }
            // Optional --ei seconds N: benchmark a live-sized chunk instead of the whole file.
            val seconds = intentSeconds
            val samples = if (seconds > 0) all.copyOf(minOf(all.size, seconds * 16_000)) else all
            val threads = intentThreads.takeIf { it > 0 } ?: Runtime.getRuntime().availableProcessors()
            val t2 = System.currentTimeMillis()
            val text = WhisperNative.transcribe(handle, samples, "en", translate, threads)
            val t3 = System.currentTimeMillis()
            WhisperNative.free(handle)
            Log.i(TAG, "WHISPER $model audio=${samples.size / 16} ms load=${t1 - t0} ms run=${t3 - t2} ms threads=$threads: $text")
            Log.i(TAG, "WHISPER sys: ${WhisperNative.systemInfo()}")
        }
    }

    private companion object {
        @Volatile var intentSeconds = 0
        @Volatile var intentThreads = 0
        const val TAG = "CaptionProbe"
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val translator = OnDeviceTranslator()
    }
}
