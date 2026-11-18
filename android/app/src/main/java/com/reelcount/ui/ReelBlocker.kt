package com.reelcount.ui

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import com.reelcount.R
import com.reelcount.ReelCountEvents
import com.reelcount.data.CountRepository
import com.reelcount.data.SettingsRepository
import com.reelcount.detect.BlockPolicy

/**
 * Enforces the daily limit.
 *
 * Called from the accessibility service whenever a reel is visible or counted.
 * When the limit (or an all-day lock) is active the service is asked to press
 * Back and a full screen overlay is shown. The five minute dismissal applies to
 * a daily limit only; an all-day lock is not dismissible.
 */
class ReelBlocker private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val overlay = OverlayManager.get(appContext)
    private val settingsRepo = SettingsRepository.get(appContext)
    private val countRepo = CountRepository.get(appContext)

    @Volatile
    private var lastTotal = 0

    /** When [lastTotal] was last confirmed against the database. */
    @Volatile
    private var totalCheckedAt = 0L

    @Volatile
    private var blocked = false

    @Volatile
    private var dismissedUntil = 0L

    @Volatile
    private var lastBackAt = 0L

    @Volatile
    private var lastEvaluateAt = 0L

    /** Records the current total and emits `onLimitReached` when it is hit. */
    fun onReelCounted(pkg: String, total: Int) {
        lastTotal = total
        val settings = settingsRepo.current()
        if (settings.blockEnabled && settings.isAppEnabled(pkg) && total >= settings.limit) {
            ReelCountEvents.limitReached(pkg, total)
        }
    }

    /**
     * Decides whether the current reel view has to be blocked. Safe to call on
     * every accessibility event; internally throttled.
     *
     * Suspending because the day's total is read from the database when the
     * in-memory copy is stale: after a process restart (or at midnight) the
     * counter starts at zero and a limit that was already reached would
     * otherwise be ignored until the next reel was counted.
     */
    suspend fun evaluateBlock(service: AccessibilityService, pkg: String) {
        val now = System.currentTimeMillis()
        if (now - lastEvaluateAt < EVALUATE_INTERVAL_MS) {
            return
        }
        lastEvaluateAt = now

        val settings = settingsRepo.current()
        val total = currentTotal(now)

        if (!BlockPolicy.shouldBlock(settings, pkg, total, now, dismissedUntil)) {
            setBlocked(false)
            return
        }

        setBlocked(true)

        if (now - lastBackAt >= BACK_INTERVAL_MS) {
            lastBackAt = now
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        }

        // The reminder is the enforcement, so it is shown whether or not the
        // floating pill is switched on; the pill is only cosmetic.
        val canDismiss = BlockPolicy.isDismissable(settings, now)
        overlay.showBlocker(
            appContext.getString(R.string.overlay_limit_reached),
            appContext.getString(R.string.overlay_dismiss),
            if (canDismiss) ::dismissForFiveMinutes else null
        )
    }

    /** Today's total, refreshed from the database at most every [TOTAL_TTL_MS]. */
    private suspend fun currentTotal(now: Long): Int {
        if (now - totalCheckedAt < TOTAL_TTL_MS) {
            return lastTotal
        }
        totalCheckedAt = now
        return try {
            countRepo.todayTotal().also { lastTotal = it }
        } catch (error: Exception) {
            Log.w(TAG, "Could not read today's total", error)
            lastTotal
        }
    }

    fun dismissForFiveMinutes() {
        dismissedUntil = System.currentTimeMillis() + DISMISS_MS
        setBlocked(false)
    }

    private fun setBlocked(value: Boolean) {
        if (blocked == value) {
            if (!value) {
                overlay.hideBlocker()
            }
            return
        }

        blocked = value
        if (!value) {
            overlay.hideBlocker()
        }
        ReelCountEvents.blockStateChanged(value)
    }

    companion object {
        private const val TAG = "ReelBlocker"
        private const val TOTAL_TTL_MS = 5_000L
        private const val EVALUATE_INTERVAL_MS = 1_500L
        private const val BACK_INTERVAL_MS = 1_500L
        private const val DISMISS_MS = 5 * 60 * 1000L

        @Volatile
        private var instance: ReelBlocker? = null

        fun get(context: Context): ReelBlocker {
            return instance ?: synchronized(this) {
                instance ?: ReelBlocker(context).also { instance = it }
            }
        }
    }
}
