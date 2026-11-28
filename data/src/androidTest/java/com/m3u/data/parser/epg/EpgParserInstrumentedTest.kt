package com.m3u.data.parser.epg

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayInputStream
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EpgParserInstrumentedTest {
    @Test
    fun parsesXmltvProgrammeAndNestedText() = runBlocking {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
                <channel id="news.one"><display-name>News</display-name></channel>
                <programme start="20261128090000 +0000"
                    stop="20261128100000 +0000" channel="news.one">
                    <title>Morning <b>News</b></title>
                    <desc>Top stories</desc>
                    <category>News</category>
                    <icon src="https://example.com/news.png" />
                </programme>
            </tv>
        """.trimIndent()

        val programmes = EpgParserImpl()
            .readProgrammes(ByteArrayInputStream(xml.toByteArray()))
            .toList()

        assertEquals(1, programmes.size)
        assertEquals("news.one", programmes.single().channel)
        assertEquals("Morning News", programmes.single().title)
        assertEquals("Top stories", programmes.single().desc)
        assertEquals(listOf("News"), programmes.single().categories)
        assertEquals("https://example.com/news.png", programmes.single().icon)
    }

    @Test
    fun rejectsNonXmltvRootInsteadOfTreatingItAsEmptyFeed() = runBlocking {
        val xml = "<html><body>temporary upstream error</body></html>"
        try {
            EpgParserImpl()
                .readProgrammes(ByteArrayInputStream(xml.toByteArray()))
                .toList()
            fail("Expected a malformed XMLTV document to fail")
        } catch (_: Exception) {
            // Expected: a failed source must not replace previously imported data.
        }
    }
}
