package com.pocketpet.core.model

/**
 * Every setting that persists across process death and app restarts, backed by
 * `PetPreferencesRepository` (DataStore under the hood). This is intentionally separate from
 * [PetNeeds]/[PetState] (which live in [PetMemoryRepository]/Room as a rolling history) — this
 * class is "what the person configured", not "what the pet is doing right now".
 */
data class PetPreferences(
    val appearance: PetAppearance = PetAppearance(),
    val theme: AppTheme = AppTheme.System,
    val favoritePosition: PetPosition? = null,
    val favoriteBehavior: PetState? = null,
    val edgeSnappingEnabled: Boolean = true,
    val positionLocked: Boolean = false,
    val overlayEnabled: Boolean = false,
    val quietHours: QuietHours = QuietHours(),
    val notificationReactionsEnabled: Boolean = false,
    val excludedNotificationPackages: Set<String> = emptySet(),
    val accessibilityFeaturesEnabled: Boolean = false,
    val voiceCommandsEnabled: Boolean = false,
    val weatherLocationLabel: String? = null,
    val useDeviceLocationForWeather: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
)
