package com.pleos.radio.domain.caption

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class CaptionsTest {

    @Test fun stereo48kResamplesToMono16kChunks() {
        val chunks = mutableListOf<ShortArray>()
        val chunker = PcmChunker(chunkSeconds = 6.0) { chunks += it }
        chunker.configure(48_000, 2)
        // 7 s of stereo audio: left 1000, right 3000 -> mono 2000, fed in odd-sized buffers.
        val total = 48_000 * 7
        var fed = 0
        while (fed < total) {
            val frames = minOf(1_237, total - fed)
            val buf = ShortArray(frames * 2) { if (it % 2 == 0) 1000 else 3000 }
            chunker.feed(buf, buf.size)
            fed += frames
        }
        assertEquals(1, chunks.size)
        assertEquals(96_000, chunks[0].size)
        assertTrue(chunks[0].all { it.toInt() == 2000 })
    }

    @Test fun resamplingKeepsDurationFor44100() {
        var produced = 0
        val chunker = PcmChunker(chunkSeconds = 1.0) { produced += it.size }
        chunker.configure(44_100, 1)
        val buf = ShortArray(44_100 * 3) { (8000 * sin(2 * PI * 440 * it / 44_100.0)).toInt().toShort() }
        chunker.feed(buf, buf.size)
        assertEquals(3 * 16_000, produced)
    }

    @Test fun rmsSeparatesSilenceFromSpeech() {
        assertTrue(PcmChunker.rms(ShortArray(16_000)) < PcmChunker.SILENCE_RMS)
        val tone = ShortArray(16_000) { (5000 * sin(2 * PI * 300 * it / 16_000.0)).toInt().toShort() }
        assertTrue(abs(PcmChunker.rms(tone) - 5000 / kotlin.math.sqrt(2.0)) < 50)
    }

    @Test fun languageHelpers() {
        assertEquals("de", CaptionLanguages.translationCode("de-DE"))
        assertEquals("বাংলা", CaptionLanguages.nativeName("bn-IN"))
        assertEquals("한국어", CaptionLanguages.nativeName("ko-KR"))
        assertTrue(CaptionLanguages.sameLanguage("en-GB", CaptionLanguage.ENGLISH))
        assertFalse(CaptionLanguages.sameLanguage("en-GB", CaptionLanguage.KOREAN))
        assertFalse(CaptionLanguages.sameLanguage("hi-IN", CaptionLanguage.OFF))
        assertEquals(CaptionLanguage.KOREAN, CaptionLanguage.fromName(null))
        assertEquals(CaptionLanguage.OFF, CaptionLanguage.fromName("OFF"))
    }

    @Test fun transcriptCleanupDropsWhisperAnnotations() {
        assertEquals("Guten Abend, hier sind die Nachrichten.", Transcripts.clean(" [Musik] Guten Abend,  hier sind die Nachrichten. "))
        assertEquals("", Transcripts.clean(" [BLANK_AUDIO] "))
        assertEquals("", Transcripts.clean("(music) ♪♪ ..."))
        assertEquals("Hello world", Transcripts.clean("*laughs* Hello (applause) world"))
    }

    @Test fun modelAndEngineDefaults() {
        assertEquals(WhisperModel.BASE, WhisperModel.fromName(null))
        assertEquals(CaptionEngineType.ON_DEVICE, CaptionEngineType.fromName("nope"))
        assertEquals("https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin", WhisperModel.BASE.url)
        assertTrue(WhisperModel.entries.all { it.sha256.length == 64 })
    }

    @Test fun captionBufferKeepsRecentSegments() {
        val buffer = CaptionBuffer(maxSegments = 2)
        buffer.add("하나")
        buffer.add("  ")
        buffer.add("둘")
        assertEquals("둘 셋", buffer.add("셋"))
        buffer.clear()
        assertEquals("", buffer.text())
    }
}
