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
 * stations, DAB the BBC National DAB lineup plus German DAB+ stations, DRM All India Radio's DRM
 * transmitters and ATSC Korea's public terrestrial TV channels, each playing the broadcaster's
 * official internet stream ([StreamSource]). A HAL-backed implementation can replace this class later.
 */
class StationCatalog(private val random: Random = Random.Default) {

    fun stations(band: Band): List<Station> = stationsWithoutLogos(band).map { s ->
        s.copy(logoUrl = LOGOS[s.id], logoOnDark = s.id in LOGOS_ON_DARK)
    }

    private fun stationsWithoutLogos(band: Band): List<Station> = when (band) {
        Band.DAB -> DAB
        Band.DRM -> DRM
        Band.FM -> FM
        Band.ATSC -> ATSC
        Band.AM -> AM
    }

    /** Simulated band scan: same services, freshly measured signal strength. */
    fun scan(band: Band): List<Station> = stations(band).map { s ->
        if (s.stream != null) s // internet simulcast: no RF signal to measure
        else s.copy(signal = random.nextInt(1, 5))
    }

    private companion object {
        private const val KBS_LOGO = "https://padmin.static.kbs.co.kr/live/2025/3/"
        private const val BBC_LOGO = "https://sounds.files.bbci.co.uk/3.12.0/networks/"
        private const val AIR_LOGO = "https://dt89je83l7epy.cloudfront.net/images/"
        private const val ARD_IMAGE = "https://api.ardmediathek.de/image-service/images/urn:ard:image:"
        private const val CBS_LOGO = "https://www.cbs.co.kr/img/og_image.png"
        private const val SBS_LOGO = "https://www.sbs.co.kr/radio/img/sbs_radio.jpg"

        private fun bbcLogo(network: String) = "$BBC_LOGO$network/blocks-colour_600x600.png"

        /**
         * Station logos from each broadcaster's own servers (KBS live API, MBC/SBS/CBS/EBS/YTN/FEBC sites,
         * BBC Sounds, broadcaster web icons, ARD image service, Akashvani site). NDR blocks external access,
         * so NDR 2 keeps the band artwork.
         */
        val LOGOS = mapOf(
            // KBS (channel logos from the KBS live API)
            "fm_kbs_cool" to KBS_LOGO + "11/1741700004290_443824.png",
            "fm_kbs_classic" to KBS_LOGO + "11/1741700004216_443813.png",
            "fm_kbs_1radio" to KBS_LOGO + "7/1741313100641_442721.png",
            "fm_kbs_happy" to KBS_LOGO + "11/1741700004070_443799.png",
            "am_kbs_happy" to KBS_LOGO + "11/1741700004070_443799.png",
            "am_kbs_1radio" to KBS_LOGO + "7/1741313100641_442721.png",
            "am_kbs_hanminjok" to KBS_LOGO + "11/1741700004397_443835.png",
            "am_kbs_3radio" to KBS_LOGO + "11/1741700004143_443802.png",
            // Other Korean broadcasters
            "fm_mbc_fm4u" to "https://img.imbc.com/commons/2018/image/radio/logo-fm4u.png",
            "fm_mbc_standard" to "https://img.imbc.com/commons/2018/image/radio/logo-fm.png",
            "fm_sbs_power" to SBS_LOGO,
            "fm_sbs_love" to SBS_LOGO,
            "fm_cbs_music" to CBS_LOGO,
            "fm_cbs_standard" to CBS_LOGO,
            "am_cbs_standard" to CBS_LOGO,
            "fm_ebs" to "https://m.ebs.co.kr/common/img/ebs_logo.png",
            "fm_ytn" to "https://radio.ytn.co.kr/img/_top/new_logo_v2.jpg",
            "am_febc" to "https://seoul.febc.net/assets/images/contents/footer_logo.png",
            // BBC (BBC Sounds network artwork)
            "dab_bbc_world" to bbcLogo("bbc_world_service"),
            "dab_bbc_r1" to bbcLogo("bbc_radio_one"),
            "dab_bbc_1xtra" to bbcLogo("bbc_1xtra"),
            "dab_bbc_r2" to bbcLogo("bbc_radio_two"),
            "dab_bbc_r3" to bbcLogo("bbc_radio_three"),
            "dab_bbc_r4" to bbcLogo("bbc_radio_four"),
            "dab_bbc_r4x" to bbcLogo("bbc_radio_four_extra"),
            "dab_bbc_5live" to bbcLogo("bbc_radio_five_live"),
            "dab_bbc_6music" to bbcLogo("bbc_6music"),
            "dab_bbc_asian" to bbcLogo("bbc_asian_network"),
            // Germany
            "dab_de_dlf" to "https://www.deutschlandfunk.de/static/img/deutschlandfunk/icons/apple-touch-icon-180x180.png",
            "dab_de_dlfkultur" to "https://www.deutschlandfunkkultur.de/static/img/deutschlandfunk_kultur/icons/apple-touch-icon-180x180.png",
            "dab_de_dlfnova" to "https://www.deutschlandfunknova.de/apple-touch-icon.png",
            "dab_de_1live" to "https://www1.wdr.de/radio/1live/resources/img/favicon/apple-touch-icon.png",
            "dab_de_wdr2" to ARD_IMAGE + "61437343796eedf3?w=512",
            "dab_de_bayern3" to ARD_IMAGE + "52fd25fc45de7c6a?w=512",
            "dab_de_swr3" to "https://www.swr3.de/assets/swr3/icons/apple-touch-icon.png",
            "dab_de_hr3" to "https://www.hr3.de/favicon.png",
            "dab_de_radioeins" to "https://www.radioeins.de/content/dam/rbb/rbb/logos/touch/rad-152.png",
            // All India Radio (Akashvani channel artwork)
            "drm_air_mumbai_b" to AIR_LOGO + "airmumbaiasmita.jpg",
            "drm_air_patna" to AIR_LOGO + "airpatna.jpg",
            "drm_air_kolkata_a" to AIR_LOGO + "RADIO_817824.jpg",
            "drm_air_lucknow" to AIR_LOGO + "airlucknow.jpg",
            "drm_air_vb_chennai" to AIR_LOGO + "vividh-bharati.jpg",
            "drm_air_rajkot" to AIR_LOGO + "airrajkot.jpg",
            "drm_air_delhi_a" to AIR_LOGO + "RADIO_435004.jpg",
            "drm_air_kolkata_b" to AIR_LOGO + "RADIO_480353.jpg",
            "drm_air_mumbai_a" to AIR_LOGO + "sammumbai.jpg",
            "drm_air_vb_delhi" to AIR_LOGO + "RADIO_624699.jpeg",
            // Terrestrial TV
            "atsc_kbs1" to "https://padmin.static.kbs.co.kr/live/2025/2/3/1738538621612_433742.png",
            "atsc_kbs2" to "https://padmin.static.kbs.co.kr/live/2025/2/3/1738538621669_433753.png",
            "atsc_ebs1" to "https://m.ebs.co.kr/common/img/ebs_logo.png",
            "atsc_ebs2" to "https://m.ebs.co.kr/common/img/ebs_logo.png",
        )

        /** White-on-transparent logos. */
        val LOGOS_ON_DARK = setOf("am_febc")

        fun dab(id: String, name: String, programme: String, genre: String, stream: StreamSource,
                slogan: String = "BBC live stream · UK only") =
            Station(id, Band.DAB, name, 225_648, "12B", "DAB", programme, slogan, genre, signal = 4, stream = stream,
                speechLanguage = "en-GB")

        /** German DAB+ service: block + centre frequency of its home ensemble. */
        fun dabDe(id: String, name: String, block: String, khz: Int, programme: String, genre: String, streamUrl: String,
                  slogan: String = "ARD live stream · worldwide") =
            Station(id, Band.DAB, name, khz, block, "DAB+", programme, slogan, genre, signal = 4,
                stream = StreamSource.Direct(streamUrl), speechLanguage = "de-DE")

        /** BBC's own HLS live stream for a domestic station; the BBC CDN only serves it to UK addresses. */
        fun bbcUk(id: String) = StreamSource.Direct(
            "https://as-hls-uk-live.akamaized.net/pool_01505109/live/uk/$id/$id.isml/$id-audio%3d96000.norewind.m3u8"
        )

        val BBC_WORLD_SERVICE = StreamSource.Direct(
            "https://as-hls-ww-live.akamaized.net/pool_87948813/live/ww/bbc_world_service/bbc_world_service.isml/bbc_world_service-audio%3d96000.norewind.m3u8"
        )

        /** Prasar Bharati stream hosts: CloudFront paths ("airdelhi06/air006") or full wavespb URLs. */
        const val WAVES = "https://radio.wavespb.com/live/"

        fun drm(id: String, name: String, khz: Int, programme: String, genre: String, stream: String, pureDrm: Boolean = false,
                language: String = "hi-IN") =
            Station(
                id, Band.DRM, name, khz, null, "DRM", "AIR $programme",
                if (pureDrm) "Pure DRM · Akashvani live stream" else "Akashvani live stream · worldwide",
                genre, signal = 4, speechLanguage = language,
                stream = StreamSource.Direct(
                    if (stream.startsWith("https://")) stream
                    else "https://d3r9rhourdc02u.cloudfront.net/assets/radio/${stream.substringBefore('/')}/${stream.substringAfter('/')}_Auto.m3u8"
                ),
            )

        fun fm(id: String, name: String, mhz: Double, programme: String, genre: String, stream: StreamSource) =
            Station(id, Band.FM, name, Math.round(mhz * 1000).toInt(), null, "FM", programme,
                "Live internet simulcast", genre, signal = 4, stream = stream,
                speechLanguage = "ko-KR", translateCaptions = false)

        /** UHF channel centre frequency (Korea uses the North American 6 MHz plan: RF 14 = 473 MHz). */
        fun uhfCentreKhz(rf: Int) = (473 + (rf - 14) * 6) * 1000

        fun atsc(id: String, name: String, virtual: String, rf: Int, badge: String, programme: String, genre: String, stream: StreamSource) =
            Station(id, Band.ATSC, name, uhfCentreKhz(rf), "$virtual (RF $rf)", badge, programme,
                "Live TV audio · official stream", genre, signal = 4, stream = stream)

        fun am(id: String, name: String, khz: Int, programme: String, genre: String, stream: StreamSource,
               slogan: String = "Live internet simulcast") =
            Station(id, Band.AM, name, khz, null, "AM", programme, slogan, genre, signal = 4, stream = stream,
                speechLanguage = "ko-KR", translateCaptions = false)

        /** FEBC publishes one Seoul web stream (FM 106.9); AM 1188 carries the same station. */
        val FEBC_SEOUL = StreamSource.Direct("https://mlive3.febc.net/live5/seoulfm/playlist.m3u8")

        /**
         * BBC National DAB multiplex (block 12B, 225.648 MHz) followed by ten German DAB+ services.
         * World Service streams worldwide; the BBC serves its domestic stations to UK addresses only, so
         * outside the UK they report "UK only". World Service comes first so the DAB tab plays anywhere.
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

            // Germany (DAB+). Blocks per "Liste der DAB-Sender in Deutschland": Deutschlandradio on the national
            // Bundesmux 5C; ARD stations on their home state's public ensemble. All stream worldwide.
            dabDe("dab_de_dlf", "Deutschlandfunk", "5C", 178_352, "Deutschlandradio · nationwide", "News / Information",
                "https://st01.sslstream.dlf.de/dlf/01/128/mp3/stream.mp3", slogan = "Deutschlandradio live stream · worldwide"),
            dabDe("dab_de_dlfkultur", "Deutschlandfunk Kultur", "5C", 178_352, "Deutschlandradio · nationwide", "Culture",
                "https://st02.sslstream.dlf.de/dlf/02/128/mp3/stream.mp3", slogan = "Deutschlandradio live stream · worldwide"),
            dabDe("dab_de_dlfnova", "Deutschlandfunk Nova", "5C", 178_352, "Deutschlandradio · nationwide", "Youth / Knowledge",
                "https://st03.sslstream.dlf.de/dlf/03/128/mp3/stream.mp3", slogan = "Deutschlandradio live stream · worldwide"),
            dabDe("dab_de_1live", "1LIVE", "11D", 222_064, "WDR · Nordrhein-Westfalen", "Pop / Youth",
                "https://wdr-1live-live.icecastssl.wdr.de/wdr/1live/live/mp3/128/stream.mp3"),
            dabDe("dab_de_wdr2", "WDR 2", "11D", 222_064, "WDR · Nordrhein-Westfalen", "Pop / News",
                "https://wdr-wdr2-rheinland.icecastssl.wdr.de/wdr/wdr2/rheinland/mp3/128/stream.mp3"),
            dabDe("dab_de_bayern3", "Bayern 3", "11D", 222_064, "BR · Bayern", "Pop",
                "https://dispatcher.rndfnk.com/br/br3/live/mp3/mid"),
            dabDe("dab_de_ndr2", "NDR 2", "7A", 188_928, "NDR · Niedersachsen (Hannover)", "Pop",
                "https://icecast.ndr.de/ndr/ndr2/niedersachsen/mp3/128/stream.mp3"),
            dabDe("dab_de_swr3", "SWR3", "9D", 208_064, "SWR · Baden-Württemberg (Stuttgart)", "Pop",
                "https://liveradio.swr.de/sw282p3/swr3/play.mp3"),
            dabDe("dab_de_hr3", "hr3", "7B", 190_640, "hr · Hessen", "Pop",
                "https://dispatcher.rndfnk.com/hr/hr3/live/mp3/high"),
            dabDe("dab_de_radioeins", "radioeins", "7D", 194_064, "rbb · Berlin", "Alternative",
                "https://dispatcher.rndfnk.com/rbb/radioeins/live/mp3/mid"),
        )

        /**
         * All India Radio (Akashvani) DRM medium-wave transmitters. DRM frequencies per DRM.org's broadcast
         * schedule (updated 26 Oct 2026); programme per transmitter per the VU2JOS Akashvani frequency list.
         * Streams are Prasar Bharati's official live streams (24 h; simulcast transmitters run DRM part-time).
         */
        val DRM = listOf(
            drm("drm_air_mumbai_b", "AIR Mumbai Asmita", 549, "Mumbai B · Asmita", "Marathi", "airmum04/air052", language = "mr-IN"),
            drm("drm_air_patna", "AIR Patna", 612, "Patna", "Hindi / Bhojpuri", WAVES + "c398958b3874b441/c398958b3874b441.m3u8"),
            drm("drm_air_kolkata_a", "AIR Kolkata Geetanjali", 648, "Kolkata A · Geetanjali", "Bengali", "airkol01/air065", language = "bn-IN"),
            drm("drm_air_lucknow", "AIR Lucknow", 738, "Lucknow", "Hindi", WAVES + "75c01e54cb46ad29/75c01e54cb46ad29.m3u8"),
            drm("drm_air_vb_chennai", "Vividh Bharati", 783, "Chennai C · Vividh Bharati", "Hindi film music",
                WAVES + "146ed6ec6dea5a24/146ed6ec6dea5a24.m3u8", pureDrm = true),
            drm("drm_air_rajkot", "AIR Rajkot", 801, "Rajkot", "Gujarati", WAVES + "a70ba921067c93ce/a70ba921067c93ce.m3u8",
                language = "gu-IN"),
            drm("drm_air_delhi_a", "AIR Delhi Indraprastha", 810, "New Delhi A · Indraprastha", "Hindi / English", "airdelhi06/air006"),
            drm("drm_air_kolkata_b", "AIR Kolkata Sanchayita", 999, "Kolkata B · Sanchayita", "Bengali", "airkol02/air066", language = "bn-IN"),
            drm("drm_air_mumbai_a", "AIR Mumbai Samvadita", 1_044, "Mumbai A · Samvadita", "Hindi / English", "airmum03/air051",
                pureDrm = true),
            drm("drm_air_vb_delhi", "Vividh Bharati Delhi", 1_368, "New Delhi C · Vividh Bharati", "Hindi film music",
                "airdelhi04/air004", pureDrm = true),
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

        /**
         * Korean terrestrial TV (Seoul head-office channels, per the stations' Korean Wikipedia infoboxes):
         * KBS on its ATSC 3.0 UHD channels, EBS on ATSC 1.0 RF 18. Plays the audio of each broadcaster's
         * official live TV stream. MBC and SBS do not allow third-party access to their live TV streams.
         */
        val ATSC = listOf(
            atsc("atsc_kbs2", "KBS 2TV", "7-1", 56, "ATSC 3.0", "KBS 2TV · UHD", "Entertainment / Drama", StreamSource.Kbs(12)),
            atsc("atsc_kbs1", "KBS 1TV", "9-1", 52, "ATSC 3.0", "KBS 1TV · UHD", "News / Documentary", StreamSource.Kbs(11)),
            atsc("atsc_ebs1", "EBS 1TV", "10-1", 18, "ATSC", "EBS 1TV · HD", "Education / Documentary",
                StreamSource.Direct("https://ebsonair.ebs.co.kr/ebs1familypc/familypc1m/playlist.m3u8")),
            atsc("atsc_ebs2", "EBS 2TV", "10-2", 18, "ATSC", "EBS 2TV · HD", "Education",
                StreamSource.Direct("https://ebsonair.ebs.co.kr/ebs2familypc/familypc1m/playlist.m3u8")),
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
