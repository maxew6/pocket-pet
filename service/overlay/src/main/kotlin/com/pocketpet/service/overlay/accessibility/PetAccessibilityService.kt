package com.pocketpet.service.overlay.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.pocketpet.core.domain.action.AccessibilityActionBridge
import com.pocketpet.core.domain.action.AccessibilityGlobalAction
import com.pocketpet.core.system.accessibility.AccessibilityServiceBridge
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Disabled by default; the person must explicitly turn this on in Android's Accessibility
 * settings after reading Pocket Pet's in-app explanation screen (see `feature:onboarding`).
 *
 * This service does exactly one thing: forward [AccessibilityGlobalAction] requests to the real
 * `performGlobalAction()` global-action API. It does not override `onAccessibilityEvent` for
 * content inspection (the config in `accessibility_service_config.xml` doesn't even request the
 * event types that would be needed to read window content), does not log keystrokes, and does not
 * inspect password fields.
 */
@AndroidEntryPoint
class PetAccessibilityService : AccessibilityService(), AccessibilityActionBridge {

    @Inject
    lateinit var bridge: AccessibilityServiceBridge

    override fun onServiceConnected() {
        super.onServiceConnected()
        bridge.attach(this)
    }

    override fun onDestroy() {
        bridge.detach()
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        bridge.detach()
        return super.onUnbind(intent)
    }

    /** Intentionally empty: this service never inspects accessibility event content. */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun isServiceConnected(): Boolean = true

    override fun performGlobalAction(action: AccessibilityGlobalAction): Boolean {
        val globalActionConstant = when (action) {
            AccessibilityGlobalAction.OpenNotificationPanel -> GLOBAL_ACTION_NOTIFICATIONS
            AccessibilityGlobalAction.OpenQuickSettings -> GLOBAL_ACTION_QUICK_SETTINGS
        }
        return performGlobalAction(globalActionConstant)
    }
}
