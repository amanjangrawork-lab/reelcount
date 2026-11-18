package com.reelcount.detect

import com.reelcount.data.ReelCountSettings

/**
 * The single decision point for "should this reel be interrupted?".
 *
 * Kept pure — no Android imports, no clock, no database — so the rules can be
 * unit tested and so the accessibility service and [com.reelcount.ui.ReelBlocker]
 * cannot drift apart.
 */
object BlockPolicy {

    /**
     * True when the reel currently on screen has to be blocked.
     *
     * An all-day lock always wins and cannot be dismissed; a daily limit is
     * skippable for the dismissal window the user accepted.
     */
    fun shouldBlock(
        settings: ReelCountSettings,
        pkg: String,
        todayTotal: Int,
        nowEpochMillis: Long,
        dismissedUntil: Long
    ): Boolean {
        if (!settings.isAppEnabled(pkg)) {
            return false
        }

        val locked = settings.isLockedNow(nowEpochMillis)
        val overLimit = settings.isOverLimit(todayTotal)

        if (!locked && !overLimit) {
            return false
        }

        // A lock is a commitment for the rest of the day: no five-minute pass.
        if (locked) {
            return true
        }

        return nowEpochMillis >= dismissedUntil
    }

    /** True when the "dismiss for 5 min" pass may be offered. */
    fun isDismissable(settings: ReelCountSettings, nowEpochMillis: Long): Boolean =
        !settings.isLockedNow(nowEpochMillis)
}
