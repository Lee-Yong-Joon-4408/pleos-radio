package com.pleos.radio.data

import com.pleos.radio.domain.Band
import com.pleos.radio.domain.Station
import com.pleos.radio.domain.StreamSource
import kotlin.random.Random

/**
 * Broadcast data source.
 *
 * Third-party Pleos Connect / AAOS apps cannot open the vehicle's broadcast-radio HAL
 * (`android.hardware.radio` is a system API). FM therefore lists the real Seoul-area stations and
 * plays each broadcaster's official internet simulcast ([StreamSource]); AM, DAB, DRM and ATSC
 * use a fixed simulated service list. A HAL-backed implementation can replace this class later.
 */
class StationCatalog(private val random: Random = Random.Default) {

    fun stations(band: Band): List<Station> = when (band) {
        Band.DAB -> DAB
        Band.DRM -> DRM
        Band.FM -> FM
        Band.ATSC -> ATSC
        Band.AM -> AM
    }

    /** Simulated band scan: same services, freshly measured signal strength. */
    fun scan(band: Band): List<Station> = stations(band).map { s ->
        if (s.stream != null) s // internet simulcast: no RF signal to measure
        else s.copy(signal = random.nextInt(if (band == Band.DAB || band == Band.ATSC) 2 else 1, 5))
    }

    private companion object {
        fun dab(id: String, name: String, ch: String, khz: Int, programme: String, slogan: String, genre: String, vararg dls: String, signal: Int = 4) =
            Station(id, Band.DAB, name, khz, ch, "DAB+", programme, slogan, genre, dls.toList(), signal)

        fun drm(id: String, name: String, khz: Int, programme: String, slogan: String, genre: String, vararg dls: String, signal: Int = 3) =
            Station(id, Band.DRM, name, khz, null, "DRM", programme, slogan, genre, dls.toList(), signal)

        fun fm(id: String, name: String, mhz: Double, programme: String, genre: String, stream: StreamSource) =
            Station(id, Band.FM, name, Math.round(mhz * 1000).toInt(), null, "FM", programme,
                "Live internet simulcast", genre, signal = 4, stream = stream)

        fun atsc(id: String, name: String, channel: String, mhz: Int, programme: String, slogan: String, genre: String, vararg dls: String, signal: Int = 4) =
            Station(id, Band.ATSC, name, mhz * 1000, channel, "ATSC 3.0", programme, slogan, genre, dls.toList(), signal)

        fun am(id: String, name: String, khz: Int, programme: String, slogan: String, genre: String, vararg dls: String, signal: Int = 3) =
            Station(id, Band.AM, name, khz, null, "AM", programme, slogan, genre, dls.toList(), signal)

        val DAB = listOf(
            dab("dab_kmusic", "K-Music", "12C", 225_648, "Good Vibes", "Your Daily Music Station", "K-Pop",
                "Now playing: Midnight Drive – The Lumens", "Next: Summer Letter – Haneul", "Request line open until 11 AM"),
            dab("dab_news", "News Radio", "12C", 225_648, "Morning Briefing", "News every hour, on the hour", "News",
                "Headlines: markets open higher", "Weather: clear skies, 18°C"),
            dab("dab_classic", "Classic FM", "11D", 222_064, "Morning Classics", "Timeless music for the road", "Classical",
                "Now playing: Vivaldi – Spring (Allegro)", "Next: Debussy – Clair de Lune"),
            dab("dab_urban", "Urban Hits", "11D", 222_064, "Hit Factory", "The freshest R&B and hip-hop", "Hip-Hop",
                "Now playing: City Lights – NOVA", "Top 20 countdown at 6 PM"),
            dab("dab_chill", "Chill Out", "12A", 223_936, "Easy Morning", "Relax, breathe, drive", "Chill",
                "Now playing: Ocean Floor – Sora", "Lo-fi beats all day long"),
            dab("dab_world", "BBC World", "12A", 223_936, "World Update", "Global news and analysis", "News",
                "Live: World Business Report", "Coming up: Science in Action"),
            dab("dab_jazz", "Jazz Lounge", "10B", 211_648, "Blue Hour", "Smooth jazz for smooth roads", "Jazz",
                "Now playing: Autumn Walk – Trio Lumen", signal = 3),
            dab("dab_rock", "Rock Nation", "10B", 211_648, "Drive Time Rock", "Turn it up", "Rock",
                "Now playing: Highway Fire – Static Kings", signal = 3),
            dab("dab_traffic", "Traffic Info", "9A", 202_928, "Road Watch", "Live traffic every 10 minutes", "Traffic",
                "Gyeongbu Expwy: slow near Hannam IC", "Olympic-daero: clear"),
            dab("dab_kids", "Kids Radio", "9A", 202_928, "Sing Along", "Songs and stories for the back seat", "Kids",
                "Now playing: The Little Star Song", signal = 2),
            dab("dab_talk", "Talk Seoul", "8C", 199_360, "City Talk", "Your city, your voice", "Talk",
                "Today: weekend festivals around town", signal = 3),
            dab("dab_retro", "Retro 90s", "8C", 199_360, "Rewind", "The best of the 90s", "Pop",
                "Now playing: Neon Summer – Pixel Hearts", signal = 3),
        )

        val DRM = listOf(
            drm("drm_global", "Global News 24", 3_955, "World at Six", "Digital shortwave news, worldwide", "News",
                "Live: Asia-Pacific headlines"),
            drm("drm_asia", "Asia Pacific Voice", 7_330, "Pacific Morning", "Voices from across the region", "Talk",
                "Interview: ocean research in Busan"),
            drm("drm_euroclassic", "Euro Classic", 6_095, "Concert Hall", "Classical music in digital clarity", "Classical",
                "Now playing: Brahms – Symphony No. 4", signal = 2),
            drm("drm_science", "Science Hour", 9_620, "Discovery", "Curious minds on air", "Science",
                "Today: the physics of radio waves", signal = 2),
            drm("drm_jazz", "Shortwave Jazz", 11_690, "Night Owls", "Jazz across the ionosphere", "Jazz",
                "Now playing: Blue Horizon – Mira Quartet"),
            drm("drm_test", "DRM Test Radio", 15_120, "Test Transmission", "xHE-AAC digital audio test", "Test",
                "Mode B · 10 kHz · 64-QAM", signal = 4),
        )

        private fun cbs(stream: String) =
            StreamSource.Direct("https://m-aac.cbs.co.kr/mweb_$stream/_definst_/$stream.stream/playlist.m3u8")

        /** Seoul metropolitan area frequencies, ascending. */
        val FM = listOf(
            fm("fm_kbs_cool", "KBS 쿨FM", 89.1, "KBS 2FM", "Pop / K-Pop", StreamSource.Kbs(25)),
            fm("fm_mbc_fm4u", "MBC FM4U", 91.9, "MBC FM4U", "Music", StreamSource.Mbc("mfm")),
            fm("fm_kbs_classic", "KBS 클래식FM", 93.1, "KBS 1FM", "Classical", StreamSource.Kbs(24)),
            fm("fm_cbs_music", "CBS 음악FM", 93.9, "CBS Music FM", "Music", cbs("cbs939")),
            fm("fm_ytn", "YTN 라디오", 94.5, "YTN Radio", "News", StreamSource.Direct("https://radiolive.ytn.co.kr/radio/_definst_/20211118_fmlive/playlist.m3u8")),
            fm("fm_mbc_standard", "MBC 표준FM", 95.9, "MBC Standard FM", "Talk / Music", StreamSource.Mbc("sfm")),
            fm("fm_kbs_1radio", "KBS 1라디오", 97.3, "KBS 1Radio", "News / Current affairs", StreamSource.Kbs(21)),
            fm("fm_cbs_standard", "CBS 표준FM", 98.1, "CBS Standard FM", "News / Talk", cbs("cbs981")),
            fm("fm_sbs_love", "SBS 러브FM", 103.5, "SBS Love FM", "Talk / Music", StreamSource.Sbs("lovepc/lovefm")),
            fm("fm_ebs", "EBS FM", 104.5, "EBS FM", "Education", StreamSource.Direct("https://ebsonair.ebs.co.kr/fmradiofamilypc/familypc1m/playlist.m3u8")),
            fm("fm_kbs_happy", "KBS 해피FM", 106.1, "KBS 2Radio", "Variety / Trot", StreamSource.Kbs(22)),
            fm("fm_sbs_power", "SBS 파워FM", 107.7, "SBS Power FM", "Pop / K-Pop", StreamSource.Sbs("powerpc/powerfm")),
        )

        /** Simulated ATSC 3.0 audio services (virtual channel, RF centre frequency). */
        val ATSC = listOf(
            atsc("atsc_news", "UHD News 24", "14-1", 479, "News Now", "Round-the-clock UHD news audio", "News",
                "Live: evening headlines", "Weather: rain moving east tonight"),
            atsc("atsc_music", "UHD Music", "16-1", 491, "Concert Live", "Live concerts in immersive audio", "Music",
                "Now playing: Seoul Philharmonic – Dvořák 9"),
            atsc("atsc_sports", "UHD Sports", "18-1", 503, "Match Day", "Live commentary for the road", "Sports",
                "Football: 1–0, 63rd minute", signal = 3),
            atsc("atsc_drama", "UHD Drama", "21-1", 521, "Prime Drama", "Listen to tonight's drama", "Drama",
                "Episode 12 – The Last Letter", signal = 3),
            atsc("atsc_kids", "UHD Kids", "23-1", 533, "Story Time", "Cartoons and songs for kids", "Kids",
                "Now: Little Dino Adventures", signal = 2),
            atsc("atsc_docs", "UHD Docs", "25-1", 545, "Planet Earth Korea", "Documentaries in sound", "Documentary",
                "Today: tidal flats of the west coast", signal = 3),
        )

        val AM = listOf(
            am("am_traffic", "Traffic AM", 603, "Road Report", "Traffic and weather together", "Traffic",
                "Seoul ring road: moving well"),
            am("am_seoultalk", "Seoul Talk", 711, "Morning Call", "Talk radio for the commute", "Talk",
                "Phone-in: your best road trip"),
            am("am_hanbit", "Hanbit News", 837, "News Desk", "Trusted news since 1961", "News",
                "Headlines at the top of the hour", signal = 4),
            am("am_classic", "Classic AM", 972, "Golden Melodies", "Songs you grew up with", "Oldies",
                "Now playing: Moonlit River – Lee Seon", signal = 2),
            am("am_sports", "Sports Talk", 1_134, "Game Day", "Live scores and analysis", "Sports",
                "Baseball: 3–2, top of the 7th"),
            am("am_heritage", "Heritage Radio", 1_188, "Our Songs", "Traditional music and stories", "Traditional",
                "Now playing: Arirang (Gayageum)", signal = 2),
            am("am_latenight", "Late Night AM", 1_350, "After Hours", "Company for the night drive", "Talk",
                "Stories from the road", signal = 1),
        )
    }
}
