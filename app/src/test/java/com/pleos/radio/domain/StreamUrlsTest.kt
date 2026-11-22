package com.pleos.radio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamUrlsTest {

    @Test fun parsesKbsServiceUrl() {
        val json = """{"ret":0,"msg":"OK","channel_item":[{"bitrate":"128Kbps","service_url":"https:\/\/1fm.gscdn.kbs.co.kr\/1fm_192_2.m3u8?Policy=abc&Key-Pair-Id=K"}]}"""
        assertEquals("https://1fm.gscdn.kbs.co.kr/1fm_192_2.m3u8?Policy=abc&Key-Pair-Id=K", StreamUrls.parseKbsServiceUrl(json))
    }

    @Test fun kbsWithoutUrlIsNull() {
        assertNull(StreamUrls.parseKbsServiceUrl("""{"ret":1,"msg":"error"}"""))
    }

    @Test fun plainTextUrlIsTrimmedAndValidated() {
        assertEquals("https://minimw.imbc.com/x/playlist.m3u8?a=1", StreamUrls.validHttpsUrl("  https://minimw.imbc.com/x/playlist.m3u8?a=1\r\n"))
        assertNull(StreamUrls.validHttpsUrl("<html>error</html>"))
        assertNull(StreamUrls.validHttpsUrl("http://insecure.example/x.m3u8"))
        assertNull(StreamUrls.validHttpsUrl("https://a b"))
    }
}
