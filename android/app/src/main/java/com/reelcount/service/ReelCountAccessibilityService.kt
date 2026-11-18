package com.reelcount.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.reelcount.ReelCountEvents
import com.reelcount.data.CountRepository
import com.reelcount.data.SettingsRepository
import com.reelcount.detect.ReelDetector
import com.reelcount.ui.OverlayManager
import com.reelcount.ui.ReelBlocker
import com.reelcount.util.PerAppCounts
import com.reelcount.util.TrackedApps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Counts Reels and Shorts.
 *
 * The service only ever asks one question of the window tree: "is a known
 * reel container on screen?". It never reads text content, so passwords,
 * messages and posts are never captured. The decision rules themselves live in
 * [ReelDetector] so they can be unit tested without a device.
 */
class ReelCountAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Package that currently owns the focused window, or null when the
     * foreground app is not tracked. Rule 6 depends on this being cleared for
     * every app that comes forward, not only for the tracked ones.
     */
    @Volatile
    private var foregroundPackage: String? = null

    /** View id of the reel container currently on screen, if any. */
    private var visibleReelViewId: String? = null

    /** When [visibleReelViewId] first became visible (uptime millis). */
    private var visibleSince: Long = 0L

    /** Last time each view id produced a count, for the 1 s debounce. */
    private val lastCountAt = HashMap<String, Long>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility service connected")

        scope.launch {
            runCatching { SettingsRepository.get(applicationContext).refreshFromDatabase() }
                .onFailure { Log.w(TAG, "Could not refresh settings", it) }
            runCatching { OverlayManager.get(applicationContext).refreshFromSettings() }
                .onFailure { Log.w(TAG, "Could not refresh overlay", it) }
        }

        ReelCountForegroundService.start(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val target = event ?: return
        val pkg = target.packageName?.toString() ?: return

        // Rule 6: whoever comes to the front owns the counter — including apps
        // we do not track, which must clear the previous owner rather than
        // leaving a stale package "foreground" while the user is elsewhere.
        if (target.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            foregroundPackage = if (TrackedApps.isTracked(pkg)) pkg else null
            visibleReelViewId = null
            visibleSince = 0L
            return
        }

        if (foregroundPackage != pkg || !TrackedApps.isTracked(pkg)) {
            return
        }

        val ids = TrackedApps.viewIds(pkg)
        if (ids.isEmpty()) {
            return
        }
        val now = SystemClock.uptimeMillis()

        when (target.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val root = target.source ?: rootInActiveWindow
                observeVisibility(ids, root, now)
                maybeBlock(pkg)
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                val root = target.source ?: rootInActiveWindow
                observeVisibility(ids, root, now)

                val viewId = visibleReelViewId
                if (viewId != null && ReelDetector.isDwellSatisfied(visibleSince, now)) {
                    maybeCount(pkg, viewId, now)
                }

                maybeBlock(pkg)
            }
        }
    }

    override fun onInterrupt() {
        visibleReelViewId = null
        visibleSince = 0L
    }

    override fun onUnbind(intent: Intent?): Boolean {
        teardown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    private fun teardown() {
        visibleReelViewId = null
        visibleSince = 0L
        foregroundPackage = null
        lastCountAt.clear()
        // Both windows belong to a service that is going away: a pill left
        // behind is cosmetic, a full-screen blocker left behind traps the user.
        runCatching { OverlayManager.get(applicationContext).hidePill() }
        runCatching { OverlayManager.get(applicationContext).hideBlocker() }
        scope.cancel()
    }

    private fun observeVisibility(ids: List<String>, root: AccessibilityNodeInfo?, now: Long) {
        val node = findReelNode(root ?: rootInActiveWindow, ids)
        val viewId = node?.viewIdResourceName

        if (viewId == null) {
            visibleReelViewId = null
            visibleSince = 0L
            return
        }

        if (viewId != visibleReelViewId) {
            visibleReelViewId = viewId
            visibleSince = now
        }
    }

    private fun maybeCount(pkg: String, viewId: String, now: Long) {
        val previous = lastCountAt[viewId] ?: 0L
        if (!ReelDetector.isDebounceSatisfied(previous, now)) {
            return
        }
        lastCountAt[viewId] = now

        // The next reel starts accumulating dwell time from this moment.
        visibleSince = now

        scope.launch {
            try {
                val row = CountRepository.get(applicationContext).increment(pkg)
                val perApp = PerAppCounts.parse(row.perAppJson)

                ReelCountEvents.reelCounted(pkg, row.total, perApp)
                ReelCountForegroundService.updateCount(applicationContext, row.total)
                OverlayManager.get(applicationContext).onReelCounted(row.total, perApp)
                ReelBlocker.get(applicationContext).onReelCounted(pkg, row.total)
            } catch (error: Exception) {
                Log.e(TAG, "Could not record reel", error)
            }
        }
    }

    private fun maybeBlock(pkg: String) {
        scope.launch {
            try {
                ReelBlocker.get(applicationContext).evaluateBlock(this@ReelCountAccessibilityService, pkg)
            } catch (error: Exception) {
                Log.w(TAG, "Block evaluation failed", error)
            }
        }
    }

    // ---------------------------------------------------------------- tree

    private fun findReelNode(root: AccessibilityNodeInfo?, ids: List<String>): AccessibilityNodeInfo? {
        if (root == null) {
            return null
        }
        return search(root, ids, 0)
    }

    private fun search(node: AccessibilityNodeInfo?, ids: List<String>, depth: Int): AccessibilityNodeInfo? {
        if (node == null || depth > MAX_TREE_DEPTH) {
            return null
        }
        if (isTrackedReelNode(node, ids)) {
            return node
        }
        for (index in 0 until node.childCount) {
            val found = search(node.getChild(index), ids, depth + 1)
            if (found != null) {
                return found
            }
        }
        return null
    }

    private fun isTrackedReelNode(node: AccessibilityNodeInfo, ids: List<String>): Boolean {
        val shortId = ReelDetector.shortViewId(node.viewIdResourceName) ?: return false
        if (!ReelDetector.isReelContainerId(shortId, ids)) {
            return false
        }

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        return ReelDetector.isRealNode(
            width = bounds.width(),
            height = bounds.height(),
            visibleToUser = node.isVisibleToUser
        )
    }

    private companion object {
        const val TAG = "ReelCountA11y"
        const val MAX_TREE_DEPTH = 40
    }
}
