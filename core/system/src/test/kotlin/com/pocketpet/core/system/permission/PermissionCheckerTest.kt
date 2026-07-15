package com.pocketpet.core.system.permission

import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PermissionCheckerTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val checker = PermissionChecker(context)

    @Test
    fun `accessibility service is reported disabled when the setting is empty`() {
        Settings.Secure.putString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "")
        assertThat(checker.isAccessibilityServiceEnabled("com.pocketpet.service.overlay.PetAccessibilityService"))
            .isFalse()
    }

    @Test
    fun `accessibility service is reported enabled when it appears in the colon-separated list`() {
        val serviceClass = "com.pocketpet.service.overlay.PetAccessibilityService"
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            "com.other.app/com.other.SomeService:${context.packageName}/$serviceClass",
        )
        assertThat(checker.isAccessibilityServiceEnabled(serviceClass)).isTrue()
    }

    @Test
    fun `notification listener is reported disabled when this package is absent`() {
        Settings.Secure.putString(context.contentResolver, "enabled_notification_listeners", "com.other.app/com.other.Listener")
        assertThat(checker.isNotificationListenerEnabled()).isFalse()
    }

    @Test
    fun `notification listener is reported enabled when this package is present`() {
        Settings.Secure.putString(
            context.contentResolver,
            "enabled_notification_listeners",
            "${context.packageName}/com.pocketpet.service.overlay.PetNotificationListenerService",
        )
        assertThat(checker.isNotificationListenerEnabled()).isTrue()
    }
}
