package com.pleos.radio.presentation

import android.util.Log
import com.pleos.radio.audio.PcmTap
import com.pleos.radio.data.translate.CaptionEngine
import com.pleos.radio.data.translate.CloudException
import com.pleos.radio.data.translate.WhisperModelStore
import com.pleos.radio.domain.Station
import com.pleos.radio.domain.caption.CaptionBuffer
import com.pleos.radio.domain.caption.CaptionEngineType
import com.pleos.radio.domain.caption.CaptionLanguage
import com.pleos.radio.domain.caption.CaptionLanguages
import com.pleos.radio.domain.caption.CaptionState
import com.pleos.radio.domain.caption.CaptionStatus
import com.pleos.radio.domain.caption.PcmChunker
import com.pleos.radio.domain.caption.Transcripts
import com.pleos.radio.domain.caption.WhisperModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Live captions for foreign-language stations: audio chunk → speech recognition → translation → caption text.
 * Runs on-device (whisper.cpp + ML Kit, free) or on Google Cloud. Only the newest pending chunk is kept,
 * so captions stay close to live even when recognition is slower than real time.
 */
class LiveTranslator(
    private val engines: Map<CaptionEngineType, CaptionEngine>,
    private val tap: PcmTap,
    scope: CoroutineScope,
) {
    data class Config(
        val station: Station? = null,
        val playing: Boolean = false,
        val target: CaptionLanguage = CaptionLanguage.KOREAN,
        val engine: CaptionEngineType = CaptionEngineType.ON_DEVICE,
        val apiKey: String = "",
        val model: WhisperModel = WhisperModel.BASE,
        val modelState: WhisperModelStore.State = WhisperModelStore.State.Missing,
        /** On/off for transcribe-only stations (FM / AM). */
        val transcriptEnabled: Boolean = true,
    ) {
        val speechLanguage: String? get() = station?.speechLanguage
        val transcribeOnly: Boolean get() = station?.translateCaptions == false
        /** Captions switched on: a target language for translated stations, the transcript switch for FM / AM. */
        val captionsOn: Boolean get() = if (transcribeOnly) transcriptEnabled else target != CaptionLanguage.OFF
        private val engineReady: Boolean
            get() = when (engine) {
                CaptionEngineType.CLOUD -> apiKey.isNotBlank()
                CaptionEngineType.ON_DEVICE -> modelState == WhisperModelStore.State.Ready
            }
        val active: Boolean get() = playing && speechLanguage != null && captionsOn && engineReady
    }

    private val _state = MutableStateFlow(CaptionState())
    val state: StateFlow<CaptionState> = _state.asStateFlow()

    @Volatile private var config = Config()
    private val buffer = CaptionBuffer()
    private val chunks = Channel<ShortArray>(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        tap.onChunk = { chunks.trySend(it) }
        scope.launch { for (pcm in chunks) process(pcm) }
    }

    /** Call on the main thread whenever station, play state, language, engine, key or model state changes. */
    fun update(next: Config) {
        val previous = config
        config = next
        if (previous.station?.id != next.station?.id || previous.target != next.target || previous.engine != next.engine ||
            previous.transcriptEnabled != next.transcriptEnabled
        ) buffer.clear()
        tap.enabled = next.active
        if (previous.active && !next.active) engines.values.forEach { it.release() }
        val language = next.speechLanguage
        val modelState = next.modelState
        _state.value = CaptionState(
            available = language != null,
            status = when {
                language == null || !next.captionsOn -> CaptionStatus.OFF
                next.engine == CaptionEngineType.CLOUD && next.apiKey.isBlank() -> CaptionStatus.NEED_KEY
                next.engine == CaptionEngineType.ON_DEVICE && modelState is WhisperModelStore.State.Downloading -> CaptionStatus.DOWNLOADING
                next.engine == CaptionEngineType.ON_DEVICE && modelState is WhisperModelStore.State.Failed -> CaptionStatus.ERROR
                next.engine == CaptionEngineType.ON_DEVICE && modelState != WhisperModelStore.State.Ready -> CaptionStatus.NEED_MODEL
                !next.playing -> CaptionStatus.OFF
                else -> CaptionStatus.LISTENING
            },
            text = buffer.text(),
            sourceLabel = language?.let(CaptionLanguages::nativeName).orEmpty(),
            target = next.target,
            error = (modelState as? WhisperModelStore.State.Failed)?.let { "Model download failed: ${it.message}" },
            engine = next.engine,
            progress = (modelState as? WhisperModelStore.State.Downloading)?.progress ?: 0f,
            modelSizeMb = next.model.sizeMb,
            transcribeOnly = next.transcribeOnly,
            transcriptEnabled = next.transcriptEnabled,
        )
    }

    private suspend fun process(pcm: ShortArray) {
        val cfg = config
        val language = cfg.speechLanguage ?: return
        if (!cfg.active || PcmChunker.rms(pcm) < PcmChunker.SILENCE_RMS) return
        val engine = engines.getValue(cfg.engine)
        val stationId = cfg.station?.id
        _state.update { it.copy(status = CaptionStatus.TRANSLATING) }
        try {
            val heard = Transcripts.clean(engine.recognize(pcm, language))
            val targetCode = cfg.target.code
            val caption = when {
                heard.isBlank() -> null
                cfg.transcribeOnly || targetCode == null || CaptionLanguages.sameLanguage(language, cfg.target) -> heard
                else -> engine.translate(heard, CaptionLanguages.translationCode(language), targetCode)
            }
            if (config.station?.id != stationId || !config.active) return
            val text = caption?.let(buffer::add) ?: buffer.text()
            _state.update { it.copy(status = CaptionStatus.LISTENING, text = text, error = null) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "caption pipeline failed", e)
            val message = when {
                e is CloudException && (e.httpCode == 400 || e.httpCode == 403) -> "Google Cloud: ${e.message}"
                e is CloudException -> "Google Cloud error ${e.httpCode}"
                cfg.engine == CaptionEngineType.ON_DEVICE -> "On-device captions: ${e.message ?: "error"}"
                else -> "Network error – captions paused"
            }
            if (config.station?.id == stationId) _state.update { it.copy(status = CaptionStatus.ERROR, error = message) }
        }
    }

    private companion object {
        const val TAG = "LiveTranslator"
    }
}
