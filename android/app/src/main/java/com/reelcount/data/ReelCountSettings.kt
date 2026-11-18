package com.reelcount.data

import com.reelcount.util.TrackedApps

/**
 * Immutable snapshot of the user's settings. Reads are served from an
 * in-memory copy so that `onAccessibilityEvent` never touches disk.
 */
data class ReelCountSettings(
    val limit: Int = DEFAULT_LIMIT,
    val blockEnabled: Boolean = false,
    val overlayEnabled: Boolean = true,
    val allDayLockUntil: Long = 0L,
    val perAppEnabled: Map<String, Boolean> = defaultToggles()
) {
    fun isAppEnabled(pkg: String): Boolean = perAppEnabled[pkg] ?: true

    fun isLockedNow(nowEpochMillis: Long = System.currentTimeMillis()): Boolean = allDayLockUntil > nowEpochMillis

    fun isOverLimit(count: Int): Boolean = blockEnabled && count >= limit

    companion object {
        const val MIN_LIMIT: Int = 10
        const val MAX_LIMIT: Int = 500
        const val LIMIT_STEP: Int = 10
        const val DEFAULT_LIMIT: Int = 100

        fun defaultToggles(): Map<String, Boolean> = TrackedApps.ALL.associateWith { true }

        fun coerceLimit(value: Int): Int = value.coerceIn(MIN_LIMIT, MAX_LIMIT)
    }
}
