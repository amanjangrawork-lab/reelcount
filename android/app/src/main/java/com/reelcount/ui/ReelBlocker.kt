package com.reelcount.ui

import android.accessibilityservice.AccessibilityService
import android.content.Context
import com.reelcount.R
import com.reelcount.ReelCountEvents
import com.reelcount.data.SettingsRepository

/**
 * Enforces the daily limit.
 *
 * Called from the accessibility service whenever a reel is visible or counted.
 * When the limit (or an all-day lock) is active the service is asked to press
 * Back and a full screen overlay is shown with a five minute dismissal.
 */
class ReelBlocker private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val overlay = OverlayManager.get(appContext)
    private val settingsRepo = SettingsRepository.get(appContext)

    @Volatile
    private var lastTotal = 0

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
            ReelCountEvents.limitReached(appContext, pkg, total)
        }
    }

    /**
     * Decides whether the current reel view has to be blocked. Safe to call on
     * every accessibility event; internally throttled.
     */
    fun evaluateBlock(service: AccessibilityService, pkg: String) {
        val now = System.currentTimeMillis()
        if (now - lastEvaluateAt < EVALUATE_INTERVAL_MS) {
            return
        }
        lastEvaluateAt = now

        val settings = settingsRepo.current()

        if (!settings.isAppEnabled(pkg)) {
            setBlocked(false)
            return
        }

        val overLimit = settings.blockEnabled && lastTotal >= settings.limit
        val locked = settings.isLockedNow(now)

        if (!overLimit && !locked) {
            setBlocked(false)
            return
        }

        // "Dismiss for 5 min" grants a temporary pass, then blocking resumes.
        if (now < dismissedUntil) {
            setBlocked(false)
            return
        }

        setBlocked(true)

        if (now - lastBackAt >= BACK_INTERVAL_MS) {
            lastBackAt = now
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        }

        if (settings.overlayEnabled) {
            overlay.showBlocker(
                appContext.getString(R.string.overlay_limit_reached),
                appContext.getString(R.string.overlay_dismiss)
            ) { dismissForFiveMinutes() }
        }
    }

    fun dismissForFiveMinutes() {
        dismissedUntil = System.currentTimeMillis() + DISMISS_MS
        setBlocked(false)
    }

    fun isBlocked(): Boolean = blocked

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
        ReelCountEvents.blockStateChanged(appContext, value)
    }

    companion object {
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
