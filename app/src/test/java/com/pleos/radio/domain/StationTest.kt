package com.pleos.radio.domain

import com.pleos.radio.data.StationCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StationTest {

    private val catalog = StationCatalog()

    @Test fun dabFrequencyLabelMatchesDesign() {
        assertEquals("12C  225.648 MHz", catalog.stations(Band.DAB).first().frequencyLabel)
    }

    @Test fun fmUsesMhzWithOneDecimal() {
        assertEquals("89.1 MHz", catalog.stations(Band.FM).first().frequencyLabel)
        assertEquals("FM", catalog.stations(Band.FM).first().badge)
    }

    @Test fun tabsAreInRequestedOrder() {
        assertEquals(listOf("FM", "AM", "DAB", "DRM", "ATSC"), Band.entries.map { it.label })
    }

    @Test fun everyFmStationHasARealStreamOthersAreSimulated() {
        assertTrue(catalog.stations(Band.FM).all { it.stream != null })
        listOf(Band.AM, Band.DAB, Band.DRM, Band.ATSC).forEach { band ->
            assertTrue(catalog.stations(band).all { it.stream == null })
        }
    }

    @Test fun fmStationsAreSortedByFrequency() {
        val freqs = catalog.stations(Band.FM).map { it.frequencyKhz }
        assertEquals(freqs.sorted(), freqs)
        assertEquals("107.7 MHz", catalog.stations(Band.FM).last().frequencyLabel)
    }

    @Test fun atscShowsVirtualChannel() {
        assertEquals("CH 14-1  479 MHz", catalog.stations(Band.ATSC).first().frequencyLabel)
    }

    @Test fun amAndDrmUseKhz() {
        assertEquals("603 kHz", catalog.stations(Band.AM).first().frequencyLabel)
        assertEquals("3955 kHz", catalog.stations(Band.DRM).first().frequencyLabel)
    }

    @Test fun infoLineStartsWithSloganAndCycles() {
        val s = catalog.stations(Band.DAB).first()
        assertEquals("Your Daily Music Station", s.infoLine(0))
        assertEquals(s.dynamicLabels[0], s.infoLine(1))
        assertEquals(s.infoLine(0), s.infoLine(s.dynamicLabels.size + 1))
    }

    @Test fun scanKeepsServicesAndValidSignal() {
        Band.entries.forEach { band ->
            val scanned = catalog.scan(band)
            assertEquals(catalog.stations(band).map { it.id }, scanned.map { it.id })
            assertTrue(scanned.all { it.signal in 1..4 })
            assertEquals(catalog.stations(band).map { it.stream }, scanned.map { it.stream })
        }
    }

    @Test fun stationIdsAreUnique() {
        val ids = Band.entries.flatMap { catalog.stations(it) }.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
