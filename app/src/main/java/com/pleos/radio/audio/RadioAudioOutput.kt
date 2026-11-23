package com.pleos.radio.audio

import com.pleos.radio.domain.Station
import kotlinx.coroutines.flow.StateFlow

/** What the audio path is doing right now (shown next to the station badge). */
sealed interface AudioStatus {
    data object Idle : AudioStatus
    data object Connecting : AudioStatus
    /** Playing the station's internet stream (IP network). */
    data object Live : AudioStatus
    /**
     * Shown as received over the RF broadcast network. [fallback] = tuner took over because the IP stream was lost;
     * [viaIp] = virtual RF channel whose audio is actually relayed from the IP stream.
     */
    data class Broadcast(val fallback: Boolean, val viaIp: Boolean = false) : AudioStatus
    /**
     * Hybrid handover demo: the station is received on [path] (IP or BROADCAST) and switches to the other
     * network every few seconds; [count] = handovers so far. Audio continues without a gap.
     */
    data class Handover(val path: ReceptionPath, val count: Int) : AudioStatus
    /** IP stream lost and broadcast fallback is off: silent, retrying until the internet is back. */
    data object Offline : AudioStatus
    /** [network] = connectivity-type failure (eligible for broadcast fallback); false = e.g. geo-blocked. */
    data class Error(val message: String, val network: Boolean = true) : AudioStatus
}

/** Which delivery network the audio is currently arriving on (drives the background animation). */
enum class ReceptionPath { NONE, BROADCAST, IP }

fun AudioStatus.receptionPath(): ReceptionPath = when (this) {
    AudioStatus.Live -> ReceptionPath.IP
    is AudioStatus.Broadcast -> ReceptionPath.BROADCAST
    is AudioStatus.Handover -> path
    else -> ReceptionPath.NONE
}

/** Path after [count] handovers: starts on IP, then alternates RF, IP, RF … */
fun handoverPath(count: Int): ReceptionPath = if (count % 2 == 0) ReceptionPath.IP else ReceptionPath.BROADCAST

/** Audio sink for the tuner. Implementations own audio focus. */
interface RadioAudioOutput {
    /** Called on the main thread when another app takes audio focus permanently (e.g. Media app starts). */
    var onFocusLost: (() -> Unit)?

    val status: StateFlow<AudioStatus>

    /** Start (or keep) playing [station]. Calling it again after an error retries. */
    fun play(station: Station)

    fun stop()

    fun release()

    /** Player whose video can be rendered (TV stations); null when the output has no video path. */
    val videoPlayer: androidx.media3.common.Player? get() = null

    /** Decode the stream's video track (only while it may be shown: TV station, parked, playing). */
    fun setVideoEnabled(enabled: Boolean) {}

    /** When the IP stream is lost: true = switch to the (simulated) broadcast tuner, false = stay silent. */
    fun setBroadcastFallbackEnabled(enabled: Boolean) {}

    /** Try the IP stream again right away (after a loss). */
    fun retryIp() {}
}
