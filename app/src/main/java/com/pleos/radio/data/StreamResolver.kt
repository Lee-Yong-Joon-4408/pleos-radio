package com.pleos.radio.data

import com.pleos.radio.domain.StreamSource
import com.pleos.radio.domain.StreamUrls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Turns a [StreamSource] into a playable HLS URL, asking the broadcaster's API for a fresh signed URL. */
class StreamResolver {

    suspend fun resolve(source: StreamSource): String = withContext(Dispatchers.IO) {
        when (source) {
            is StreamSource.Direct -> source.url
            is StreamSource.Kbs -> StreamUrls.parseKbsServiceUrl(
                get("https://cfpwwwapi.kbs.co.kr/api/v1/landing/live/channel_code/${source.channelCode}")
            ) ?: throw IOException("KBS: no stream URL in response")
            is StreamSource.Mbc -> StreamUrls.validHttpsUrl(
                get("https://sminiplay.imbc.com/aacplay.ashx?agent=webapp&channel=${source.channel}")
            ) ?: throw IOException("MBC: unexpected response")
            is StreamSource.Sbs -> StreamUrls.validHttpsUrl(
                get("https://apis.sbs.co.kr/play-api/1.0/livestream/${source.path}?protocol=hls&ssl=Y")
            ) ?: throw IOException("SBS: unexpected response")
        }
    }

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        conn.setRequestProperty("User-Agent", USER_AGENT)
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code from ${conn.url.host}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 8_000
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android) PleosRadio"
    }
}
