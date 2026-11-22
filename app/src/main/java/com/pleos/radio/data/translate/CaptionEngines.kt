package com.pleos.radio.data.translate

import com.pleos.radio.domain.caption.CaptionLanguages
import com.pleos.radio.domain.caption.WhisperModel
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/** Speech recognition + translation backend for live captions. */
interface CaptionEngine {
    /** [pcm16k]: 16 kHz mono PCM16. [speechLanguage]: BCP-47 broadcast language, e.g. "de-DE". */
    suspend fun recognize(pcm16k: ShortArray, speechLanguage: String): String
    suspend fun translate(text: String, source: String, target: String): String
    /** Frees heavy resources (models) while captions are not in use. */
    fun release() {}
}

/** Free and offline: whisper.cpp for speech, ML Kit for translation. */
class OnDeviceCaptionEngine(
    private val models: WhisperModelStore,
    private val translator: OnDeviceTranslator,
    private val modelProvider: () -> WhisperModel,
) : CaptionEngine {

    // whisper contexts are not thread-safe: all native calls go through one thread.
    private val whisperThread = Executors.newSingleThreadExecutor { r -> Thread(r, "whisper").apply { priority = Thread.NORM_PRIORITY - 1 } }
        .asCoroutineDispatcher()
    private var handle = 0L
    private var loaded: WhisperModel? = null

    override suspend fun recognize(pcm16k: ShortArray, speechLanguage: String): String = withContext(whisperThread) {
        val model = modelProvider()
        if (loaded != model) {
            freeNative()
            val file = models.file(model)
            check(models.isReady(model)) { "Speech model not downloaded" }
            handle = WhisperNative.init(file.absolutePath)
            check(handle != 0L) { "Could not load speech model" }
            loaded = model
        }
        val samples = FloatArray(pcm16k.size) { pcm16k[it] / 32768f }
        WhisperNative.transcribe(handle, samples, CaptionLanguages.translationCode(speechLanguage), false, THREADS)
    }

    override suspend fun translate(text: String, source: String, target: String): String =
        translator.translate(text, source, target)

    override fun release() {
        whisperThread.executor.execute { freeNative() }
    }

    private fun freeNative() {
        if (handle != 0L) WhisperNative.free(handle)
        handle = 0L
        loaded = null
    }

    private companion object {
        /** Two threads measured fastest on the Pleos emulator; more threads oversubscribe the CPU. */
        const val THREADS = 2
    }
}

/** Google Cloud Speech-to-Text + Translation (paid, needs an API key). */
class CloudCaptionEngine(private val client: GoogleCloudClient, private val keyProvider: () -> String) : CaptionEngine {
    override suspend fun recognize(pcm16k: ShortArray, speechLanguage: String): String =
        client.recognize(pcm16k, speechLanguage, keyProvider())

    override suspend fun translate(text: String, source: String, target: String): String =
        client.translate(text, source, target, keyProvider())
}
