package com.pleos.radio.audio

import com.pleos.radio.domain.Station
import kotlinx.coroutines.flow.StateFlow

/** What the audio path is doing right now (shown next to the station badge). */
sealed interface AudioStatus {
    data object Idle : AudioStatus
    data object Simulated : AudioStatus
    data object Connecting : AudioStatus
    data object Live : AudioStatus
    data class Error(val message: String) : AudioStatus
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
