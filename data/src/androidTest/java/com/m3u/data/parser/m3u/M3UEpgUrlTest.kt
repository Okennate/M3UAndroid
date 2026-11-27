package com.m3u.data.parser.m3u

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class M3UEpgUrlTest {
    @Test
    fun parsesSupportedHeaderAttributesAndMultipleUrls() {
        val urls = parseM3uEpgUrlsFromHeader(
            """#EXTM3U x-tvg-url="https://guide.example/one.xml,https://guide.example/two.xml" url-tvg='http://other.example/epg.xml'"""
        )

        assertEquals(
            listOf(
                "https://guide.example/one.xml",
                "https://guide.example/two.xml",
                "http://other.example/epg.xml",
            ),
            urls,
        )
    }

    @Test
    fun ignoresInvalidAndNonHttpUrlsAndDeduplicates() {
        val urls = parseM3uEpgUrlsFromHeader(
            """#EXTM3U tvg-url="javascript:alert(1),https://guide.example/epg.xml,https://guide.example/epg.xml" x-epg-url="file:///sdcard/epg.xml" """
        )

        assertEquals(listOf("https://guide.example/epg.xml"), urls)
    }

    @Test
    fun readingHeaderDoesNotConsumePlaylistInput() {
        val content = """#EXTM3U x-tvg-url="https://guide.example/epg.xml"
#EXTINF:-1 tvg-id="news",News
https://stream.example/live
"""
        val input = ByteArrayInputStream(content.toByteArray(Charsets.UTF_8)).buffered()

        assertEquals(
            listOf("https://guide.example/epg.xml"),
            input.readM3uEpgUrlsFromHeader(),
        )
        assertTrue(input.readBytes().contentEquals(content.toByteArray(Charsets.UTF_8)))
    }
}
