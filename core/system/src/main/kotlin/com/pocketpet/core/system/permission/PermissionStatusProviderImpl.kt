package com.pocketpet.core.system.permission

import com.pocketpet.core.domain.repository.PermissionStatusProvider

private const val ACCESSIBILITY_SERVICE_CLASS_NAME = "com.pocketpet.service.overlay.accessibility.PetAccessibilityService"

class PermissionStatusProviderImpl(
    private val permissionChecker: PermissionChecker,
) : PermissionStatusProvider {
    override fun hasOverlayPermission(): Boolean = permissionChecker.hasOverlayPermission()
    override fun hasNotificationPermission(): Boolean = permissionChecker.hasNotificationPermission()
    override fun hasRecordAudioPermission(): Boolean = permissionChecker.hasRecordAudioPermission()
    override fun hasCoarseLocationPermission(): Boolean = permissionChecker.hasCoarseLocationPermission()
    override fun isNotificationListenerEnabled(): Boolean = permissionChecker.isNotificationListenerEnabled()
    override fun isAccessibilityServiceEnabled(): Boolean =
        permissionChecker.isAccessibilityServiceEnabled(ACCESSIBILITY_SERVICE_CLASS_NAME)
}
