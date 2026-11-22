package com.pleos.radio.data.translate

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Free, on-device translation with Google ML Kit. Each language model (~30 MB) is downloaded once on
 * first use and then works offline. Clients are cached per language pair.
 */
class OnDeviceTranslator {

    private val clients = mutableMapOf<Pair<String, String>, Translator>()

    /** [source]/[target] are ISO codes such as "de", "hi", "ko", "zh-CN". */
    suspend fun translate(text: String, source: String, target: String): String {
        val client = client(source, target)
        client.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
        return client.translate(text).await()
    }

    @Synchronized
    private fun client(source: String, target: String): Translator {
        val src = mlKitCode(source) ?: throw IllegalArgumentException("Unsupported source language: $source")
        val dst = mlKitCode(target) ?: throw IllegalArgumentException("Unsupported target language: $target")
        return clients.getOrPut(src to dst) {
            Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(src).setTargetLanguage(dst).build())
        }
    }

    fun close() = synchronized(this) { clients.values.forEach { it.close() }; clients.clear() }

    companion object {
        /** ML Kit language codes are bare ISO-639-1 ("zh" for Chinese). */
        fun mlKitCode(code: String): String? = TranslateLanguage.fromLanguageTag(code.substringBefore('-').lowercase())
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
