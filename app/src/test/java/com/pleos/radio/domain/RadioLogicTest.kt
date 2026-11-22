package com.pleos.radio.domain

import com.pleos.radio.data.StationCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioLogicTest {

    private val dab = StationCatalog().stations(Band.DAB)

    @Test fun nextAndPreviousWrapAround() {
        assertEquals(dab[1], RadioLogic.adjacent(dab, dab[0].id, +1))
        assertEquals(dab.last(), RadioLogic.adjacent(dab, dab[0].id, -1))
        assertEquals(dab.first(), RadioLogic.adjacent(dab, dab.last().id, +1))
    }

    @Test fun adjacentWithUnknownCurrentStartsAtEnds() {
        assertEquals(dab.first(), RadioLogic.adjacent(dab, "missing", +1))
        assertEquals(dab.last(), RadioLogic.adjacent(dab, null, -1))
        assertNull(RadioLogic.adjacent(emptyList(), null, +1))
    }

    @Test fun defaultPresetsMatchDesignOrder() {
        val presets = RadioLogic.defaultPresets(dab)
        assertEquals(RadioLogic.PRESET_SLOTS, presets.size)
        val names = presets.take(6).map { id -> dab.first { it.id == id }.name }
        assertEquals(listOf("K-Music", "News Radio", "Classic FM", "Urban Hits", "Chill Out", "BBC World"), names)
    }

    @Test fun defaultPresetsPadWithEmptySlots() {
        val am = StationCatalog().stations(Band.AM)
        val presets = RadioLogic.defaultPresets(am)
        assertEquals(am.size, presets.count { it != null })
        assertNull(presets.last())
    }

    @Test fun assignPresetReplacesOnlyThatSlot() {
        val presets = RadioLogic.defaultPresets(dab)
        val updated = RadioLogic.assignPreset(presets, 2, "dab_jazz")
        assertEquals("dab_jazz", updated[2])
        assertEquals(presets[1], updated[1])
        assertEquals(presets, RadioLogic.assignPreset(presets, 99, "dab_jazz"))
    }

    @Test fun restorePresetsDropsUnknownIdsAndPads() {
        val known = dab.map { it.id }.toSet()
        val restored = RadioLogic.restorePresets(listOf("dab_news", "gone", null), known, emptyList())
        assertEquals(RadioLogic.PRESET_SLOTS, restored.size)
        assertEquals("dab_news", restored[0])
        assertNull(restored[1])
        val fallback = RadioLogic.defaultPresets(dab)
        assertEquals(fallback, RadioLogic.restorePresets(null, known, fallback))
    }

    @Test fun restorePresetsFallsBackWhenNothingIsReceivable() {
        val known = dab.map { it.id }.toSet()
        val fallback = RadioLogic.defaultPresets(dab)
        assertEquals(fallback, RadioLogic.restorePresets(listOf("old_1", "old_2", null), known, fallback))
    }

    @Test fun favoritesToggleAndFilter() {
        val favs = RadioLogic.toggle(emptySet(), "dab_kmusic")
        assertTrue("dab_kmusic" in favs)
        assertEquals(listOf(dab[0]), RadioLogic.filter(dab, favs, onlyFavorites = true))
        assertEquals(dab, RadioLogic.filter(dab, favs, onlyFavorites = false))
        assertTrue(RadioLogic.toggle(favs, "dab_kmusic").isEmpty())
    }

    @Test fun twoPresetPages() = assertEquals(2, RadioLogic.pageCount())
}
