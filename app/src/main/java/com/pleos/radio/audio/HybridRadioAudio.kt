package com.pleos.radio.audio

import android.util.Log
import com.pleos.radio.domain.Station
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Hybrid radio: plays a station's internet stream (IP) when it has one and falls back to the broadcast
 * tuner when the IP stream is lost for network reasons, returning to IP once it is reachable again.
 * The tuner is [SimulatedBroadcastAudio] today; a broadcast-radio-HAL implementation can replace it.
 */
class HybridRadioAudio(
    private val simulated: SimulatedBroadcastAudio,
    private val streaming: StreamingRadioAudio,
    /** True when the station's IP stream can be reached right now (used to leave the fallback). */
    private val ipReachable: suspend (Station) -> Boolean,
    private val scope: CoroutineScope,
) : RadioAudioOutput {

    private enum class Route { NONE, BROADCAST, STREAM, OFFLINE }

    override var onFocusLost: (() -> Unit)? = null

    private val route = MutableStateFlow(Route.NONE)
    private val fallback = MutableStateFlow(false)
    private val currentStation = MutableStateFlow<Station?>(null)
    private var current: Station?
        get() = currentStation.value
        set(value) { currentStation.value = value }
    private var probeJob: Job? = null
    private var handoverJob: Job? = null
    private var handoverStationId: String? = null
    /** Handovers performed on the current handover-demo station (even = IP, odd = RF). */
    private val handovers = MutableStateFlow(0)

    /** Off until a real tuner exists: a lost IP stream then means silence + "no internet", not a tone. */
    @Volatile private var broadcastFallbackEnabled = false

    override val status: StateFlow<AudioStatus> = combine(route, fallback, streaming.status, currentStation, handovers) { r, fb, s, st, n ->
        when {
            // Handover demo: same decoded audio, only the reception path changes -> no gap in the output.
            r == Route.STREAM && st?.handoverDemo == true && s == AudioStatus.Live -> AudioStatus.Handover(handoverPath(n), n)
            else -> plainStatus(r, fb, s, st)
        }
    }.stateIn(scope, SharingStarted.Eagerly, AudioStatus.Idle)

    private fun plainStatus(r: Route, fb: Boolean, s: AudioStatus, st: Station?): AudioStatus =
        when (r) {
            Route.NONE -> AudioStatus.Idle
            Route.BROADCAST -> AudioStatus.Broadcast(fallback = fb)
            Route.OFFLINE -> AudioStatus.Offline
            // Virtual RF channel: presented as RF reception while its audio is relayed from the IP stream.
            Route.STREAM -> if (st?.virtualRf == true && s == AudioStatus.Live) AudioStatus.Broadcast(fallback = false, viaIp = true) else s
        }

    init {
        // Only the active path may report focus loss; the other one may lose focus to us during a switch.
        simulated.onFocusLost = { if (route.value == Route.BROADCAST) lose() }
        streaming.onFocusLost = { if (route.value == Route.STREAM) lose() }
        // IP stream lost (network) -> broadcast tuner, or silence + retry when the fallback is off.
        scope.launch {
            streaming.status.collect { s ->
                if (route.value == Route.STREAM && s is AudioStatus.Error && s.network) {
                    if (broadcastFallbackEnabled) switchToBroadcast() else switchToOffline()
                }
            }
        }
    }

    private fun lose() {
        stopAll()
        onFocusLost?.invoke()
    }

    override fun play(station: Station) {
        val sameStation = station.id == current?.id
        current = station
        when {
            station.stream == null -> playBroadcast(station, isFallback = false)
            // Already on the broadcast fallback for this station: stay there until the IP probe succeeds.
            sameStation && route.value == Route.BROADCAST && fallback.value -> simulated.play(station)
            // Already waiting offline for this station: keep waiting (the probe resumes it).
            sameStation && route.value == Route.OFFLINE -> Unit
            else -> playIp(station)
        }
    }

    private fun playIp(station: Station) {
        probeJob?.cancel()
        fallback.value = false
        if (route.value == Route.BROADCAST) simulated.stop()
        route.value = Route.STREAM
        streaming.play(station)
        startHandover(station)
    }

    /**
     * Handover demo: every [HANDOVER_INTERVAL_MS] while the audio is live, hand the station over to the other
     * network (IP <-> RF). Both paths carry the same programme and the player is never stopped or re-prepared,
     * so the switch is seamless; a real tuner would be time-aligned to the stream before the crossover.
     */
    private fun startHandover(station: Station) {
        // Re-selecting the station that is already handing over keeps the running cycle.
        if (station.handoverDemo && handoverJob?.isActive == true && handoverStationId == station.id) return
        handoverJob?.cancel()
        handoverStationId = station.id
        handovers.value = 0
        if (!station.handoverDemo) return
        handoverJob = scope.launch {
            while (true) {
                delay(HANDOVER_INTERVAL_MS)
                if (route.value != Route.STREAM || current?.id != station.id) return@launch
                if (streaming.status.value != AudioStatus.Live) continue
                handovers.value += 1
                val n = handovers.value
                Log.i(TAG, "handover #$n ${handoverPath(n - 1)} -> ${handoverPath(n)} (${station.name}, audio uninterrupted)")
            }
        }
    }

    private fun playBroadcast(station: Station, isFallback: Boolean) {
        if (route.value == Route.STREAM) streaming.stop()
        route.value = Route.BROADCAST
        fallback.value = isFallback
        simulated.play(station)
    }

    private fun switchToBroadcast() {
        val station = current ?: return
        playBroadcast(station, isFallback = true)
        startProbe(station)
    }

    private fun switchToOffline() {
        val station = current ?: return
        streaming.stop()
        simulated.stop()
        fallback.value = false
        route.value = Route.OFFLINE
        startProbe(station)
    }

    /** Polls the station's IP stream while on fallback / offline and returns to it once reachable. */
    private fun startProbe(station: Station) {
        probeJob?.cancel()
        probeJob = scope.launch {
            while (true) {
                delay(PROBE_INTERVAL_MS)
                val waiting = route.value == Route.OFFLINE || (route.value == Route.BROADCAST && fallback.value)
                if (!waiting || current?.id != station.id) return@launch
                if (ipReachable(station)) {
                    playIp(station)
                    return@launch
                }
            }
        }
    }

    override fun setBroadcastFallbackEnabled(enabled: Boolean) {
        broadcastFallbackEnabled = enabled
        // Apply immediately if the stream is currently lost.
        when {
            !enabled && route.value == Route.BROADCAST && fallback.value -> switchToOffline()
            enabled && route.value == Route.OFFLINE -> switchToBroadcast()
        }
    }

    override fun retryIp() {
        val station = current ?: return
        if (route.value == Route.OFFLINE || (route.value == Route.BROADCAST && fallback.value)) playIp(station)
    }

    override fun stop() = stopAll()

    private fun stopAll() {
        probeJob?.cancel()
        handoverJob?.cancel()
        handovers.value = 0
        route.value = Route.NONE
        fallback.value = false
        simulated.stop()
        streaming.stop()
    }

    override val videoPlayer: androidx.media3.common.Player get() = streaming.videoPlayer

    override fun setVideoEnabled(enabled: Boolean) = streaming.setVideoEnabled(enabled)

    override fun release() {
        stopAll()
        simulated.release()
        streaming.release()
    }

    private companion object {
        const val PROBE_INTERVAL_MS = 20_000L
        const val HANDOVER_INTERVAL_MS = 5_000L
        const val TAG = "HybridRadioAudio"
    }
}
