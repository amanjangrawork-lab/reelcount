package com.reelcount.detect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One test per rule, plus the edge cases the six rules were written for. These
 * run on the JVM, so the detection heuristics are no longer unverified.
 */
class ReelDetectorTest {

    private val instagramIds = listOf("clips_viewer_view_pager", "reel_player_page_container")

    // ------------------------------------------------------------ id parsing

    @Test
    fun `shortViewId strips the package prefix`() {
        assertEquals(
            "reel_player_page_container",
            ReelDetector.shortViewId("com.instagram.android:id/reel_player_page_container")
        )
    }

    @Test
    fun `shortViewId returns null for missing ids`() {
        assertNull(ReelDetector.shortViewId(null))
        assertNull(ReelDetector.shortViewId(""))
        assertNull(ReelDetector.shortViewId("no-id-here"))
        assertNull(ReelDetector.shortViewId("com.instagram.android:id/"))
    }

    // ------------------------------------------------ rule 5 and container match

    @Test
    fun `known container ids are accepted`() {
        for (id in instagramIds) {
            assertTrue(id, ReelDetector.isReelContainerId(id, instagramIds))
        }
    }

    @Test
    fun `unknown reel_player ids are accepted`() {
        assertTrue(ReelDetector.isReelContainerId("reel_player_something_new", instagramIds))
    }

    @Test
    fun `stories are rejected even if the id matches a known container`() {
        val withStory = instagramIds + "reel_viewer_container"
        assertFalse(ReelDetector.isReelContainerId("reel_viewer_container", withStory))
    }

    @Test
    fun `unrelated view ids are rejected`() {
        assertFalse(ReelDetector.isReelContainerId("clips_viewer_root", instagramIds))
        assertFalse(ReelDetector.isReelContainerId("image_view", instagramIds))
    }

    // ------------------------------------------------ rules 1 and 2: real node

    @Test
    fun `zero sized nodes are rejected`() {
        assertFalse(ReelDetector.isRealNode(width = 0, height = 400, visibleToUser = true))
        assertFalse(ReelDetector.isRealNode(width = 400, height = 0, visibleToUser = true))
        assertFalse(ReelDetector.isRealNode(width = -1, height = -1, visibleToUser = true))
    }

    @Test
    fun `invisible nodes are rejected`() {
        assertFalse(ReelDetector.isRealNode(width = 400, height = 400, visibleToUser = false))
    }

    @Test
    fun `a visible non-empty node passes`() {
        assertTrue(ReelDetector.isRealNode(width = 1080, height = 1920, visibleToUser = true))
    }

    // ------------------------------------------------ rule 4: dwell time

    @Test
    fun `dwell time needs a start`() {
        assertFalse(ReelDetector.isDwellSatisfied(visibleSince = 0L, now = 10_000L))
    }

    @Test
    fun `dwell time rejects just under two seconds`() {
        assertFalse(ReelDetector.isDwellSatisfied(visibleSince = 1_000L, now = 2_999L))
    }

    @Test
    fun `dwell time accepts two seconds`() {
        assertTrue(ReelDetector.isDwellSatisfied(visibleSince = 1_000L, now = 3_000L))
    }

    // ------------------------------------------------ rule 3: debounce

    @Test
    fun `a view that never counted may count immediately`() {
        assertTrue(ReelDetector.isDebounceSatisfied(lastCountAt = 0L, now = 1L))
    }

    @Test
    fun `debounce rejects a second count within one second`() {
        assertFalse(ReelDetector.isDebounceSatisfied(lastCountAt = 5_000L, now = 5_999L))
    }

    @Test
    fun `debounce allows a count after one second`() {
        assertTrue(ReelDetector.isDebounceSatisfied(lastCountAt = 5_000L, now = 6_000L))
    }
}
