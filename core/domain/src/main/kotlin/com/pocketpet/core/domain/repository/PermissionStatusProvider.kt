package com.pocketpet.core.domain.repository

/**
 * Read-only permission/feature status the UI needs to render (onboarding, settings, the home
 * dashboard's "permission health" summary). Implemented in `core:system` against the real
 * Android APIs; `feature:*` modules depend on this interface only, never on `core:system`
 * directly, keeping the dependency-inversion rule intact for something as UI-visible as
 * permission state.
 */
interface PermissionStatusProvider {
    fun hasOverlayPermission(): Boolean
    fun hasNotificationPermission(): Boolean
    fun hasRecordAudioPermission(): Boolean
    fun hasCoarseLocationPermission(): Boolean
    fun isNotificationListenerEnabled(): Boolean
    fun isAccessibilityServiceEnabled(): Boolean
}
