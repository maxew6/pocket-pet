package com.pocketpet.core.system.notification

import com.pocketpet.core.domain.repository.NotificationEventSource
import com.pocketpet.core.model.NotificationEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Same pattern as [com.pocketpet.core.system.accessibility.AccessibilityServiceBridge]: the OS
 * creates the real `NotificationListenerService`, so this singleton is the stable object every
 * other class is injected with. The service calls [publish] for every newly posted notification
 * (metadata only — see [NotificationEvent]); everyone else just collects [events].
 */
class NotificationEventBus : NotificationEventSource {

    private val _events = MutableSharedFlow<NotificationEvent>(extraBufferCapacity = 8)
    override val events: Flow<NotificationEvent> = _events.asSharedFlow()

    fun publish(event: NotificationEvent) {
        _events.tryEmit(event)
    }
}
