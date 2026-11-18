package com.reelcount.detect

import com.reelcount.data.ReelCountSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockPolicyTest {

    private val pkg = "com.instagram.android"

    private fun settings(
        limit: Int = 100,
        blockEnabled: Boolean = true,
        allDayLockUntil: Long = 0L,
        perAppEnabled: Map<String, Boolean> = emptyMap()
    ) = ReelCountSettings(
        limit = limit,
        blockEnabled = blockEnabled,
        allDayLockUntil = allDayLockUntil,
        perAppEnabled = perAppEnabled
    )

    @Test
    fun underTheLimitDoesNotBlock() {
        assertFalse(
            BlockPolicy.shouldBlock(settings(limit = 100), pkg, todayTotal = 99, NOW, 0L)
        )
    }

    @Test
    fun atOrOverTheLimitBlocks() {
        assertTrue(BlockPolicy.shouldBlock(settings(limit = 100), pkg, todayTotal = 100, NOW, 0L))
        assertTrue(BlockPolicy.shouldBlock(settings(limit = 100), pkg, todayTotal = 140, NOW, 0L))
    }

    @Test
    fun blockingDisabledNeverBlocks() {
        assertFalse(
            BlockPolicy.shouldBlock(
                settings(limit = 10, blockEnabled = false),
                pkg,
                todayTotal = 500,
                NOW,
                0L
            )
        )
    }

    @Test
    fun disabledAppIsNeverBlocked() {
        assertFalse(
            BlockPolicy.shouldBlock(
                settings(limit = 10, perAppEnabled = mapOf(pkg to false)),
                pkg,
                todayTotal = 500,
                NOW,
                0L
            )
        )
    }

    @Test
    fun recentDismissalSkipsTheLimit() {
        assertFalse(
            BlockPolicy.shouldBlock(
                settings(limit = 10),
                pkg,
                todayTotal = 20,
                NOW,
                dismissedUntil = NOW + 1_000L
            )
        )
        assertTrue(
            BlockPolicy.shouldBlock(
                settings(limit = 10),
                pkg,
                todayTotal = 20,
                NOW,
                dismissedUntil = NOW - 1L
            )
        )
    }

    @Test
    fun allDayLockBlocksEvenAfterADismissal() {
        assertTrue(
            BlockPolicy.shouldBlock(
                settings(allDayLockUntil = NOW + 60_000L),
                pkg,
                todayTotal = 0,
                NOW,
                dismissedUntil = NOW + 600_000L
            )
        )
    }

    @Test
    fun expiredLockDoesNotBlock() {
        assertFalse(
            BlockPolicy.shouldBlock(
                settings(allDayLockUntil = NOW - 1L),
                pkg,
                todayTotal = 0,
                NOW,
                0L
            )
        )
    }

    @Test
    fun aLockCannotBeDismissed() {
        assertFalse(BlockPolicy.isDismissable(settings(allDayLockUntil = NOW + 60_000L), NOW))
        assertTrue(BlockPolicy.isDismissable(settings(), NOW))
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
