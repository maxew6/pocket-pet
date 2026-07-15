package com.pocketpet.core.domain.action

/** The narrow set of AccessibilityService global actions Pocket Pet is allowed to perform. */
enum class AccessibilityGlobalAction {
    OpenNotificationPanel,
    OpenQuickSettings,
}

/**
 * Bridges `core:system`'s [PetActionExecutor] to the running accessibility service (which lives
 * in `service:overlay`, since only the OS can instantiate an `AccessibilityService`) without
 * `core:system` needing a module dependency on `service:overlay`. Implemented by the service
 * itself, which registers/clears its instance as it connects/disconnects.
 */
interface AccessibilityActionBridge {
    fun isServiceConnected(): Boolean
    fun performGlobalAction(action: AccessibilityGlobalAction): Boolean
}
