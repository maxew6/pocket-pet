package com.pocketpet.core.system.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * A single place that knows how to check every permission Pocket Pet needs — ordinary runtime
 * permissions via [PackageManager], and the two special "draw over other apps" / "write system
 * settings" permissions via their dedicated `Settings.canX()` checks (they are not requestable
 * through the normal runtime permission dialog).
 */
class PermissionChecker(private val appContext: Context) {

    fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(appContext)

    fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return hasRuntimePermission(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun hasRecordAudioPermission(): Boolean = hasRuntimePermission(Manifest.permission.RECORD_AUDIO)

    fun hasCoarseLocationPermission(): Boolean = hasRuntimePermission(Manifest.permission.ACCESS_COARSE_LOCATION)

    fun hasWriteSettingsPermission(): Boolean = Settings.System.canWrite(appContext)

    fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            appContext.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        return enabledListeners.contains(appContext.packageName)
    }

    fun isAccessibilityServiceEnabled(serviceClassName: String): Boolean {
        val enabledServices = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabledServices.contains("${appContext.packageName}/$serviceClassName")
    }

    private fun hasRuntimePermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
}
