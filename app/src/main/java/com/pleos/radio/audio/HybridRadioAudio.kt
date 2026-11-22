package com.pleos.radio.audio

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

    private enum class Route { NONE, BROADCAST, STREAM }

    override var onFocusLost: (() -> Unit)? = null

    private val route = MutableStateFlow(Route.NONE)
    private val fallback = MutableStateFlow(false)
    private val currentStation = MutableStateFlow<Station?>(null)
    private var current: Station?
        get() = currentStation.value
        set(value) { currentStation.value = value }
    private var probeJob: Job? = null

    override val status: StateFlow<AudioStatus> = combine(route, fallback, streaming.status, currentStation) { r, fb, s, st ->
        when (r) {
            Route.NONE -> AudioStatus.Idle
            Route.BROADCAST -> AudioStatus.Broadcast(fallback = fb)
            // Virtual RF channel: presented as RF reception while its audio is relayed from the IP stream.
            Route.STREAM -> if (st?.virtualRf == true && s == AudioStatus.Live) AudioStatus.Broadcast(fallback = false, viaIp = true) else s
        }
    }.stateIn(scope, SharingStarted.Eagerly, AudioStatus.Idle)

    init {
        // Only the active path may report focus loss; the other one may lose focus to us during a switch.
        simulated.onFocusLost = { if (route.value == Route.BROADCAST) lose() }
        streaming.onFocusLost = { if (route.value == Route.STREAM) lose() }
        // Hybrid switching: IP stream lost (network) -> broadcast tuner.
        scope.launch {
            streaming.status.collect { s ->
                if (route.value == Route.STREAM && s is AudioStatus.Error && s.network) switchToBroadcast()
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
            else -> playIp(station)
        }
    }

    private fun playIp(station: Station) {
        probeJob?.cancel()
        fallback.value = false
        if (route.value == Route.BROADCAST) simulated.stop()
        route.value = Route.STREAM
        streaming.play(station)
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
        probeJob?.cancel()
        probeJob = scope.launch {
            while (true) {
                delay(PROBE_INTERVAL_MS)
                if (route.value != Route.BROADCAST || !fallback.value || current?.id != station.id) return@launch
                if (ipReachable(station)) {
                    playIp(station)
                    return@launch
                }
            }
        }
    }

    override fun stop() = stopAll()

    private fun stopAll() {
        probeJob?.cancel()
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
    }
}
