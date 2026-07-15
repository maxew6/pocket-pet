package com.pocketpet.core.domain.repository

import com.pocketpet.core.model.BatteryStatus
import com.pocketpet.core.model.NotificationEvent
import kotlinx.coroutines.flow.Flow

/**
 * Live battery/charging status. The single interface [PetBehaviorEngine] depends on; the
 * Android-framework implementation ([com.pocketpet.core.system.battery.BatteryMonitor], bound
 * through [SystemStatusRepository]) listens to `ACTION_BATTERY_CHANGED` and friends.
 */
interface SystemStatusRepository {
    val batteryStatus: Flow<BatteryStatus>
}

/**
 * Emits metadata-only [NotificationEvent]s from Pocket Pet's opt-in `NotificationListenerService`.
 * Emits nothing at all if the person hasn't granted notification-listener access.
 */
interface NotificationEventSource {
    val events: Flow<NotificationEvent>
}
