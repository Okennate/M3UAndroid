package com.m3u.data.parser.epg

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.InputStream
import javax.inject.Inject

internal class EpgParserImpl @Inject constructor() : EpgParser {
    override fun readProgrammes(input: InputStream): Flow<EpgProgramme> = channelFlow {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(input, null)
        }

        // Do not silently accept an empty response, HTML error page, or a
        // document whose root is not XMLTV. Import staging preserves old data
        // whenever this parser fails.
        while (parser.eventType != XmlPullParser.START_TAG &&
            parser.next() != XmlPullParser.END_DOCUMENT
        ) {
            // Advance to the document root.
        }
        if (parser.eventType != XmlPullParser.START_TAG || parser.name != "tv") {
            throw XmlPullParserException("Expected XMLTV <tv> root element")
        }

        var finished = false
        while (!finished) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "programme") {
                        // Structural XML errors fail the source rather than
                        // committing a partially parsed replacement.
                        send(parser.readProgramme())
                    } else {
                        parser.skip()
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "tv") finished = true
                }

                XmlPullParser.END_DOCUMENT -> {
                    throw XmlPullParserException("Unexpected end of XMLTV document")
                }
            }
        }
        // Consume the document tail too, so trailing garbage or a second root
        // cannot make a malformed feed look like a successful partial import.
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                throw XmlPullParserException("Unexpected content after XMLTV </tv>")
            }
        }
    }.flowOn(Dispatchers.Default)

    private fun XmlPullParser.readProgramme(): EpgProgramme {
        require(XmlPullParser.START_TAG, null, "programme")
        val start = getAttributeValue(null, "start")
        val stop = getAttributeValue(null, "stop")
        val channel = getAttributeValue(null, "channel").orEmpty()
        var title: String? = null
        var desc: String? = null
        val categories = mutableListOf<String>()
        var icon: String? = null

        var finished = false
        while (!finished) {
            when (next()) {
                XmlPullParser.START_TAG -> when (name) {
                    "title" -> title = readTextElement("title")
                    "desc" -> desc = readTextElement("desc")
                    "category" -> readTextElement("category")?.let(categories::add)
                    "icon" -> {
                        icon = getAttributeValue(null, "src")
                        skip()
                    }
                    else -> skip()
                }

                XmlPullParser.END_TAG -> {
                    if (name == "programme") finished = true
                }

                XmlPullParser.END_DOCUMENT -> {
                    throw XmlPullParserException("Unexpected end inside XMLTV programme")
                }
            }
        }

        return EpgProgramme(
            start = start,
            stop = stop,
            channel = channel,
            title = title,
            desc = desc,
            icon = icon,
            categories = categories
        )
    }

    private fun XmlPullParser.readTextElement(elementName: String): String? {
        require(XmlPullParser.START_TAG, null, elementName)
        val result = StringBuilder()
        var depth = 1
        while (depth > 0) {
            when (next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.TEXT,
                XmlPullParser.CDSECT,
                XmlPullParser.ENTITY_REF -> result.append(text)
                XmlPullParser.END_DOCUMENT -> {
                    throw XmlPullParserException("Unexpected end inside XMLTV <$elementName>")
                }
            }
        }
        return result.toString().trim().takeIf(String::isNotEmpty)
    }

    private fun XmlPullParser.skip() {
        check(eventType == XmlPullParser.START_TAG)
        var depth = 1
        while (depth > 0) {
            when (next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> {
                    throw XmlPullParserException("Unexpected end while skipping XMLTV element")
                }
            }
        }
    }
}
