package com.pleos.radio

import android.app.Application
import com.pleos.radio.audio.HybridRadioAudio
import com.pleos.radio.audio.PcmTap
import com.pleos.radio.data.translate.CloudCaptionEngine
import com.pleos.radio.data.translate.GoogleCloudClient
import com.pleos.radio.data.translate.OnDeviceCaptionEngine
import com.pleos.radio.data.translate.OnDeviceTranslator
import com.pleos.radio.data.translate.WhisperModelStore
import com.pleos.radio.domain.caption.CaptionEngineType
import com.pleos.radio.domain.caption.WhisperModel
import com.pleos.radio.presentation.LiveTranslator
import com.pleos.radio.audio.RadioAudioOutput
import com.pleos.radio.audio.SimulatedBroadcastAudio
import com.pleos.radio.audio.StreamingRadioAudio
import com.pleos.radio.data.RadioPreferences
import com.pleos.radio.data.StationCatalog
import com.pleos.radio.data.StreamResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Minimal service locator (same pattern as the forked pleos-steerin app). */
class RadioApplication : Application() {

    /** Lives as long as the process; audio outlives individual screens. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var catalog: StationCatalog
        private set
    lateinit var preferences: RadioPreferences
        private set
    lateinit var audio: RadioAudioOutput
        private set
    lateinit var translator: LiveTranslator
        private set
    lateinit var whisperModels: WhisperModelStore
        private set

    override fun onCreate() {
        super.onCreate()
        catalog = StationCatalog()
        preferences = RadioPreferences(this)
        val tap = PcmTap()
        val resolver = StreamResolver()
        audio = HybridRadioAudio(
            simulated = SimulatedBroadcastAudio(this),
            streaming = StreamingRadioAudio(this, resolver, appScope, tap),
            ipReachable = { station -> station.stream?.let { resolver.reachable(it) } ?: false },
            scope = appScope,
        )
        whisperModels = WhisperModelStore(this, appScope)
        translator = LiveTranslator(
            engines = mapOf(
                CaptionEngineType.ON_DEVICE to OnDeviceCaptionEngine(whisperModels, OnDeviceTranslator()) {
                    WhisperModel.fromName(preferences.whisperModel())
                },
                CaptionEngineType.CLOUD to CloudCaptionEngine(GoogleCloudClient(this)) {
                    preferences.cloudApiKey().ifBlank { BuildConfig.GOOGLE_CLOUD_API_KEY }
                },
            ),
            tap = tap,
            scope = appScope,
        )
    }
}
