package com.reelcount.data

import com.reelcount.util.TrackedApps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReelCountSettingsTest {

    @Test
    fun `limit is clamped into the slider range`() {
        assertEquals(ReelCountSettings.MIN_LIMIT, ReelCountSettings.coerceLimit(0))
        assertEquals(ReelCountSettings.MIN_LIMIT, ReelCountSettings.coerceLimit(-50))
        assertEquals(ReelCountSettings.MAX_LIMIT, ReelCountSettings.coerceLimit(9_999))
        assertEquals(120, ReelCountSettings.coerceLimit(120))
    }

    @Test
    fun `over limit requires blocking to be enabled`() {
        val blockingOff = ReelCountSettings(limit = 100, blockEnabled = false)
        assertFalse(blockingOff.isOverLimit(500))

        val blockingOn = ReelCountSettings(limit = 100, blockEnabled = true)
        assertFalse(blockingOn.isOverLimit(99))
        assertTrue(blockingOn.isOverLimit(100))
        assertTrue(blockingOn.isOverLimit(101))
    }

    @Test
    fun `every tracked app is enabled by default`() {
        val settings = ReelCountSettings()
        for (pkg in TrackedApps.ALL) {
            assertTrue(pkg, settings.isAppEnabled(pkg))
        }
    }

    @Test
    fun `an explicit per app toggle wins over the default`() {
        val settings = ReelCountSettings(perAppEnabled = mapOf(TrackedApps.INSTAGRAM to false))
        assertFalse(settings.isAppEnabled(TrackedApps.INSTAGRAM))
        assertTrue(settings.isAppEnabled(TrackedApps.YOUTUBE))
    }

    @Test
    fun `the all-day lock expires at its timestamp`() {
        val settings = ReelCountSettings(allDayLockUntil = 10_000L)
        assertTrue(settings.isLockedNow(nowEpochMillis = 9_999L))
        assertFalse(settings.isLockedNow(nowEpochMillis = 10_000L))
    }

    @Test
    fun `no lock means not locked`() {
        assertFalse(ReelCountSettings(allDayLockUntil = 0L).isLockedNow(nowEpochMillis = 0L))
    }
}
