package com.pleos.radio.audio

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import com.pleos.radio.domain.caption.PcmChunker
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Receives the decoded PCM that ExoPlayer is about to play (via [TeeAudioProcessor]) and chunks it for
 * live translation. Does nothing unless [enabled], so ordinary playback pays no cost.
 */
@UnstableApi
class PcmTap : TeeAudioProcessor.AudioBufferSink {

    /** Receives ~6 s 16 kHz mono chunks (called on the playback thread; keep it cheap). */
    @Volatile var onChunk: ((ShortArray) -> Unit)? = null

    @Volatile var enabled: Boolean = false
        set(value) {
            if (field != value && !value) chunker.reset()
            field = value
        }

    @Volatile private var pcm16 = false
    private val chunker = PcmChunker { chunk -> onChunk?.invoke(chunk) }
    private var scratch = ShortArray(0)

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        pcm16 = encoding == C.ENCODING_PCM_16BIT
        if (pcm16) chunker.configure(sampleRateHz, channelCount)
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        if (!enabled || !pcm16) return
        val shorts = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val n = shorts.remaining()
        if (scratch.size < n) scratch = ShortArray(n)
        shorts.get(scratch, 0, n)
        chunker.feed(scratch, n)
    }
}
