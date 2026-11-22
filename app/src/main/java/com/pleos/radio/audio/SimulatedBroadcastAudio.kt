package com.pleos.radio.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import com.pleos.radio.domain.Band
import com.pleos.radio.domain.Station
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Stand-in for the broadcast tuner's audio path on bands without a real stream (DRM, ATSC).
 *
 * Generates a soft, station-specific chord pad (so play/pause/tune are audible on the emulator),
 * a short burst of static when retuning, and analogue-style hiss on FM/AM/DRM proportional to the
 * weak signal. Requests media audio focus like a real radio source: pauses on permanent loss,
 * suspends on transient loss (navigation prompts, calls) and ducks when asked.
 */
class SimulatedBroadcastAudio(context: Context) {

    var onFocusLost: (() -> Unit)? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val main = Handler(Looper.getMainLooper())

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener(::onFocusChange, main)
        .build()

    private var hasFocus = false
    private var resumeOnGain = false
    private var current: Station? = null
    private var worker: Worker? = null

    fun play(station: Station) {
        val retune = current != null && current?.id != station.id
        current = station
        if (!hasFocus) {
            hasFocus = audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (!hasFocus) return
        }
        val w = worker ?: Worker().also { worker = it; it.start() }
        w.configure(station, staticBurst = retune || !w.started)
        w.started = true
    }

    fun stop() {
        resumeOnGain = false
        stopWorker()
        if (hasFocus) {
            audioManager.abandonAudioFocusRequest(focusRequest)
            hasFocus = false
        }
    }

    fun release() = stop()

    private fun stopWorker() {
        worker?.finish()
        worker = null
    }

    private fun onFocusChange(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                worker?.duck = 1f
                if (resumeOnGain) {
                    resumeOnGain = false
                    current?.let { play(it) }
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> worker?.duck = 0.3f
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                resumeOnGain = worker != null
                stopWorker()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasFocus = false
                resumeOnGain = false
                stopWorker()
                onFocusLost?.invoke()
            }
        }
    }

    /** Audio render thread. All cross-thread fields are volatile scalars. */
    private inner class Worker : Thread("RadioAudio") {
        @Volatile var duck = 1f
        @Volatile private var running = true
        @Volatile private var targetRoot = 220.0
        @Volatile private var hiss = 0.0
        @Volatile private var staticSamples = 0
        var started = false

        fun configure(station: Station, staticBurst: Boolean) {
            // Deterministic root note per station: A3 transposed by 0..11 semitones.
            val semis = abs(station.id.hashCode()) % 12
            targetRoot = 196.0 * 2.0.pow(semis / 12.0)
            val weak = (4 - station.signal.coerceIn(0, 4)) / 4.0
            hiss = when (station.band) {
                Band.DAB -> 0.0
                Band.DRM -> 0.15 * weak
                Band.FM -> 0.3 * weak
                Band.ATSC -> 0.0
                Band.AM -> 0.25 + 0.5 * weak
            }
            if (staticBurst) staticSamples = SAMPLE_RATE * 35 / 100
        }

        fun finish() {
            running = false
        }

        override fun run() {
            val minBuf = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(max(minBuf, 8192))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            val buffer = ShortArray(1024)
            val ratios = doubleArrayOf(1.0, 1.25, 1.5, 2.0)
            val weights = doubleArrayOf(0.45, 0.25, 0.25, 0.12)
            val phases = DoubleArray(ratios.size)
            var root = targetRoot
            var t = 0.0
            var gain = 0.0
            var noiseLp = 0.0
            try {
                track.play()
                while (true) {
                    val wantGain = if (running) 1.0 else 0.0
                    for (i in buffer.indices) {
                        root += (targetRoot - root) * 0.0008
                        var pad = 0.0
                        for (k in ratios.indices) {
                            phases[k] += 2 * PI * root * ratios[k] / SAMPLE_RATE
                            if (phases[k] > 2 * PI) phases[k] -= 2 * PI
                            pad += sin(phases[k]) * weights[k]
                        }
                        val lfo = 0.65 + 0.35 * sin(2 * PI * 0.12 * t)
                        noiseLp += (Random.nextDouble(-1.0, 1.0) - noiseLp) * 0.35
                        val inStatic = staticSamples > 0
                        if (inStatic) staticSamples--
                        val sample = if (inStatic) noiseLp * 0.35 else pad * 0.16 * lfo + noiseLp * hiss * 0.08
                        gain += (wantGain - gain) * 0.002
                        buffer[i] = (sample * gain * duck * Short.MAX_VALUE).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        t += 1.0 / SAMPLE_RATE
                    }
                    track.write(buffer, 0, buffer.size)
                    if (!running && gain < 0.001) break
                }
            } finally {
                runCatching { track.stop() }
                track.release()
            }
        }
    }

    private companion object {
        const val SAMPLE_RATE = 22_050
    }
}
