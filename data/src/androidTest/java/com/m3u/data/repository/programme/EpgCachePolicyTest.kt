package com.m3u.data.repository.programme

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EpgCachePolicyTest {
    private val lastRefresh = 1_000_000L

    @Test
    fun backgroundRefreshSkipsARecentlyImportedFeed() {
        assertTrue(
            shouldSkipEpgRefresh(
                ignoreCache = false,
                hasCachedProgrammes = true,
                lastSuccessfulRefresh = lastRefresh,
                now = lastRefresh + EPG_CACHE_TTL_MILLIS - 1L,
            )
        )
    }

    @Test
    fun backgroundRefreshRefreshesAnExpiredFeed() {
        assertFalse(
            shouldSkipEpgRefresh(
                ignoreCache = false,
                hasCachedProgrammes = true,
                lastSuccessfulRefresh = lastRefresh,
                now = lastRefresh + EPG_CACHE_TTL_MILLIS,
            )
        )
    }

    @Test
    fun manualRefreshAlwaysBypassesTheCache() {
        assertFalse(
            shouldSkipEpgRefresh(
                ignoreCache = true,
                hasCachedProgrammes = true,
                lastSuccessfulRefresh = lastRefresh,
                now = lastRefresh + 1L,
            )
        )
    }

    @Test
    fun feedWithoutCachedProgrammesIsNeverSkipped() {
        assertFalse(
            shouldSkipEpgRefresh(
                ignoreCache = false,
                hasCachedProgrammes = false,
                lastSuccessfulRefresh = lastRefresh,
                now = lastRefresh + 1L,
            )
        )
    }
}
