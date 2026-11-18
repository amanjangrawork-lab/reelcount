package com.reelcount.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerAppCountsTest {

    @Test
    fun `missing and blank input yields an empty map`() {
        assertTrue(PerAppCounts.parse(null).isEmpty())
        assertTrue(PerAppCounts.parse("").isEmpty())
        assertTrue(PerAppCounts.parse("   ").isEmpty())
    }

    @Test
    fun `a corrupt row yields an empty map instead of throwing`() {
        assertTrue(PerAppCounts.parse("{not json").isEmpty())
        assertTrue(PerAppCounts.parse("[1,2,3]").isEmpty())
    }

    @Test
    fun `counts survive a round trip`() {
        val counts = mapOf(TrackedApps.INSTAGRAM to 12, TrackedApps.YOUTUBE to 3)
        assertEquals(counts, PerAppCounts.parse(PerAppCounts.serialize(counts)))
    }

    @Test
    fun `zero and negative counters are not stored`() {
        val json = PerAppCounts.serialize(
            mapOf(TrackedApps.INSTAGRAM to 0, TrackedApps.YOUTUBE to -4, TrackedApps.FACEBOOK to 2)
        )
        assertEquals(
            mapOf(TrackedApps.FACEBOOK to 2),
            PerAppCounts.parse(json)
        )
    }

    @Test
    fun `unknown keys in stored JSON are ignored by the toggle parser`() {
        val toggles = PerAppCounts.parseToggles("""{"com.example.other":false}""")
        for (pkg in TrackedApps.ALL) {
            assertTrue(pkg, toggles[pkg] == true)
        }
        assertFalse(toggles.containsKey("com.example.other"))
    }

    @Test
    fun `toggles default to enabled and respect an explicit false`() {
        val stored = PerAppCounts.serializeToggles(mapOf(TrackedApps.SNAPCHAT to false))
        val toggles = PerAppCounts.parseToggles(stored)

        assertFalse(toggles[TrackedApps.SNAPCHAT] == true)
        assertTrue(toggles[TrackedApps.INSTAGRAM] == true)
    }

    @Test
    fun `serialised toggles always list every tracked app`() {
        val json = PerAppCounts.serializeToggles(emptyMap())
        val toggles = PerAppCounts.parseToggles(json)
        for (pkg in TrackedApps.ALL) {
            assertEquals(true, toggles[pkg])
        }
    }
}
