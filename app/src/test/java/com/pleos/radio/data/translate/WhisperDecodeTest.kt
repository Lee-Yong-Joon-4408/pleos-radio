package com.pleos.radio.data.translate

import org.junit.Assert.assertEquals
import org.junit.Test

class WhisperDecodeTest {
    @Test fun splitMultiByteCharacterIsDroppedNotFatal() {
        // " [드" followed by the first byte of another Hangul syllable, then " moons" — the exact shape that crashed.
        val bytes = byteArrayOf(0x20, 0x5b, 0xeb.toByte(), 0x93.toByte(), 0x9c.toByte(), 0xeb.toByte(), 0x20, 0x6d, 0x6f, 0x6f, 0x6e, 0x73)
        assertEquals(" [드 moons", Utf8.decodeLenient(bytes))
    }

    @Test fun validTextIsUnchanged() {
        val text = "안녕하세요 Guten Abend नमस्ते 🎵"
        assertEquals(text, Utf8.decodeLenient(text.toByteArray(Charsets.UTF_8)))
    }
}
