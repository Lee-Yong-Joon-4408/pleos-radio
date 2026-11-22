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
        }
    }

    @Test fun stationIdsAreUnique() {
        val ids = Band.entries.flatMap { catalog.stations(it) }.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
