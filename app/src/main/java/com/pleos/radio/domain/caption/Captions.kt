package com.pleos.radio.domain.caption

/** Caption (translation target) languages offered in the menu; OFF disables live translation. */
enum class CaptionLanguage(val code: String?, val label: String) {
    KOREAN("ko", "한국어"),
    ENGLISH("en", "English"),
    JAPANESE("ja", "日本語"),
    CHINESE("zh-CN", "中文"),
    OFF(null, "Off");

    companion object {
        fun fromName(name: String?): CaptionLanguage = entries.firstOrNull { it.name == name } ?: KOREAN
    }
}

/** Where speech recognition + translation run. */
enum class CaptionEngineType(val label: String) {
    ON_DEVICE("On-device (free)"),
    CLOUD("Google Cloud");

    companion object {
        fun fromName(name: String?): CaptionEngineType = entries.firstOrNull { it.name == name } ?: ON_DEVICE
    }
}

/** whisper.cpp models (multilingual, 5-bit quantised) from huggingface.co/ggerganov/whisper.cpp. */
enum class WhisperModel(val fileName: String, val sizeBytes: Long, val sha256: String, val label: String) {
    TINY("ggml-tiny-q5_1.bin", 32_152_673, "818710568da3ca15689e31a743197b520007872ff9576237bda97bd1b469c3d7", "Tiny · 32 MB"),
    BASE("ggml-base-q5_1.bin", 59_707_625, "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898", "Base · 60 MB"),
    SMALL("ggml-small-q5_1.bin", 190_085_487, "ae85e4a935d7a567bd102fe55afc16bb595bdb618e11b2fc7591bc08120411bb", "Small · 190 MB");

    val url: String get() = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/$fileName"
    val sizeMb: Int get() = (sizeBytes / 1_000_000).toInt()

    companion object {
        fun fromName(name: String?): WhisperModel = entries.firstOrNull { it.name == name } ?: BASE
    }
}

enum class CaptionStatus { OFF, NEED_KEY, NEED_MODEL, DOWNLOADING, LISTENING, TRANSLATING, ERROR }

/** What the caption panel shows. [available] = the current station broadcasts in a foreign language. */
data class CaptionState(
    val available: Boolean = false,
    val status: CaptionStatus = CaptionStatus.OFF,
    val text: String = "",
    val sourceLabel: String = "",
    val target: CaptionLanguage = CaptionLanguage.KOREAN,
    val error: String? = null,
    val engine: CaptionEngineType = CaptionEngineType.ON_DEVICE,
    /** Model download progress 0..1 while [status] is DOWNLOADING. */
    val progress: Float = 0f,
    val modelSizeMb: Int = 0,
)

object CaptionLanguages {
    /** "de-DE" → "de" (Cloud Translation source code). */
    fun translationCode(speechLanguage: String): String = speechLanguage.substringBefore('-').lowercase()

    /** Native name of the broadcast language, shown as "Deutsch → 한국어". */
    fun nativeName(speechLanguage: String): String = when (translationCode(speechLanguage)) {
        "de" -> "Deutsch"
        "en" -> "English"
        "hi" -> "हिन्दी"
        "bn" -> "বাংলা"
        "mr" -> "मराठी"
        "gu" -> "ગુજરાતી"
        else -> speechLanguage
    }

    /** Languages for which Speech-to-Text v1 offers automatic punctuation. */
    fun supportsPunctuation(speechLanguage: String): Boolean = translationCode(speechLanguage) in setOf("en", "de", "hi")

    /** True when no translation is needed (e.g. an English station with English captions). */
    fun sameLanguage(speechLanguage: String, target: CaptionLanguage): Boolean =
        target.code?.substringBefore('-')?.lowercase() == translationCode(speechLanguage)
}

object Transcripts {
    private val ANNOTATION = Regex("""\[[^\]]*]|\([^)]*\)|♪+|\*[^*]*\*""")
    private val NOISE_ONLY = Regex("""^[\s.,!?…\-–—]*$""")

    /**
     * Strips Whisper's non-speech annotations ("[Music]", "(applause)", "♪", "*laughs*") and collapses whitespace;
     * returns "" when nothing but annotations / punctuation is left.
     */
    fun clean(raw: String): String {
        val text = ANNOTATION.replace(raw, " ").replace(Regex("""\s+"""), " ").trim()
        return if (NOISE_ONLY.matches(text)) "" else text
    }
}

/** Rolling caption text: keeps the most recent segments so the panel can show the latest lines. */
class CaptionBuffer(private val maxSegments: Int = 6) {
    private val segments = ArrayDeque<String>()

    fun add(segment: String): String {
        val clean = segment.trim()
        if (clean.isNotEmpty()) {
            segments.addLast(clean)
            while (segments.size > maxSegments) segments.removeFirst()
        }
        return text()
    }

    fun clear() = segments.clear()

    fun text(): String = segments.joinToString(" ")
}
