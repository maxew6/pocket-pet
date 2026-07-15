package com.pocketpet.core.model

/**
 * A newly posted notification's *metadata only*. Pocket Pet's `NotificationListenerService`
 * deliberately never reads title, text, or any extras — see `core:system`'s listener
 * implementation — so this class has no field capable of holding notification content by
 * construction, not just by convention.
 */
data class NotificationEvent(
    val postingPackageName: String,
    val postedAtEpochMillis: Long,
)
