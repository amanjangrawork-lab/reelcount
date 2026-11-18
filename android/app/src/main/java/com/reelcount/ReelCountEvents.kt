package com.reelcount

import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * Fan-out point for counted reels.
 *
 * Every counted reel is delivered to (a) the React Native module through the
 * [emitter] lambda, and (b) an app-internal broadcast that other native
 * components (overlay, notification) can observe without holding a reference
 * to the accessibility service.
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

    fun reelCounted(context: Context, pkg: String, total: Int, perApp: Map<String, Int>) {
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
        emit(context, EVENT_REEL_COUNTED, payload)
    }

    fun limitReached(context: Context, pkg: String, total: Int) {
        val payload = Bundle().apply {
            putString(EXTRA_PKG, pkg)
            putInt(EXTRA_TOTAL, total)
        }
        emit(context, EVENT_LIMIT_REACHED, payload)
    }

    fun blockStateChanged(context: Context, blocked: Boolean) {
        val payload = Bundle().apply { putBoolean(EXTRA_BLOCKED, blocked) }
        emit(context, EVENT_BLOCK_STATE_CHANGED, payload)
    }

    private fun emit(context: Context, name: String, payload: Bundle) {
        emitter?.let { listener ->
            try {
                listener(name, payload)
            } catch (ignored: Exception) {
                // A dead React instance must never break counting.
            }
        }

        val intent = Intent(name).apply {
            setPackage(context.packageName)
            putExtras(payload)
        }
        try {
            context.sendBroadcast(intent)
        } catch (ignored: Exception) {
            // Broadcast is a convenience channel, not a critical path.
        }
    }
}
