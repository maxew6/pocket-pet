package com.pocketpet.core.data.di

import android.content.Context
import com.pocketpet.core.domain.action.AccessibilityActionBridge
import com.pocketpet.core.domain.action.InstalledAppResolver
import com.pocketpet.core.domain.action.PetActionExecutor
import com.pocketpet.core.domain.provider.DispatcherProvider
import com.pocketpet.core.domain.repository.NotificationEventSource
import com.pocketpet.core.domain.repository.PermissionStatusProvider
import com.pocketpet.core.domain.repository.SystemStatusRepository
import com.pocketpet.core.domain.repository.WeatherRepository
import com.pocketpet.core.system.DefaultDispatcherProvider
import com.pocketpet.core.system.accessibility.AccessibilityServiceBridge
import com.pocketpet.core.system.action.InstalledAppResolverImpl
import com.pocketpet.core.system.action.PetActionExecutorImpl
import com.pocketpet.core.system.battery.AndroidBatteryMonitor
import com.pocketpet.core.system.flashlight.FlashlightController
import com.pocketpet.core.system.location.DeviceLocationProvider
import com.pocketpet.core.system.notification.NotificationEventBus
import com.pocketpet.core.system.permission.PermissionChecker
import com.pocketpet.core.system.permission.PermissionStatusProviderImpl
import com.pocketpet.core.system.voice.VoiceCommandController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SystemModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides
    @Singleton
    fun providePermissionChecker(@ApplicationContext context: Context): PermissionChecker =
        PermissionChecker(context)

    @Provides
    @Singleton
    fun provideFlashlightController(@ApplicationContext context: Context): FlashlightController =
        FlashlightController(context)

    @Provides
    @Singleton
    fun provideVoiceCommandController(@ApplicationContext context: Context): VoiceCommandController =
        VoiceCommandController(context)

    @Provides
    @Singleton
    fun provideDeviceLocationProvider(
        @ApplicationContext context: Context,
        permissionChecker: PermissionChecker,
    ): DeviceLocationProvider = DeviceLocationProvider(context, permissionChecker)

    @Provides
    @Singleton
    fun provideSystemStatusRepository(@ApplicationContext context: Context): SystemStatusRepository =
        AndroidBatteryMonitor(context)

    @Provides
    @Singleton
    fun provideInstalledAppResolver(@ApplicationContext context: Context): InstalledAppResolver =
        InstalledAppResolverImpl(context)

    // --- Service bridges: one singleton instance shared between its concrete holder type (which
    // the real Service attaches itself to) and the domain interface it implements. ---

    @Provides
    @Singleton
    fun provideAccessibilityServiceBridge(): AccessibilityServiceBridge = AccessibilityServiceBridge()

    @Provides
    @Singleton
    fun provideAccessibilityActionBridge(bridge: AccessibilityServiceBridge): AccessibilityActionBridge = bridge

    @Provides
    @Singleton
    fun provideNotificationEventBus(): NotificationEventBus = NotificationEventBus()

    @Provides
    @Singleton
    fun provideNotificationEventSource(bus: NotificationEventBus): NotificationEventSource = bus

    @Provides
    @Singleton
    fun providePermissionStatusProvider(permissionChecker: PermissionChecker): PermissionStatusProvider =
        PermissionStatusProviderImpl(permissionChecker)

    @Provides
    @Singleton
    fun providePetActionExecutor(
        @ApplicationContext context: Context,
        flashlightController: FlashlightController,
        accessibilityBridge: AccessibilityActionBridge,
        weatherRepository: WeatherRepository,
        dispatchers: DispatcherProvider,
    ): PetActionExecutor = PetActionExecutorImpl(
        appContext = context,
        flashlightController = flashlightController,
        accessibilityBridge = accessibilityBridge,
        weatherRepository = weatherRepository,
        dispatchers = dispatchers,
    )
}
