package com.pleos.radio.data

import android.content.Context
import com.pleos.radio.domain.Band

/** On-device persistence for favourites, presets and the last tuned service. */
class RadioPreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("radio_prefs", Context.MODE_PRIVATE)

    fun favorites(): Set<String> = prefs.getStringSet(KEY_FAVORITES, emptySet())!!.toSet()
    fun saveFavorites(ids: Set<String>) = prefs.edit().putStringSet(KEY_FAVORITES, ids).apply()

    fun presets(band: Band): List<String?>? =
        prefs.getString(presetKey(band), null)?.split(SEP)?.map { it.ifEmpty { null } }

    fun savePresets(band: Band, presets: List<String?>) =
        prefs.edit().putString(presetKey(band), presets.joinToString(SEP) { it.orEmpty() }).apply()

    fun clearPresets() = prefs.edit().apply { Band.entries.forEach { remove(presetKey(it)) } }.apply()

    fun lastBand(): Band? = prefs.getString(KEY_BAND, null)?.let { name -> Band.entries.firstOrNull { it.name == name } }
    fun saveLastBand(band: Band) = prefs.edit().putString(KEY_BAND, band.name).apply()

    fun lastStation(band: Band): String? = prefs.getString("last_station_${band.name}", null)
    fun saveLastStation(band: Band, id: String) = prefs.edit().putString("last_station_${band.name}", id).apply()

    fun audioEnabled(): Boolean = prefs.getBoolean(KEY_AUDIO, true)
    fun saveAudioEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_AUDIO, enabled).apply()

    fun captionLanguage(): String? = prefs.getString(KEY_CAPTION, null)
    fun saveCaptionLanguage(name: String) = prefs.edit().putString(KEY_CAPTION, name).apply()

    /** Google Cloud API key entered in Settings (overrides the build-time key from local.properties). */
    fun cloudApiKey(): String = prefs.getString(KEY_CLOUD_KEY, "").orEmpty()
    fun saveCloudApiKey(key: String) = prefs.edit().putString(KEY_CLOUD_KEY, key.trim()).apply()

    fun transcriptEnabled(): Boolean = prefs.getBoolean(KEY_TRANSCRIPT, true)
    fun saveTranscriptEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_TRANSCRIPT, enabled).apply()

    /** One-time migration: virtual RF presets added to existing preset lists. */
    fun rfPresetsAdded(): Boolean = prefs.getBoolean(KEY_RF_PRESETS, false)
    fun markRfPresetsAdded() = prefs.edit().putBoolean(KEY_RF_PRESETS, true).apply()

    fun captionEngine(): String? = prefs.getString(KEY_ENGINE, null)
    fun saveCaptionEngine(name: String) = prefs.edit().putString(KEY_ENGINE, name).apply()

    fun whisperModel(): String? = prefs.getString(KEY_WHISPER_MODEL, null)
    fun saveWhisperModel(name: String) = prefs.edit().putString(KEY_WHISPER_MODEL, name).apply()

    private fun presetKey(band: Band) = "presets_${band.name}"

    private companion object {
        const val SEP = "|"
        const val KEY_FAVORITES = "favorites"
        const val KEY_BAND = "last_band"
        const val KEY_AUDIO = "audio_enabled"
        const val KEY_CAPTION = "caption_language"
        const val KEY_CLOUD_KEY = "google_cloud_api_key"
        const val KEY_ENGINE = "caption_engine"
        const val KEY_TRANSCRIPT = "transcript_enabled"
        const val KEY_RF_PRESETS = "rf_presets_v1"
        const val KEY_WHISPER_MODEL = "whisper_model"
    }
}
