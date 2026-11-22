package com.pleos.radio.audio

import com.pleos.radio.domain.Station
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Routes each station to its real internet stream when it has one, otherwise to the simulated tuner audio. */
class HybridRadioAudio(
    private val simulated: SimulatedBroadcastAudio,
    private val streaming: StreamingRadioAudio,
    scope: CoroutineScope,
) : RadioAudioOutput {

    private enum class Route { NONE, SIMULATED, STREAM }

    override var onFocusLost: (() -> Unit)? = null

    private val route = MutableStateFlow(Route.NONE)

    override val status: StateFlow<AudioStatus> = combine(route, streaming.status) { r, s ->
        when (r) {
            Route.NONE -> AudioStatus.Idle
            Route.SIMULATED -> AudioStatus.Simulated
            Route.STREAM -> s
        }
    }.stateIn(scope, SharingStarted.Eagerly, AudioStatus.Idle)

    init {
        // Only the active path may report focus loss; the other one may lose focus to us during a switch.
        simulated.onFocusLost = { if (route.value == Route.SIMULATED) lose() }
        streaming.onFocusLost = { if (route.value == Route.STREAM) lose() }
    }

    private fun lose() {
        route.value = Route.NONE
        onFocusLost?.invoke()
    }

    override fun play(station: Station) {
        if (station.stream != null) {
            if (route.value == Route.SIMULATED) simulated.stop()
            route.value = Route.STREAM
            streaming.play(station)
        } else {
            if (route.value == Route.STREAM) streaming.stop()
            route.value = Route.SIMULATED
            simulated.play(station)
        }
    }

    override fun stop() {
        route.value = Route.NONE
        simulated.stop()
        streaming.stop()
    }

    override fun release() {
        stop()
        simulated.release()
        streaming.release()
    }
}
