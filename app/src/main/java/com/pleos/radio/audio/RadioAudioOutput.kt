package com.pleos.radio.audio

import com.pleos.radio.domain.Station
import kotlinx.coroutines.flow.StateFlow

/** What the audio path is doing right now (shown next to the station badge). */
sealed interface AudioStatus {
    data object Idle : AudioStatus
    data object Connecting : AudioStatus
    /** Playing the station's internet stream (IP network). */
    data object Live : AudioStatus
    /** Playing from the broadcast tuner; [fallback] = switched here because the IP stream was lost. */
    data class Broadcast(val fallback: Boolean) : AudioStatus
    /** [network] = connectivity-type failure (eligible for broadcast fallback); false = e.g. geo-blocked. */
    data class Error(val message: String, val network: Boolean = true) : AudioStatus
}

/** Which delivery network the audio is currently arriving on (drives the background animation). */
enum class ReceptionPath { NONE, BROADCAST, IP }

fun AudioStatus.receptionPath(): ReceptionPath = when (this) {
    AudioStatus.Live -> ReceptionPath.IP
    is AudioStatus.Broadcast -> ReceptionPath.BROADCAST
    else -> ReceptionPath.NONE
}

/** Audio sink for the tuner. Implementations own audio focus. */
interface RadioAudioOutput {
    /** Called on the main thread when another app takes audio focus permanently (e.g. Media app starts). */
    var onFocusLost: (() -> Unit)?

    val status: StateFlow<AudioStatus>

    /** Start (or keep) playing [station]. Calling it again after an error retries. */
    fun play(station: Station)

    fun stop()

    fun release()
}
