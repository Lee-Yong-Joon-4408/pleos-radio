package com.pleos.radio.domain

/** Pure tuner/preset rules, kept free of Android types so they are unit-testable. */
object RadioLogic {

    const val PRESET_SLOTS = 36
    const val PRESETS_PER_PAGE = 6

    /** Station [step] positions away from [currentId] in [stations], wrapping around the ends. */
    fun adjacent(stations: List<Station>, currentId: String?, step: Int): Station? {
        if (stations.isEmpty()) return null
        val index = stations.indexOfFirst { it.id == currentId }
        if (index < 0) return if (step >= 0) stations.first() else stations.last()
        return stations[Math.floorMod(index + step, stations.size)]
    }

    /** Factory presets: the first [slots] stations in band order, remaining slots empty. */
    fun defaultPresets(stations: List<Station>, slots: Int = PRESET_SLOTS): List<String?> =
        List(slots) { stations.getOrNull(it)?.id }

    /** Store [stationId] in [slot]. Out-of-range slots leave the list unchanged. */
    fun assignPreset(presets: List<String?>, slot: Int, stationId: String): List<String?> =
        if (slot !in presets.indices) presets else presets.toMutableList().also { it[slot] = stationId }

    fun toggle(set: Set<String>, id: String): Set<String> = if (id in set) set - id else set + id

    fun pageCount(slots: Int = PRESET_SLOTS, perPage: Int = PRESETS_PER_PAGE): Int = (slots + perPage - 1) / perPage

    fun filter(stations: List<Station>, favorites: Set<String>, onlyFavorites: Boolean): List<Station> =
        if (onlyFavorites) stations.filter { it.id in favorites } else stations

    /**
     * Restore a saved preset list, padding/truncating to [slots] and dropping ids no longer receivable.
     * If nothing saved is receivable any more (e.g. the station list was replaced), use [fallback].
     * Slots beyond the saved list (the preset bank grew) are filled with defaults not already stored.
     */
    fun restorePresets(saved: List<String?>?, known: Set<String>, fallback: List<String?>, slots: Int = PRESET_SLOTS): List<String?> {
        if (saved == null) return fallback
        val restored = MutableList(slots) { i -> saved.getOrNull(i)?.takeIf { it in known } }
        if (restored.all { it == null }) return fallback
        val extra = fallback.filterNotNull().filter { it !in restored }.iterator()
        for (i in saved.size until slots) if (extra.hasNext()) restored[i] = extra.next()
        return restored
    }
}
