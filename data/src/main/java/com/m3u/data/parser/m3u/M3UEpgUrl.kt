package com.m3u.data.parser.m3u

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream

private const val MAX_M3U_HEADER_BYTES = 64 * 1024

private val EPG_URL_ATTRIBUTE = Regex(
    """(?i)(?:^|\\s)(?:x-tvg-url|url-tvg|tvg-url|x-epg-url|epg-url)\\s*=\\s*(?:"([^"]*)"|'([^']*)'|([^\\s]+))"""
)

/**
 * Reads only the first M3U header line and resets the stream so the normal
 * playlist parser receives the complete, unchanged input.
 */
internal fun BufferedInputStream.readM3uEpgUrlsFromHeader(): List<String> {
    mark(MAX_M3U_HEADER_BYTES + 1)
    val header = ByteArrayOutputStream()
    while (header.size() < MAX_M3U_HEADER_BYTES) {
        val next = read()
        if (next == -1 || next == '\\n'.code) break
        if (next != '\\r'.code) header.write(next)
    }
    reset()
    return parseM3uEpgUrlsFromHeader(
        String(header.toByteArray(), Charsets.UTF_8)
    )
}

/**
 * Supports the common XMLTV URL attributes used by M3U providers.
 * A comma-separated value may contain more than one guide URL.
 */
internal fun parseM3uEpgUrlsFromHeader(header: String): List<String> {
    val normalizedHeader = header.removePrefix("\\uFEFF")
    return EPG_URL_ATTRIBUTE.findAll(normalizedHeader)
        .flatMap { match ->
            val value = sequenceOf(
                match.groupValues[1],
                match.groupValues[2],
                match.groupValues[3],
            ).firstOrNull(String::isNotBlank).orEmpty()
            value.replace("&amp;", "&")
                .split(',')
                .asSequence()
                .map { it.trim().trimEnd(';') }
        }
        .filter { candidate ->
            candidate.length <= 8_192 &&
                candidate.toHttpUrlOrNull()?.let { url ->
                    url.scheme == "http" || url.scheme == "https"
                } == true
        }
        .distinct()
        .toList()
}
