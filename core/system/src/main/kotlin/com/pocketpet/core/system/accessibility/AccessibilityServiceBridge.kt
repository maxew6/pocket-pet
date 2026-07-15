package com.pocketpet.core.system.accessibility

import com.pocketpet.core.domain.action.AccessibilityActionBridge
import com.pocketpet.core.domain.action.AccessibilityGlobalAction

/**
 * The OS — not Hilt — creates the actual `AccessibilityService` instance, so nothing can inject
 * "the running service" directly. Instead, this singleton is what gets injected everywhere
 * ([com.pocketpet.core.system.action.PetActionExecutorImpl] included); the real service attaches
 * itself here in `onServiceConnected()` and detaches in `onDestroy()`, so callers always see an
 * accurate [isServiceConnected] regardless of the service's own lifecycle.
 */
class AccessibilityServiceBridge : AccessibilityActionBridge {

    @Volatile
    private var delegate: AccessibilityActionBridge? = null

    fun attach(delegate: AccessibilityActionBridge) {
        this.delegate = delegate
    }

    fun detach() {
        delegate = null
    }

    override fun isServiceConnected(): Boolean = delegate != null

    override fun performGlobalAction(action: AccessibilityGlobalAction): Boolean =
        delegate?.performGlobalAction(action) ?: false
}
