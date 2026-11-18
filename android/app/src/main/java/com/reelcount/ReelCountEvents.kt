package com.reelcount

import android.os.Bundle

/**
 * Fan-out point for counted reels.
 *
 * Every event is delivered to the React Native module through the [emitter]
 * lambda, which the module installs while the JS instance is alive. Native
 * components are called directly by the accessibility service, so there is no
 * broadcast channel here: one delivery path, not two.
 */
object ReelCountEvents {

    const val EVENT_REEL_COUNTED = "com.reelcount.event.REEL_COUNTED"
    const val EVENT_LIMIT_REACHED = "com.reelcount.event.LIMIT_REACHED"
    const val EVENT_BLOCK_STATE_CHANGED = "com.reelcount.event.BLOCK_STATE_CHANGED"

    const val EXTRA_PKG = "pkg"
    const val EXTRA_TOTAL = "total"
    const val EXTRA_PER_APP = "perApp"
    const val EXTRA_BLOCKED = "blocked"

    /** Registered by ReelCountModule while the React instance is alive. */
    @Volatile
    private var emitter: ((String, Bundle) -> Unit)? = null

    fun setEmitter(listener: ((String, Bundle) -> Unit)?) {
        emitter = listener
    }

    fun reelCounted(pkg: String, total: Int, perApp: Map<String, Int>) {
        val payload = Bundle().apply {
            putString(EXTRA_PKG, pkg)
            putInt(EXTRA_TOTAL, total)
            putBundle(
                EXTRA_PER_APP,
                Bundle().apply {
                    for ((key, value) in perApp) {
                        putInt(key, value)
                    }
                }
            )
        }
        emit(EVENT_REEL_COUNTED, payload)
    }

    fun limitReached(pkg: String, total: Int) {
        val payload = Bundle().apply {
            putString(EXTRA_PKG, pkg)
            putInt(EXTRA_TOTAL, total)
        }
        emit(EVENT_LIMIT_REACHED, payload)
    }

    fun blockStateChanged(blocked: Boolean) {
        val payload = Bundle().apply { putBoolean(EXTRA_BLOCKED, blocked) }
        emit(EVENT_BLOCK_STATE_CHANGED, payload)
    }

    private fun emit(name: String, payload: Bundle) {
        val listener = emitter ?: return
        try {
            listener(name, payload)
        } catch (ignored: Exception) {
            // A dead React instance must never break counting.
        }
    }
}
