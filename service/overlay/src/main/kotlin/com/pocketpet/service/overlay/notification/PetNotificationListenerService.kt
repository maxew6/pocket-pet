package com.pocketpet.service.overlay.notification

import android.service.notification.StatusBarNotification
import com.pocketpet.core.domain.provider.ClockProvider
import com.pocketpet.core.domain.repository.PetPreferencesRepository
import com.pocketpet.core.model.NotificationEvent
import com.pocketpet.core.model.PetPreferences
import com.pocketpet.core.system.notification.NotificationEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Disabled until the person explicitly grants notification-listener access *and* turns on the
 * in-app "notification reactions" toggle. Reacts only to a notification being posted — this class
 * never reads `sbn.notification.extras` (title, text, or anything else), so there is nothing here
 * capable of leaking notification content into the pet's behavior or speech.
 *
 * [onNotificationPosted] is a plain OS callback, not a suspend function, so preferences (which are
 * read through a suspend/Flow API) are mirrored into plain volatile fields by a small coroutine
 * started in [onListenerConnected] — the callback itself only ever does cheap, synchronous reads.
 */
@AndroidEntryPoint
class PetNotificationListenerService : android.service.notification.NotificationListenerService() {

    @Inject
    lateinit var eventBus: NotificationEventBus

    @Inject
    lateinit var preferencesRepository: PetPreferencesRepository

    @Inject
    lateinit var clock: ClockProvider

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var cachedPreferences: PetPreferences = PetPreferences()
    @Volatile private var lastPublishedAtEpochMillis: Long = 0L

    override fun onListenerConnected() {
        super.onListenerConnected()
        preferencesRepository.preferences
            .onEach { cachedPreferences = it }
            .launchIn(serviceScope)
    }

    override fun onListenerDisconnected() {
        serviceScope.cancel()
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val prefs = cachedPreferences
        if (!prefs.notificationReactionsEnabled) return
        if (sbn.packageName == packageName) return // ignore Pocket Pet's own foreground-service notification
        if (sbn.packageName in prefs.excludedNotificationPackages) return
        if (prefs.quietHours.contains(clock.currentHourOfDay())) return

        val now = clock.nowEpochMillis()
        if (now - lastPublishedAtEpochMillis < NOTIFICATION_DEBOUNCE_MILLIS) return
        lastPublishedAtEpochMillis = now

        eventBus.publish(NotificationEvent(postingPackageName = sbn.packageName, postedAtEpochMillis = now))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) = Unit

    private companion object {
        const val NOTIFICATION_DEBOUNCE_MILLIS = 8_000L
    }
}
