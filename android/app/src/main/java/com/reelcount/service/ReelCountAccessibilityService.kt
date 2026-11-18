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
 * messages and posts are never captured.
 */
class ReelCountAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Package that currently owns the focused window. Rule 6. */
    @Volatile
    private var foregroundPackage: String? = null

    /** View id of the reel container currently on screen, if any. */
    private var visibleReelViewId: String? = null

    /** When [visibleReelViewId] first became visible (uptime millis). */
    private var visibleSince: Long = 0L

    /** Last time each view id produced a count, for the 1 s debounce. */
    private val lastCountAt = HashMap<String, Long>()

    private val viewIdsByPackage: Map<String, List<String>> = mapOf(
        TrackedApps.INSTAGRAM to listOf("clips_viewer_view_pager", "reel_player_page_container"),
        TrackedApps.YOUTUBE to listOf("reel_player_page_container", "shorts_player_container"),
        TrackedApps.FACEBOOK to listOf("reel_player_page_container", "clips_viewer_view_pager"),
        TrackedApps.SNAPCHAT to listOf("reel_player_page_container")
    )

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
        if (!TrackedApps.isTracked(pkg)) {
            return
        }

        if (target.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // Rule 6: remember which tracked app is actually in front.
            foregroundPackage = pkg
            visibleReelViewId = null
            visibleSince = 0L
            return
        }

        // Rule 6: ignore events for a tracked app that is not in the foreground.
        if (foregroundPackage != pkg) {
            return
        }

        val ids = viewIdsByPackage[pkg] ?: return
        val now = SystemClock.uptimeMillis()

        when (target.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val root = target.source ?: rootInActiveWindow
                observeVisibility(pkg, ids, root, now)
                maybeBlock(pkg)
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                val root = target.source ?: rootInActiveWindow
                observeVisibility(pkg, ids, root, now)

                val viewId = visibleReelViewId
                if (viewId != null && isDwellSatisfied(now)) {
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
        runCatching { OverlayManager.get(applicationContext).hidePill() }
        scope.cancel()
    }

    private fun observeVisibility(pkg: String, ids: List<String>, root: AccessibilityNodeInfo?, now: Long) {
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

    /** Rule 4: a reel must have been on screen for two seconds to count. */
    private fun isDwellSatisfied(now: Long): Boolean = visibleSince > 0L && now - visibleSince >= MIN_VISIBLE_MS

    private fun maybeCount(pkg: String, viewId: String, now: Long) {
        // Rule 3: the same view id cannot increment twice within one second.
        val previous = lastCountAt[viewId] ?: 0L
        if (now - previous < DEBOUNCE_MS) {
            return
        }
        lastCountAt[viewId] = now

        // The next reel starts accumulating dwell time from this moment.
        visibleSince = now

        scope.launch {
            try {
                val row = CountRepository.get(applicationContext).increment(pkg)
                val perApp = PerAppCounts.parse(row.perAppJson)

                ReelCountEvents.reelCounted(applicationContext, pkg, row.total, perApp)
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

    /** Rules 1, 2 and 5 in one place. */
    private fun isTrackedReelNode(node: AccessibilityNodeInfo, ids: List<String>): Boolean {
        val rawId = node.viewIdResourceName ?: return false
        val shortId = rawId.substringAfter(":id/", "")
        if (shortId.isEmpty()) {
            return false
        }

        // Rule 5: Stories use "reel_viewer_" ids and are never counted.
        if (shortId.startsWith(STORIES_PREFIX)) {
            return false
        }

        val isReelContainer =
            ids.contains(shortId) || shortId.startsWith(REEL_PLAYER_PREFIX)
        if (!isReelContainer) {
            return false
        }

        // Rule 1: zero-sized nodes are not real.
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.width() <= 0 || bounds.height() <= 0) {
            return false
        }

        // Rule 2: the node must actually be visible.
        if (!node.isVisibleToUser) {
            return false
        }

        return true
    }

    private companion object {
        const val TAG = "ReelCountA11y"

        /** Rule 4. */
        const val MIN_VISIBLE_MS = 2_000L

        /** Rule 3. */
        const val DEBOUNCE_MS = 1_000L

        const val MAX_TREE_DEPTH = 40

        const val STORIES_PREFIX = "reel_viewer_"
        const val REEL_PLAYER_PREFIX = "reel_player_"
    }
}
