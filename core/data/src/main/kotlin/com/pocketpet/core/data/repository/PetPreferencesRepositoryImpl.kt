package com.pocketpet.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pocketpet.core.domain.repository.PetPreferencesRepository
import com.pocketpet.core.model.Accessory
import com.pocketpet.core.model.AppTheme
import com.pocketpet.core.model.ColorTone
import com.pocketpet.core.model.PetAppearance
import com.pocketpet.core.model.PetPosition
import com.pocketpet.core.model.PetPreferences
import com.pocketpet.core.model.PetState
import com.pocketpet.core.model.QuietHours
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.petPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "pet_preferences")

private object Keys {
    val PET_NAME = stringPreferencesKey("pet_name")
    val COLOR_TONE = stringPreferencesKey("color_tone")
    val ACCESSORY = stringPreferencesKey("accessory")
    val SCALE = floatPreferencesKey("scale")
    val OPACITY = floatPreferencesKey("opacity")
    val ANIMATION_SPEED = floatPreferencesKey("animation_speed")
    val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
    val THEME = stringPreferencesKey("theme")
    val FAVORITE_POSITION_X = floatPreferencesKey("favorite_position_x")
    val FAVORITE_POSITION_Y = floatPreferencesKey("favorite_position_y")
    val FAVORITE_BEHAVIOR = stringPreferencesKey("favorite_behavior")
    val EDGE_SNAPPING = booleanPreferencesKey("edge_snapping")
    val POSITION_LOCKED = booleanPreferencesKey("position_locked")
    val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
    val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
    val QUIET_HOURS_START = intPreferencesKey("quiet_hours_start")
    val QUIET_HOURS_END = intPreferencesKey("quiet_hours_end")
    val NOTIFICATION_REACTIONS_ENABLED = booleanPreferencesKey("notification_reactions_enabled")
    val EXCLUDED_NOTIFICATION_PACKAGES = stringSetPreferencesKey("excluded_notification_packages")
    val ACCESSIBILITY_FEATURES_ENABLED = booleanPreferencesKey("accessibility_features_enabled")
    val VOICE_COMMANDS_ENABLED = booleanPreferencesKey("voice_commands_enabled")
    val WEATHER_LOCATION_LABEL = stringPreferencesKey("weather_location_label")
    val USE_DEVICE_LOCATION_FOR_WEATHER = booleanPreferencesKey("use_device_location_for_weather")
    val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
}

class PetPreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : PetPreferencesRepository {

    override val preferences: Flow<PetPreferences> = dataStore.data.map { it.toDomain() }

    override suspend fun current(): PetPreferences = preferences.first()

    override suspend fun update(transform: (PetPreferences) -> PetPreferences) {
        dataStore.edit { prefs ->
            val updated = transform(prefs.toDomain())
            prefs.writeFrom(updated)
        }
    }

    private fun Preferences.toDomain(): PetPreferences {
        val quietHours = QuietHours(
            enabled = this[Keys.QUIET_HOURS_ENABLED] ?: true,
            startHour = this[Keys.QUIET_HOURS_START] ?: 22,
            endHour = this[Keys.QUIET_HOURS_END] ?: 7,
        )
        val favoriteX = this[Keys.FAVORITE_POSITION_X]
        val favoriteY = this[Keys.FAVORITE_POSITION_Y]
        return PetPreferences(
            appearance = PetAppearance(
                name = this[Keys.PET_NAME] ?: "Mochi",
                colorTone = this[Keys.COLOR_TONE]?.toEnumOrDefault(ColorTone.Milk) ?: ColorTone.Milk,
                accessory = this[Keys.ACCESSORY]?.toEnumOrDefault(Accessory.None) ?: Accessory.None,
                scale = this[Keys.SCALE] ?: 1f,
                opacity = this[Keys.OPACITY] ?: 1f,
                animationSpeedMultiplier = this[Keys.ANIMATION_SPEED] ?: 1f,
                reducedMotion = this[Keys.REDUCED_MOTION] ?: false,
            ),
            theme = this[Keys.THEME]?.toEnumOrDefault(AppTheme.System) ?: AppTheme.System,
            favoritePosition = if (favoriteX != null && favoriteY != null) {
                PetPosition(favoriteX, favoriteY)
            } else {
                null
            },
            favoriteBehavior = this[Keys.FAVORITE_BEHAVIOR]?.toEnumOrNull<PetState>(),
            edgeSnappingEnabled = this[Keys.EDGE_SNAPPING] ?: true,
            positionLocked = this[Keys.POSITION_LOCKED] ?: false,
            overlayEnabled = this[Keys.OVERLAY_ENABLED] ?: false,
            quietHours = quietHours,
            notificationReactionsEnabled = this[Keys.NOTIFICATION_REACTIONS_ENABLED] ?: false,
            excludedNotificationPackages = this[Keys.EXCLUDED_NOTIFICATION_PACKAGES] ?: emptySet(),
            accessibilityFeaturesEnabled = this[Keys.ACCESSIBILITY_FEATURES_ENABLED] ?: false,
            voiceCommandsEnabled = this[Keys.VOICE_COMMANDS_ENABLED] ?: false,
            weatherLocationLabel = this[Keys.WEATHER_LOCATION_LABEL],
            useDeviceLocationForWeather = this[Keys.USE_DEVICE_LOCATION_FOR_WEATHER] ?: false,
            hasCompletedOnboarding = this[Keys.HAS_COMPLETED_ONBOARDING] ?: false,
        )
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.writeFrom(prefs: PetPreferences) {
        this[Keys.PET_NAME] = prefs.appearance.name
        this[Keys.COLOR_TONE] = prefs.appearance.colorTone.name
        this[Keys.ACCESSORY] = prefs.appearance.accessory.name
        this[Keys.SCALE] = prefs.appearance.scale
        this[Keys.OPACITY] = prefs.appearance.opacity
        this[Keys.ANIMATION_SPEED] = prefs.appearance.animationSpeedMultiplier
        this[Keys.REDUCED_MOTION] = prefs.appearance.reducedMotion
        this[Keys.THEME] = prefs.theme.name
        prefs.favoritePosition?.let {
            this[Keys.FAVORITE_POSITION_X] = it.xDp
            this[Keys.FAVORITE_POSITION_Y] = it.yDp
        }
        prefs.favoriteBehavior?.let { this[Keys.FAVORITE_BEHAVIOR] = it.name }
        this[Keys.EDGE_SNAPPING] = prefs.edgeSnappingEnabled
        this[Keys.POSITION_LOCKED] = prefs.positionLocked
        this[Keys.OVERLAY_ENABLED] = prefs.overlayEnabled
        this[Keys.QUIET_HOURS_ENABLED] = prefs.quietHours.enabled
        this[Keys.QUIET_HOURS_START] = prefs.quietHours.startHour
        this[Keys.QUIET_HOURS_END] = prefs.quietHours.endHour
        this[Keys.NOTIFICATION_REACTIONS_ENABLED] = prefs.notificationReactionsEnabled
        this[Keys.EXCLUDED_NOTIFICATION_PACKAGES] = prefs.excludedNotificationPackages
        this[Keys.ACCESSIBILITY_FEATURES_ENABLED] = prefs.accessibilityFeaturesEnabled
        this[Keys.VOICE_COMMANDS_ENABLED] = prefs.voiceCommandsEnabled
        prefs.weatherLocationLabel?.let { this[Keys.WEATHER_LOCATION_LABEL] = it }
        this[Keys.USE_DEVICE_LOCATION_FOR_WEATHER] = prefs.useDeviceLocationForWeather
        this[Keys.HAS_COMPLETED_ONBOARDING] = prefs.hasCompletedOnboarding
    }
}
