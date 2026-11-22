package com.pleos.radio.domain

import java.util.Locale

/** Broadcast bands shown as tabs on the main screen. */
enum class Band(val label: String) {
    DAB("DAB"),
    DRM("DRM"),
    AM("AM"),
}

/**
 * One receivable service.
 *
 * @param frequencyKhz carrier frequency in kHz (DAB: block centre frequency, e.g. 225648).
 * @param channel DAB block label such as "12C"; null for DRM/AM.
 * @param badge service-type badge shown above the name ("DAB+", "DRM", "AM").
 * @param dynamicLabels rotating radiotext / DLS lines shown under the programme name.
 * @param signal received signal strength, 0..4 bars.
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
) {
    /** "12C  225.648 MHz" for DAB, "3955 kHz" for DRM/AM. */
    val frequencyLabel: String
        get() = when (band) {
            Band.DAB -> String.format(Locale.US, "%s  %.3f MHz", channel ?: "", frequencyKhz / 1000.0).trim()
            Band.DRM, Band.AM -> "$frequencyKhz kHz"
        }

    /** Slogan first, then each dynamic label, cycling with [index]. */
    fun infoLine(index: Int): String {
        val lines = listOf(slogan) + dynamicLabels
        return lines[Math.floorMod(index, lines.size)]
    }
}
