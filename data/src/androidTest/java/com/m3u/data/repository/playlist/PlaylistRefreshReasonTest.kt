package com.m3u.data.repository.playlist

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistRefreshReasonTest {
    @Test
    fun manualRefreshBypassesEpgCache() {
        assertTrue(PlaylistRefreshReason.USER.ignoreEpgCache)
    }

    @Test
    fun backgroundRefreshUsesEpgCacheFreshnessWindow() {
        assertFalse(PlaylistRefreshReason.BACKGROUND.ignoreEpgCache)
    }
}
