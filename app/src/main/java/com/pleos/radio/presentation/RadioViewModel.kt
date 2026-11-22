package com.pleos.radio.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pleos.radio.RadioApplication
import com.pleos.radio.domain.Band
import com.pleos.radio.domain.RadioLogic
import com.pleos.radio.domain.Station
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Left navigation rail destinations. Radio is the main (launch) screen. */
enum class Section { RADIO, MEDIA, PHONE, NAVIGATION, SETTINGS }

data class RadioUiState(
    val section: Section = Section.RADIO,
    val band: Band = Band.DAB,
    val stations: List<Station> = emptyList(),
    val current: Station? = null,
    val playing: Boolean = true,
    val favorites: Set<String> = emptySet(),
    val presets: List<Station?> = emptyList(),
    val presetPage: Int = 0,
    val stationListOpen: Boolean = false,
    val favoritesOnly: Boolean = false,
    val scanning: Boolean = false,
    val scanProgress: Float = 0f,
    val labelIndex: Int = 0,
    val audioEnabled: Boolean = true,
    val message: String? = null,
) {
    val isFavorite: Boolean get() = current?.id in favorites
    val listedStations: List<Station> get() = RadioLogic.filter(stations, favorites, favoritesOnly)
    val presetPageCount: Int get() = RadioLogic.pageCount(presets.size)
}

class RadioViewModel(app: Application) : AndroidViewModel(app) {

    private val container = app as RadioApplication
    private val catalog = container.catalog
    private val prefs = container.preferences
    private val audio = container.audio

    private val stationsByBand = Band.entries.associateWith { catalog.stations(it) }.toMutableMap()
    private val presetsByBand = Band.entries.associateWith { band ->
        val stations = stationsByBand.getValue(band)
        RadioLogic.restorePresets(prefs.presets(band), stations.map { it.id }.toSet(), RadioLogic.defaultPresets(stations))
    }.toMutableMap()

    private val _ui = MutableStateFlow(RadioUiState(favorites = prefs.favorites(), audioEnabled = prefs.audioEnabled()))
    val ui: StateFlow<RadioUiState> = _ui.asStateFlow()

    private var scanJob: Job? = null
    private var messageJob: Job? = null

    init {
        audio.onFocusLost = { _ui.update { it.copy(playing = false) } }
        showBand(prefs.lastBand() ?: Band.DAB)
        syncAudio()
        // Rotate the dynamic label (DLS / radiotext) like a real receiver.
        viewModelScope.launch {
            while (true) {
                delay(LABEL_ROTATE_MS)
                if (_ui.value.playing) _ui.update { it.copy(labelIndex = it.labelIndex + 1) }
            }
        }
    }

    // --- band & tuning ------------------------------------------------------------------

    fun selectBand(band: Band) {
        if (band == _ui.value.band) return
        scanJob?.cancel()
        showBand(band)
        prefs.saveLastBand(band)
        syncAudio()
    }

    private fun showBand(band: Band) {
        val stations = stationsByBand.getValue(band)
        val current = stations.firstOrNull { it.id == prefs.lastStation(band) } ?: stations.firstOrNull()
        _ui.update {
            it.copy(
                band = band,
                stations = stations,
                current = current,
                presets = resolvePresets(band),
                presetPage = 0,
                scanning = false,
                scanProgress = 0f,
                labelIndex = 0,
            )
        }
    }

    fun tune(station: Station) {
        if (station.id == _ui.value.current?.id) {
            if (!_ui.value.playing) togglePlay()
            return
        }
        _ui.update { it.copy(current = station, labelIndex = 0, playing = true) }
        prefs.saveLastStation(station.band, station.id)
        syncAudio()
    }

    /** Previous / next service (DAB) or seek to the previous / next receivable frequency (DRM, AM). */
    fun seek(step: Int) {
        val s = _ui.value
        RadioLogic.adjacent(s.stations, s.current?.id, step)?.let(::tune)
    }

    fun togglePlay() {
        _ui.update { it.copy(playing = !it.playing) }
        syncAudio()
    }

    fun toggleFavorite(station: Station? = _ui.value.current) {
        station ?: return
        val updated = RadioLogic.toggle(_ui.value.favorites, station.id)
        _ui.update { it.copy(favorites = updated) }
        prefs.saveFavorites(updated)
        flash(if (station.id in updated) "${station.name} added to favorites" else "${station.name} removed from favorites")
    }

    // --- presets ------------------------------------------------------------------------

    fun selectPreset(slot: Int) {
        val station = _ui.value.presets.getOrNull(slot)
        if (station == null) flash("Preset ${slot + 1} is empty – long-press to save the current station")
        else tune(station)
    }

    fun savePreset(slot: Int) {
        val s = _ui.value
        val current = s.current ?: return
        presetsByBand[s.band] = RadioLogic.assignPreset(presetsByBand.getValue(s.band), slot, current.id)
        prefs.savePresets(s.band, presetsByBand.getValue(s.band))
        _ui.update { it.copy(presets = resolvePresets(s.band)) }
        flash("${current.name} saved to preset ${slot + 1}")
    }

    fun changePresetPage(delta: Int) {
        _ui.update { it.copy(presetPage = Math.floorMod(it.presetPage + delta, it.presetPageCount)) }
    }

    fun resetPresets() {
        prefs.clearPresets()
        Band.entries.forEach { presetsByBand[it] = RadioLogic.defaultPresets(stationsByBand.getValue(it)) }
        _ui.update { it.copy(presets = resolvePresets(it.band), presetPage = 0) }
        flash("Presets restored to defaults")
    }

    private fun resolvePresets(band: Band): List<Station?> {
        val stations = stationsByBand.getValue(band)
        return presetsByBand.getValue(band).map { id -> stations.firstOrNull { it.id == id } }
    }

    // --- station list -------------------------------------------------------------------

    fun openStationList() = _ui.update { it.copy(stationListOpen = true) }
    fun closeStationList() = _ui.update { it.copy(stationListOpen = false) }
    fun setFavoritesOnly(only: Boolean) = _ui.update { it.copy(favoritesOnly = only) }

    /** Simulated band scan: progress bar, then a refreshed service list with new signal levels. */
    fun scan() {
        if (_ui.value.scanning) return
        val band = _ui.value.band
        scanJob = viewModelScope.launch {
            _ui.update { it.copy(scanning = true, scanProgress = 0f) }
            for (i in 1..SCAN_STEPS) {
                delay(SCAN_STEP_MS)
                _ui.update { it.copy(scanProgress = i / SCAN_STEPS.toFloat()) }
            }
            val found = catalog.scan(band)
            stationsByBand[band] = found
            _ui.update { s ->
                s.copy(
                    scanning = false,
                    stations = found,
                    current = found.firstOrNull { it.id == s.current?.id } ?: s.current,
                    presets = resolvePresets(band),
                )
            }
            flash("Scan complete – ${found.size} ${band.label} stations found")
        }
    }

    // --- navigation & settings ----------------------------------------------------------

    fun selectSection(section: Section) = _ui.update { it.copy(section = section, stationListOpen = false) }
    fun goHome() = selectSection(Section.RADIO)

    fun setAudioEnabled(enabled: Boolean) {
        _ui.update { it.copy(audioEnabled = enabled) }
        prefs.saveAudioEnabled(enabled)
        syncAudio()
    }

    /** Steering-wheel / hardware media keys. */
    fun onMediaKey(key: MediaKey) = when (key) {
        MediaKey.NEXT -> seek(+1)
        MediaKey.PREVIOUS -> seek(-1)
        MediaKey.PLAY_PAUSE -> togglePlay()
        MediaKey.PLAY -> if (!_ui.value.playing) togglePlay() else Unit
        MediaKey.PAUSE -> if (_ui.value.playing) togglePlay() else Unit
    }

    fun dismissMessage() = _ui.update { it.copy(message = null) }

    private fun flash(text: String) {
        messageJob?.cancel()
        _ui.update { it.copy(message = text) }
        messageJob = viewModelScope.launch {
            delay(MESSAGE_MS)
            dismissMessage()
        }
    }

    private fun syncAudio() {
        val s = _ui.value
        val station = s.current
        if (s.playing && s.audioEnabled && station != null) audio.play(station) else audio.stop()
    }

    override fun onCleared() {
        audio.onFocusLost = null
        audio.release()
    }

    private companion object {
        const val LABEL_ROTATE_MS = 6_000L
        const val SCAN_STEPS = 25
        const val SCAN_STEP_MS = 100L
        const val MESSAGE_MS = 2_500L
    }
}

enum class MediaKey { NEXT, PREVIOUS, PLAY_PAUSE, PLAY, PAUSE }
