package com.pleos.radio

import android.app.Application
import com.pleos.radio.audio.RadioAudioOutput
import com.pleos.radio.audio.SimulatedBroadcastAudio
import com.pleos.radio.data.RadioPreferences
import com.pleos.radio.data.StationCatalog

/** Minimal service locator (same pattern as the forked pleos-steerin app). */
class RadioApplication : Application() {

    lateinit var catalog: StationCatalog
        private set
    lateinit var preferences: RadioPreferences
        private set
    lateinit var audio: RadioAudioOutput
        private set

    override fun onCreate() {
        super.onCreate()
        catalog = StationCatalog()
        preferences = RadioPreferences(this)
        audio = SimulatedBroadcastAudio(this)
    }
}
