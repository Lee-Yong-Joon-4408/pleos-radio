package com.pleos.radio.data

import com.pleos.radio.domain.Band
import com.pleos.radio.domain.Station
import com.pleos.radio.domain.StreamSource
import kotlin.random.Random

/**
 * Broadcast data source.
 *
 * Third-party Pleos Connect / AAOS apps cannot open the vehicle's broadcast-radio HAL
 * (`android.hardware.radio` is a system API). FM and AM therefore list the real Seoul-area
 * stations and DAB the BBC National DAB lineup, each playing the broadcaster's official internet
 * stream ([StreamSource]); DRM and ATSC use a fixed simulated service list. A HAL-backed implementation can replace this class later.
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
        else s.copy(signal = random.nextInt(if (band == Band.ATSC) 2 else 1, 5))
    }

    private companion object {
        fun dab(id: String, name: String, programme: String, genre: String, stream: StreamSource,
                slogan: String = "BBC live stream · UK only") =
            Station(id, Band.DAB, name, 225_648, "12B", "DAB", programme, slogan, genre, signal = 4, stream = stream)

        /** BBC's own HLS live stream for a domestic station; the BBC CDN only serves it to UK addresses. */
        fun bbcUk(id: String) = StreamSource.Direct(
            "https://as-hls-uk-live.akamaized.net/pool_01505109/live/uk/$id/$id.isml/$id-audio%3d96000.norewind.m3u8"
        )

        val BBC_WORLD_SERVICE = StreamSource.Direct(
            "https://as-hls-ww-live.akamaized.net/pool_87948813/live/ww/bbc_world_service/bbc_world_service.isml/bbc_world_service-audio%3d96000.norewind.m3u8"
        )

        fun drm(id: String, name: String, khz: Int, programme: String, slogan: String, genre: String, vararg dls: String, signal: Int = 3) =
            Station(id, Band.DRM, name, khz, null, "DRM", programme, slogan, genre, dls.toList(), signal)

        fun fm(id: String, name: String, mhz: Double, programme: String, genre: String, stream: StreamSource) =
            Station(id, Band.FM, name, Math.round(mhz * 1000).toInt(), null, "FM", programme,
                "Live internet simulcast", genre, signal = 4, stream = stream)

        fun atsc(id: String, name: String, channel: String, mhz: Int, programme: String, slogan: String, genre: String, vararg dls: String, signal: Int = 4) =
            Station(id, Band.ATSC, name, mhz * 1000, channel, "ATSC 3.0", programme, slogan, genre, dls.toList(), signal)

        fun am(id: String, name: String, khz: Int, programme: String, genre: String, stream: StreamSource,
               slogan: String = "Live internet simulcast") =
            Station(id, Band.AM, name, khz, null, "AM", programme, slogan, genre, signal = 4, stream = stream)

        /** FEBC publishes one Seoul web stream (FM 106.9); AM 1188 carries the same station. */
        val FEBC_SEOUL = StreamSource.Direct("https://mlive3.febc.net/live5/seoulfm/playlist.m3u8")

        /**
         * BBC National DAB multiplex (block 12B, 225.648 MHz). World Service streams worldwide; the
         * BBC serves its domestic stations to UK addresses only, so outside the UK they report "UK only".
         * World Service comes first so the DAB tab plays out of the box anywhere.
         */
        val DAB = listOf(
            dab("dab_bbc_world", "BBC World Service", "International news and analysis", "News", BBC_WORLD_SERVICE,
                slogan = "BBC live stream · worldwide"),
            dab("dab_bbc_r1", "BBC Radio 1", "New music and entertainment", "Pop", bbcUk("bbc_radio_one")),
            dab("dab_bbc_1xtra", "BBC Radio 1Xtra", "Hip-hop, R&B, grime and Afrobeats", "Urban", bbcUk("bbc_1xtra")),
            dab("dab_bbc_r2", "BBC Radio 2", "The UK's most listened-to station", "Adult contemporary", bbcUk("bbc_radio_two")),
            dab("dab_bbc_r3", "BBC Radio 3", "Classical music, jazz and arts", "Classical", bbcUk("bbc_radio_three")),
            dab("dab_bbc_r4", "BBC Radio 4", "News, drama, comedy and documentaries", "Speech", bbcUk("bbc_radio_fourfm")),
            dab("dab_bbc_r4x", "BBC Radio 4 Extra", "Comedy, drama and stories", "Speech", bbcUk("bbc_radio_four_extra")),
            dab("dab_bbc_5live", "BBC Radio 5 Live", "Live news and sport", "News / Sport", bbcUk("bbc_radio_five_live")),
            dab("dab_bbc_6music", "BBC Radio 6 Music", "Alternative and independent music", "Alternative", bbcUk("bbc_6music")),
            dab("dab_bbc_asian", "BBC Asian Network", "British Asian music and talk", "Asian", bbcUk("bbc_asian_network")),
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

        /** Seoul-area medium-wave stations (kHz, ascending) with their broadcaster's live stream. */
        val AM = listOf(
            am("am_kbs_happy", "KBS 해피FM", 603, "KBS 2Radio · AM 603", "Variety / Trot", StreamSource.Kbs(22)),
            am("am_kbs_1radio", "KBS 1라디오", 711, "KBS 1Radio · AM 711", "News / Current affairs", StreamSource.Kbs(21)),
            am("am_cbs_standard", "CBS 표준FM", 837, "CBS Standard FM · AM 837", "News / Talk", cbs("cbs981")),
            am("am_kbs_hanminjok", "KBS 한민족방송", 972, "KBS Hanminjok Radio · AM 972", "Overseas Koreans", StreamSource.Kbs(26)),
            am("am_kbs_3radio", "KBS 3라디오", 1_134, "KBS 3Radio · AM 1134", "Accessibility / Social welfare", StreamSource.Kbs(23)),
            am("am_febc", "극동방송", 1_188, "FEBC Seoul · AM 1188", "Christian", FEBC_SEOUL,
                slogan = "Live stream (same programme as FM 106.9)"),
        )
    }
}
