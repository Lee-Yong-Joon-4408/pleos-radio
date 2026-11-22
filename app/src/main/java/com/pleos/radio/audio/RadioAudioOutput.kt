package com.pleos.radio.audio

import com.pleos.radio.domain.Station

/** Audio sink for the tuner. Implementations own audio focus. */
interface RadioAudioOutput {
    /** Called on the main thread when another app takes audio focus permanently (e.g. Media app starts). */
    var onFocusLost: (() -> Unit)?

    /** Start (or keep) playing [station]; a change of station plays a short tuning sound. */
    fun play(station: Station)

    fun stop()

    fun release()
}
