package com.pleos.radio.data.translate

/** Thin JNI surface over whisper.cpp (see app/src/main/cpp/whisper_jni.cpp). */
object WhisperNative {
    init {
        System.loadLibrary("pleos_whisper")
    }

    /** Returns a context handle, or 0 if the model could not be loaded. */
    external fun init(modelPath: String): Long
    external fun free(handle: Long)
    external fun systemInfo(): String

    /** Raw UTF-8 transcript bytes (may contain a split multi-byte character at token boundaries). */
    private external fun transcribeUtf8(handle: Long, samples: FloatArray, language: String, translate: Boolean, threads: Int): ByteArray

    /** Transcribes 16 kHz mono float PCM; malformed UTF-8 is replaced instead of crashing. */
    fun transcribe(handle: Long, samples: FloatArray, language: String, translate: Boolean, threads: Int): String =
        Utf8.decodeLenient(transcribeUtf8(handle, samples, language, translate, threads))
}

/** Pure helpers (no native library), unit-testable on the JVM. */
object Utf8 {
    /** Decodes UTF-8, replacing malformed sequences with U+FFFD and then dropping them. */
    fun decodeLenient(bytes: ByteArray): String = String(bytes, Charsets.UTF_8).replace("\uFFFD", "")
}
