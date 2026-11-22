package com.pleos.radio.domain

/**
 * Where a station's real audio comes from.
 *
 * Third-party apps cannot use the vehicle's broadcast tuner, so Korean FM stations play the
 * broadcaster's official internet simulcast instead. KBS, MBC and SBS hand out short-lived signed
 * URLs, so those are resolved through the broadcaster's API every time playback starts.
 */
sealed interface StreamSource {
    /** Fixed HLS playlist URL (CBS, EBS, YTN). */
    data class Direct(val url: String) : StreamSource

    /** KBS live API channel code: 21 1Radio, 22 Happy FM, 24 Classic FM, 25 Cool FM. */
    data class Kbs(val channelCode: Int) : StreamSource

    /** MBC mini player channel: "sfm" (표준FM), "mfm" (FM4U). */
    data class Mbc(val channel: String) : StreamSource

    /** SBS play-api path: "powerpc/powerfm", "lovepc/lovefm". */
    data class Sbs(val path: String) : StreamSource
}

/** Pure parsing helpers for the broadcaster APIs (unit-tested). */
object StreamUrls {

    private val KBS_SERVICE_URL = Regex("\"service_url\"\\s*:\\s*\"([^\"]+)\"")

    /** First `service_url` in the KBS live API JSON, unescaped. */
    fun parseKbsServiceUrl(json: String): String? =
        KBS_SERVICE_URL.find(json)?.groupValues?.get(1)
            ?.replace("\\/", "/")
            ?.replace("\\u0026", "&")
            ?.let(::validHttpsUrl)

    /** MBC / SBS APIs answer with the bare playlist URL as plain text. */
    fun validHttpsUrl(text: String): String? =
        text.trim().takeIf { it.startsWith("https://") && it.length < 4096 && it.none(Char::isWhitespace) }
}
