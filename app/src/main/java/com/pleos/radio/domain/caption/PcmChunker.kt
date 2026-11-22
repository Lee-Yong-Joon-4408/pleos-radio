package com.pleos.radio.domain.caption

import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Turns decoded interleaved PCM16 (any sample rate / channel count) into fixed-length 16 kHz mono chunks
 * for speech recognition: averages channels, resamples with linear interpolation, and calls [onChunk]
 * every [chunkSeconds] of audio. Thread-safe; fed from the audio playback thread.
 */
class PcmChunker(
    private val targetRate: Int = 16_000,
    chunkSeconds: Double = 6.0,
    private val onChunk: (ShortArray) -> Unit,
) {
    private val out = ShortArray((targetRate * chunkSeconds).toInt())
    private var outLen = 0
    private var channels = 0
    private var step = 0.0
    private var t = 0.0          // next output position, in input frames relative to the current buffer
    private var prev = 0f        // last mono sample of the previous buffer (index -1)

    @Synchronized
    fun configure(sampleRate: Int, channelCount: Int) {
        channels = channelCount.coerceAtLeast(1)
        step = sampleRate.toDouble() / targetRate
        reset()
    }

    @Synchronized
    fun reset() {
        outLen = 0
        t = 0.0
        prev = 0f
    }

    /** [samples] holds [count] interleaved PCM16 values. */
    @Synchronized
    fun feed(samples: ShortArray, count: Int) {
        if (channels == 0 || step <= 0.0) return
        val frames = count / channels
        if (frames == 0) return
        fun mono(i: Int): Float {
            if (i < 0) return prev
            var sum = 0
            val base = i * channels
            for (c in 0 until channels) sum += samples[base + c]
            return sum.toFloat() / channels
        }
        while (t <= frames - 1) {
            val i0 = floor(t).toInt()
            val frac = (t - i0).toFloat()
            val a = mono(i0)
            val b = if (i0 + 1 <= frames - 1) mono(i0 + 1) else a
            out[outLen++] = (a + (b - a) * frac).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            if (outLen == out.size) {
                onChunk(out.copyOf())
                outLen = 0
            }
            t += step
        }
        prev = mono(frames - 1)
        t -= frames
    }

    companion object {
        /** Root-mean-square level of a chunk (0..32767); used to skip silence. */
        fun rms(chunk: ShortArray): Double {
            if (chunk.isEmpty()) return 0.0
            var acc = 0.0
            for (s in chunk) acc += s.toDouble() * s
            return sqrt(acc / chunk.size)
        }

        const val SILENCE_RMS = 300.0
    }
}
