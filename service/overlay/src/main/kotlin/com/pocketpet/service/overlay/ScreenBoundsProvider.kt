package com.pocketpet.service.overlay

import android.content.Context
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager
import com.pocketpet.core.model.ScreenBounds

/**
 * Computes the current safe display area (full display minus status bar / nav bar / cutouts) so
 * the behavior engine and overlay never plan a position under a system bar. API 30+ reads real
 * [WindowInsets] from [WindowManager.currentWindowMetrics]; API 26-29 (no `currentWindowMetrics`)
 * falls back to display metrics plus the platform's own `status_bar_height` resource — a
 * long-standing, pragmatic technique for a pre-insets-API status bar estimate. Nav-bar height
 * isn't reliably queryable pre-30 without the same trick for a second resource that varies more
 * by OEM, so the legacy path only estimates the top inset and leaves the rest at zero, erring on
 * "assume less safe area" rather than guessing.
 */
class ScreenBoundsProvider(private val context: Context) {

    fun current(density: Float): ScreenBounds {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            currentModern(windowManager, density)
        } else {
            currentLegacy(windowManager, density)
        }
    }

    private fun currentModern(windowManager: WindowManager, density: Float): ScreenBounds {
        val metrics = windowManager.currentWindowMetrics
        val bounds = metrics.bounds
        val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
        )
        return ScreenBounds(
            widthDp = bounds.width() / density,
            heightDp = bounds.height() / density,
            topInsetDp = insets.top / density,
            bottomInsetDp = insets.bottom / density,
            leftInsetDp = insets.left / density,
            rightInsetDp = insets.right / density,
        )
    }

    @Suppress("DEPRECATION")
    private fun currentLegacy(windowManager: WindowManager, density: Float): ScreenBounds {
        val displayMetrics = android.util.DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        val statusBarResId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarPx = if (statusBarResId > 0) context.resources.getDimensionPixelSize(statusBarResId) else 0
        return ScreenBounds(
            widthDp = displayMetrics.widthPixels / density,
            heightDp = displayMetrics.heightPixels / density,
            topInsetDp = statusBarPx / density,
            bottomInsetDp = 0f,
            leftInsetDp = 0f,
            rightInsetDp = 0f,
        )
    }
}
