package com.pleos.radio.domain

import java.util.Locale

/** Broadcast bands, in the order of the tabs on the main screen. */
enum class Band(val label: String) {
    FM("FM"),
    AM("AM"),
    DAB("DAB"),
    DRM("DRM"),
    ATSC("ATSC"),
}

/**
 * One receivable service.
 *
 * @param frequencyKhz carrier frequency in kHz (FM 89.1 MHz = 89100, DAB block centre e.g. 225648).
 * @param channel DAB block label such as "12B", or ATSC virtual channel such as "14-1"; null otherwise.
 * @param badge service-type badge shown above the name ("FM", "DAB+", "ATSC 3.0" …).
 * @param dynamicLabels rotating radiotext / DLS lines shown under the programme name.
 * @param signal received signal strength, 0..4 bars.
 * @param stream real audio source; null means the simulated tuner audio is used.
 */
data class Station(
    val id: String,
    val band: Band,
    val name: String,
    val frequencyKhz: Int,
    val channel: String? = null,
    val badge: String,
    val programme: String,
    val slogan: String,
    val genre: String,
    val dynamicLabels: List<String> = emptyList(),
    val signal: Int = 4,
    val stream: StreamSource? = null,
) {
    /** "89.1 MHz", "603 kHz", "12B  225.648 MHz", "CH 14-1  479 MHz". */
    val frequencyLabel: String
        get() = when (band) {
            Band.FM -> String.format(Locale.US, "%.1f MHz", frequencyKhz / 1000.0)
            Band.DAB -> String.format(Locale.US, "%s  %.3f MHz", channel ?: "", frequencyKhz / 1000.0).trim()
            Band.ATSC -> "CH ${channel ?: "?"}  ${frequencyKhz / 1000} MHz"
            Band.DRM, Band.AM -> "$frequencyKhz kHz"
        }

    /** Slogan first, then each dynamic label, cycling with [index]. */
    fun infoLine(index: Int): String {
        val lines = listOf(slogan) + dynamicLabels
        return lines[Math.floorMod(index, lines.size)]
    }
}
