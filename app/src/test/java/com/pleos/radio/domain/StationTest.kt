package com.pleos.radio.domain

import com.pleos.radio.data.StationCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StationTest {

    private val catalog = StationCatalog()

    @Test fun dabIsTenBbcThenTenGermanServices() {
        val dab = catalog.stations(Band.DAB)
        assertEquals(20, dab.size)
        val (bbc, german) = dab.partition { it.name.startsWith("BBC") }
        assertEquals(10, bbc.size)
        assertTrue(bbc.all { it.frequencyLabel == "12B  225.648 MHz" && it.badge == "DAB" })
        assertEquals("BBC World Service", dab.first().name)
        assertEquals(10, german.size)
        assertTrue(german.all { it.badge == "DAB+" })
        assertEquals("5C  178.352 MHz", german.first { it.name == "Deutschlandfunk" }.frequencyLabel)
        assertEquals("11D  222.064 MHz", german.first { it.name == "Bayern 3" }.frequencyLabel)
    }

    @Test fun fmUsesMhzWithOneDecimal() {
        assertEquals("89.1 MHz", catalog.stations(Band.FM).first().frequencyLabel)
        assertEquals("FM", catalog.stations(Band.FM).first().badge)
    }

    @Test fun tabsAreInRequestedOrder() {
        assertEquals(listOf("FM", "AM", "DAB", "DRM", "ATSC"), Band.entries.map { it.label })
    }

    @Test fun onlyAtscIsSimulated() {
        listOf(Band.FM, Band.AM, Band.DAB, Band.DRM).forEach { band ->
            assertTrue(catalog.stations(band).all { it.stream != null })
        }
        listOf(Band.ATSC).forEach { band ->
            assertTrue(catalog.stations(band).all { it.stream == null })
        }
    }

    @Test fun fmStationsAreSortedByFrequency() {
        val freqs = catalog.stations(Band.FM).map { it.frequencyKhz }
        assertEquals(freqs.sorted(), freqs)
        assertEquals("107.7 MHz", catalog.stations(Band.FM).last().frequencyLabel)
    }

    @Test fun amStationsAreRealSeoulFrequencies() {
        assertEquals(listOf(603, 711, 837, 972, 1134, 1188), catalog.stations(Band.AM).map { it.frequencyKhz })
    }

    @Test fun drmIsAllIndiaRadioOnRealDrmFrequencies() {
        val drm = catalog.stations(Band.DRM)
        assertEquals(listOf(549, 612, 648, 738, 783, 801, 810, 999, 1044, 1368), drm.map { it.frequencyKhz })
        assertTrue(drm.all { (it.stream as StreamSource.Direct).url.let { u -> u.startsWith("https://") && u.endsWith(".m3u8") } })
        assertEquals(
            "https://d3r9rhourdc02u.cloudfront.net/assets/radio/airdelhi06/air006_Auto.m3u8",
            (drm.first { it.id == "drm_air_delhi_a" }.stream as StreamSource.Direct).url,
        )
    }

    @Test fun realStationsHaveHttpsLogosExceptNdr2() {
        val real = listOf(Band.FM, Band.AM, Band.DAB, Band.DRM).flatMap { catalog.stations(it) }
        val missing = real.filter { it.logoUrl == null }.map { it.id }
        assertEquals(listOf("dab_de_ndr2"), missing)
        assertTrue(real.mapNotNull { it.logoUrl }.all { it.startsWith("https://") })
        assertTrue(catalog.stations(Band.ATSC).all { it.logoUrl == null })
        assertTrue(catalog.scan(Band.FM).all { it.logoUrl != null })
    }

    @Test fun atscShowsVirtualChannel() {
        assertEquals("CH 14-1  479 MHz", catalog.stations(Band.ATSC).first().frequencyLabel)
    }

    @Test fun amAndDrmUseKhz() {
        assertEquals("603 kHz", catalog.stations(Band.AM).first().frequencyLabel)
        assertEquals("549 kHz", catalog.stations(Band.DRM).first().frequencyLabel)
    }

    @Test fun infoLineStartsWithSloganAndCycles() {
        val s = catalog.stations(Band.ATSC).first()
        assertEquals("Round-the-clock UHD news audio", s.infoLine(0))
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
