package com.pleos.radio.data.translate

/** Thin JNI surface over whisper.cpp (see app/src/main/cpp/whisper_jni.cpp). */
object WhisperNative {
    init {
        System.loadLibrary("pleos_whisper")
    }

    /** Returns a context handle, or 0 if the model could not be loaded. */
    external fun init(modelPath: String): Long
    external fun free(handle: Long)
    external fun transcribe(handle: Long, samples: FloatArray, language: String, translate: Boolean, threads: Int): String
    external fun systemInfo(): String
}
