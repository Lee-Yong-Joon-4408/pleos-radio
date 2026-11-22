// JNI bridge to whisper.cpp for on-device live captions.
#include <jni.h>
#include <android/log.h>
#include <string>
#include <thread>
#include <algorithm>
#include "whisper.h"

#define TAG "WhisperJni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_pleos_radio_data_translate_WhisperNative_init(JNIEnv *env, jobject, jstring modelPath) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false;
    whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);
    if (ctx == nullptr) LOGW("failed to load model %s", path);
    else LOGI("loaded model %s", path);
    env->ReleaseStringUTFChars(modelPath, path);
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT void JNICALL
Java_com_pleos_radio_data_translate_WhisperNative_free(JNIEnv *, jobject, jlong handle) {
    if (handle != 0) whisper_free(reinterpret_cast<whisper_context *>(handle));
}

static jbyteArray toBytes(JNIEnv *env, const std::string &s) {
    jbyteArray out = env->NewByteArray((jsize) s.size());
    if (out != nullptr && !s.empty()) env->SetByteArrayRegion(out, 0, (jsize) s.size(), reinterpret_cast<const jbyte *>(s.data()));
    return out;
}

// samples: 16 kHz mono float PCM in [-1, 1]. language: ISO-639-1 ("de", "hi", ...).
// translate=true makes Whisper output English regardless of the spoken language.
// Returns raw UTF-8 bytes: Whisper tokens can split a multi-byte character, and NewStringUTF aborts the
// process on invalid (Modified) UTF-8, so decoding happens leniently on the Kotlin side.
JNIEXPORT jbyteArray JNICALL
Java_com_pleos_radio_data_translate_WhisperNative_transcribeUtf8(JNIEnv *env, jobject, jlong handle, jfloatArray samples,
                                                              jstring language, jboolean translate, jint threads) {
    auto *ctx = reinterpret_cast<whisper_context *>(handle);
    if (ctx == nullptr) return toBytes(env, "");

    const char *lang = env->GetStringUTFChars(language, nullptr);
    std::string langCopy(lang);
    env->ReleaseStringUTFChars(language, lang);

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    params.translate = translate;
    params.language = langCopy.c_str();
    params.detect_language = false;
    params.n_threads = threads > 0 ? threads : (int) std::max(1u, std::thread::hardware_concurrency());
    params.no_context = true;
    params.no_timestamps = true;
    params.single_segment = false;
    params.suppress_blank = true;

    jsize n = env->GetArrayLength(samples);
    // The encoder normally always runs on a 30 s window (1500 frames, 50 per second). For short live chunks,
    // shrink it to the real audio length plus a margin: several times faster with little accuracy loss.
    params.audio_ctx = std::min(1500, (int) (n / 320) + 64);

    jfloat *data = env->GetFloatArrayElements(samples, nullptr);
    int rc = whisper_full(ctx, params, data, n);
    env->ReleaseFloatArrayElements(samples, data, JNI_ABORT);
    if (rc != 0) {
        LOGW("whisper_full failed: %d", rc);
        return toBytes(env, "");
    }

    std::string text;
    const int segments = whisper_full_n_segments(ctx);
    for (int i = 0; i < segments; ++i) {
        const char *seg = whisper_full_get_segment_text(ctx, i);
        if (seg != nullptr) text += seg;
    }
    return toBytes(env, text);
}

JNIEXPORT jstring JNICALL
Java_com_pleos_radio_data_translate_WhisperNative_systemInfo(JNIEnv *env, jobject) {
    return env->NewStringUTF(whisper_print_system_info());
}

} // extern "C"
