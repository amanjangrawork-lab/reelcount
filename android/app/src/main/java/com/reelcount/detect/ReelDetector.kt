package com.reelcount.detect

/**
 * The six anti-false-positive rules, as pure functions.
 *
 * The accessibility service supplies values (view ids, node bounds, timestamps)
 * and this object decides. Nothing here touches the Android framework, so every
 * rule is covered by plain JVM unit tests in `src/test`.
 */
object ReelDetector {

    /** Rule 4: a reel must be on screen this long before it counts. */
    const val MIN_VISIBLE_MS: Long = 2_000L

    /** Rule 3: the same view id cannot count twice within this window. */
    const val DEBOUNCE_MS: Long = 1_000L

    /** Rule 5: Stories report `reel_viewer_*` ids and are never counted. */
    const val STORIES_PREFIX: String = "reel_viewer_"

    /** Any app-owned container starting with this prefix counts as a player. */
    const val REEL_PLAYER_PREFIX: String = "reel_player_"

    /**
     * `com.instagram.android:id/reel_player_page_container` becomes
     * `reel_player_page_container`. Returns null when the node has no id.
     */
    fun shortViewId(rawViewId: String?): String? {
        if (rawViewId.isNullOrEmpty()) {
            return null
        }
        val shortId = rawViewId.substringAfter(":id/", "")
        return shortId.ifEmpty { null }
    }

    /** Rule 5 plus the container match: is this id a reel player, never a Story? */
    fun isReelContainerId(shortId: String, knownIds: Collection<String>): Boolean {
        if (shortId.startsWith(STORIES_PREFIX)) {
            return false
        }
        return knownIds.contains(shortId) || shortId.startsWith(REEL_PLAYER_PREFIX)
    }

    /** Rules 1 and 2: zero-sized nodes are not real, and the node must be visible. */
    fun isRealNode(width: Int, height: Int, visibleToUser: Boolean): Boolean {
        if (width <= 0 || height <= 0) {
            return false
        }
        return visibleToUser
    }

    /** Rule 4: [visibleSince] is the uptime at which the container appeared. */
    fun isDwellSatisfied(visibleSince: Long, now: Long): Boolean =
        visibleSince > 0L && now - visibleSince >= MIN_VISIBLE_MS

    /**
     * Rule 3: [lastCountAt] is 0 for a view id that has never counted. That case
     * is allowed unconditionally: `now` is device uptime, so a reel watched in
     * the first second after boot would otherwise be debounced against zero.
     */
    fun isDebounceSatisfied(lastCountAt: Long, now: Long): Boolean =
        lastCountAt == 0L || now - lastCountAt >= DEBOUNCE_MS
}
