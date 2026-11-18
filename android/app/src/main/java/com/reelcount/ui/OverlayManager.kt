package com.reelcount.ui

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.reelcount.R
import com.reelcount.data.CountRepository
import com.reelcount.data.SettingsRepository
import com.reelcount.util.AccessibilityUtils
import com.reelcount.util.TrackedApps
import kotlin.math.abs

/**
 * Draws the floating counter pill and the limit overlay with WindowManager.
 *
 * Colours come from `res/values/colors.xml`; the pill window is only as large as
 * the pill, so it never intercepts touches outside its own bounds.
 */
class OverlayManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())

    private var pill: LinearLayout? = null
    private var pillParams: WindowManager.LayoutParams? = null
    private var titleView: TextView? = null
    private var detailView: TextView? = null
    private var blocker: View? = null

    private var expanded = false
    private var lastTotal = 0
    private var lastPerApp: Map<String, Int> = emptyMap()

    private val autoHide = Runnable { hidePill() }

    /** Reloads settings and today's numbers, then shows or hides the pill. */
    suspend fun refreshFromSettings() {
        val settings = SettingsRepository.get(appContext).current()
        val counts = CountRepository.get(appContext)
        val total = counts.todayTotal()
        val perApp = counts.perAppCounts()

        onMain {
            lastTotal = total
            lastPerApp = perApp
            if (settings.overlayEnabled) {
                ensurePillShown()
                renderPill()
                scheduleAutoHide()
            } else {
                hidePillInternal()
            }
        }
    }

    /** Called for every counted reel. */
    fun onReelCounted(total: Int, perApp: Map<String, Int>) {
        onMain {
            lastTotal = total
            lastPerApp = perApp

            val settings = SettingsRepository.get(appContext).current()
            if (!settings.overlayEnabled) {
                hidePillInternal()
                return@onMain
            }
            ensurePillShown()
            renderPill()
            scheduleAutoHide()
        }
    }

    fun hidePill() {
        onMain { hidePillInternal() }
    }

    fun showBlocker(message: String, dismissLabel: String, onDismiss: () -> Unit) {
        onMain {
            if (blocker != null) {
                return@onMain
            }
            if (!AccessibilityUtils.canDrawOverlays(appContext)) {
                return@onMain
            }

            val root = LinearLayout(appContext).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundColor(color(R.color.reelcount_blocker_background))
                setPadding(dp(28), dp(28), dp(28), dp(28))
            }

            val title = TextView(appContext).apply {
                text = message
                setTextColor(color(R.color.reelcount_pill_text))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                gravity = Gravity.CENTER
            }

            val button = TextView(appContext).apply {
                text = dismissLabel
                setTextColor(color(R.color.reelcount_blocker_button_text))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                gravity = Gravity.CENTER
                isClickable = true
                setPadding(dp(22), dp(14), dp(22), dp(14))
                background = GradientDrawable().apply {
                    cornerRadius = dp(28).toFloat()
                    setColor(color(R.color.reelcount_accent))
                }
                setOnClickListener {
                    onDismiss()
                    hideBlocker()
                }
            }

            root.addView(
                title,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(28) }
            )
            root.addView(
                button,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            runCatching { windowManager.addView(root, params) }
                .onSuccess { blocker = root }
                .onFailure { Log.w(TAG, "Could not add blocker overlay", it) }
        }
    }

    fun hideBlocker() {
        onMain {
            val view = blocker ?: return@onMain
            runCatching { windowManager.removeView(view) }
            blocker = null
        }
    }

    // ------------------------------------------------------------- internals

    private fun ensurePillShown() {
        if (!AccessibilityUtils.canDrawOverlays(appContext)) {
            return
        }
        if (pill != null) {
            return
        }

        val view = createPill()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(120)
        }

        runCatching { windowManager.addView(view, params) }
            .onSuccess {
                pill = view
                pillParams = params
                renderPill()
            }
            .onFailure { Log.w(TAG, "Could not add counter pill", it) }
    }

    private fun hidePillInternal() {
        handler.removeCallbacks(autoHide)
        val view = pill ?: return
        runCatching { windowManager.removeView(view) }
        pill = null
        pillParams = null
        titleView = null
        detailView = null
        expanded = false
    }

    private fun createPill(): LinearLayout {
        val container = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(9), dp(16), dp(9))
            background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setColor(color(R.color.reelcount_pill))
                setStroke(dp(1), color(R.color.reelcount_pill_border))
            }
            elevation = dp(6).toFloat()
        }

        titleView = TextView(appContext).apply {
            setTextColor(color(R.color.reelcount_pill_text))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
        }

        detailView = TextView(appContext).apply {
            setTextColor(color(R.color.reelcount_pill_detail))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
            visibility = View.GONE
        }

        container.addView(titleView)
        container.addView(detailView)
        container.setOnTouchListener(DragTouchListener())
        return container
    }

    private fun renderPill() {
        titleView?.text = "$lastTotal reels"
        val detail = detailView ?: return
        if (!expanded) {
            detail.visibility = View.GONE
            detail.text = ""
            return
        }
        detail.visibility = View.VISIBLE
        detail.text = breakdownText()
    }

    private fun breakdownText(): String {
        if (lastPerApp.isEmpty()) {
            return "No reels yet today"
        }
        return lastPerApp.entries
            .sortedByDescending { it.value }
            .joinToString("\n") { "${TrackedApps.displayName(it.key)}: ${it.value}" }
    }

    private fun toggleExpanded() {
        expanded = !expanded
        renderPill()
        scheduleAutoHide()
    }

    private fun scheduleAutoHide() {
        handler.removeCallbacks(autoHide)
        handler.postDelayed(autoHide, AUTO_HIDE_MS)
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            handler.post(block)
        }
    }

    private fun color(resId: Int): Int = ContextCompat.getColor(appContext, resId)

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        appContext.resources.displayMetrics
    ).toInt()

    /** Drag to move, tap to expand the per-app breakdown. */
    private inner class DragTouchListener : View.OnTouchListener {

        private var initialX = 0
        private var initialY = 0
        private var touchX = 0f
        private var touchY = 0f
        private var dragging = false

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            val params = pillParams ?: return false

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    dragging = false
                    handler.removeCallbacks(autoHide)
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - touchX).toInt()
                    val deltaY = (event.rawY - touchY).toInt()

                    if (abs(deltaX) > dp(6) || abs(deltaY) > dp(6)) {
                        dragging = true
                    }

                    if (dragging) {
                        params.x = initialX + deltaX
                        params.y = initialY + deltaY
                        runCatching { windowManager.updateViewLayout(view, params) }
                    }
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    if (!dragging && event.eventTime - event.downTime < TAP_TIMEOUT_MS) {
                        toggleExpanded()
                    } else {
                        scheduleAutoHide()
                    }
                    return true
                }

                MotionEvent.ACTION_CANCEL -> {
                    scheduleAutoHide()
                    return true
                }
            }
            return false
        }
    }

    companion object {
        private const val TAG = "OverlayManager"
        private const val AUTO_HIDE_MS = 5_000L
        private const val TAP_TIMEOUT_MS = 300L

        @Volatile
        private var instance: OverlayManager? = null

        fun get(context: Context): OverlayManager {
            return instance ?: synchronized(this) {
                instance ?: OverlayManager(context).also { instance = it }
            }
        }
    }
}
