package com.pleos.radio.data.translate

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import com.pleos.radio.domain.caption.CaptionLanguages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

class CloudException(val httpCode: Int, message: String) : IOException(message)

/**
 * Google Cloud Speech-to-Text v1 (sync recognize) and Cloud Translation v2, called over REST with an API key.
 * Sends the Android package / signing-certificate headers so the key can be restricted to this app.
 */
class GoogleCloudClient(context: Context) {

    private val packageName = context.packageName
    private val certSha1: String? = signingCertSha1(context)

    /** Transcribes 16 kHz mono PCM16 audio spoken in [languageCode] (BCP-47, e.g. "de-DE"). */
    suspend fun recognize(pcm16k: ShortArray, languageCode: String, apiKey: String): String = withContext(Dispatchers.IO) {
        val bytes = ByteBuffer.allocate(pcm16k.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            .apply { asShortBuffer().put(pcm16k) }.array()
        val config = JSONObject()
            .put("encoding", "LINEAR16")
            .put("sampleRateHertz", 16_000)
            .put("languageCode", languageCode)
        if (CaptionLanguages.supportsPunctuation(languageCode)) config.put("enableAutomaticPunctuation", true)
        val body = JSONObject()
            .put("config", config)
            .put("audio", JSONObject().put("content", Base64.encodeToString(bytes, Base64.NO_WRAP)))
        val results = post(SPEECH_URL, apiKey, body).optJSONArray("results") ?: JSONArray()
        buildString {
            for (i in 0 until results.length()) {
                val alt = results.getJSONObject(i).optJSONArray("alternatives")?.optJSONObject(0) ?: continue
                append(alt.optString("transcript").trim()).append(' ')
            }
        }.trim()
    }

    /** Translates [text] from [source] (e.g. "de") into [target] (e.g. "ko"). */
    suspend fun translate(text: String, source: String, target: String, apiKey: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject().put("q", text).put("source", source).put("target", target).put("format", "text")
        post(TRANSLATE_URL, apiKey, body)
            .getJSONObject("data").getJSONArray("translations").getJSONObject(0).getString("translatedText")
    }

    private fun post(url: String, apiKey: String, body: JSONObject): JSONObject {
        val conn = URL("$url?key=${URLEncoder.encode(apiKey, "UTF-8")}").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setRequestProperty("X-Android-Package", packageName)
        certSha1?.let { conn.setRequestProperty("X-Android-Cert", it) }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = runCatching { JSONObject(text).getJSONObject("error").getString("message") }
                    .getOrDefault("HTTP $code")
                throw CloudException(code, message)
            }
            return JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val SPEECH_URL = "https://speech.googleapis.com/v1/speech:recognize"
        const val TRANSLATE_URL = "https://translation.googleapis.com/language/translate/v2"
        const val TIMEOUT_MS = 15_000

        fun signingCertSha1(context: Context): String? = runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val cert = info.signingInfo?.apkContentsSigners?.firstOrNull() ?: return null
            MessageDigest.getInstance("SHA-1").digest(cert.toByteArray()).joinToString("") { "%02X".format(it) }
        }.getOrNull()
    }
}
