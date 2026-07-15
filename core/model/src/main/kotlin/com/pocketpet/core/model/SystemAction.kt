package com.pocketpet.core.model

/**
 * Every system action Pocket Pet exposes, whether from the overlay quick menu, the dashboard, or
 * a parsed voice command. [PetActionExecutor] (defined in `core:domain`, implemented in
 * `core:system`) maps each of these to the narrowest public Android API or safe intent that can
 * fulfill it, and returns an [ActionResult] — never a silent no-op.
 */
enum class SystemAction {
    TorchOn,
    TorchOff,
    OpenCamera,
    OpenClock,
    OpenCalculator,
    OpenCalendar,
    OpenContacts,
    OpenPhone,
    OpenMessages,
    OpenBrightnessSettings,
    OpenBatterySaverSettings,
    OpenWifiSettings,
    OpenBluetoothSettings,
    OpenNotificationPanel,
    OpenQuickSettings,
    VolumeUp,
    VolumeDown,
    ShowTime,
    ShowBattery,
    ShowStorage,
    ShowMemory,
    ShowWeather,
    OpenBatteryOptimizationSettings,

    /** Launches a specific installed app the user picked ahead of time (see [LaunchAppRequest]). */
    LaunchSelectedApp,
}

/** Parameters for [SystemAction.LaunchSelectedApp]: the exact package the user chose. */
data class LaunchAppRequest(val packageName: String, val displayName: String)
