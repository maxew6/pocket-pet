package com.pocketpet.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpet.core.domain.repository.PermissionStatusProvider
import com.pocketpet.core.domain.repository.PetPreferencesRepository
import com.pocketpet.core.model.AppTheme
import com.pocketpet.core.model.PetPreferences
import com.pocketpet.core.model.QuietHours
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsPermissionStatus(
    val overlayGranted: Boolean,
    val notificationsGranted: Boolean,
    val notificationListenerGranted: Boolean,
    val accessibilityGranted: Boolean,
    val microphoneGranted: Boolean,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PetPreferencesRepository,
    private val permissionStatusProvider: PermissionStatusProvider,
) : ViewModel() {

    val preferences: StateFlow<PetPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetPreferences())

    fun permissionStatus(): SettingsPermissionStatus = SettingsPermissionStatus(
        overlayGranted = permissionStatusProvider.hasOverlayPermission(),
        notificationsGranted = permissionStatusProvider.hasNotificationPermission(),
        notificationListenerGranted = permissionStatusProvider.isNotificationListenerEnabled(),
        accessibilityGranted = permissionStatusProvider.isAccessibilityServiceEnabled(),
        microphoneGranted = permissionStatusProvider.hasRecordAudioPermission(),
    )

    fun setTheme(theme: AppTheme) = update { it.copy(theme = theme) }
    fun setEdgeSnapping(enabled: Boolean) = update { it.copy(edgeSnappingEnabled = enabled) }
    fun setPositionLocked(locked: Boolean) = update { it.copy(positionLocked = locked) }
    fun setNotificationReactionsEnabled(enabled: Boolean) = update { it.copy(notificationReactionsEnabled = enabled) }
    fun setAccessibilityFeaturesEnabled(enabled: Boolean) = update { it.copy(accessibilityFeaturesEnabled = enabled) }
    fun setVoiceCommandsEnabled(enabled: Boolean) = update { it.copy(voiceCommandsEnabled = enabled) }

    fun setQuietHoursEnabled(enabled: Boolean) = update {
        it.copy(quietHours = it.quietHours.copy(enabled = enabled))
    }

    fun setQuietHoursWindow(startHour: Int, endHour: Int) = update {
        it.copy(quietHours = QuietHours(enabled = it.quietHours.enabled, startHour = startHour, endHour = endHour))
    }

    private fun update(transform: (PetPreferences) -> PetPreferences) {
        viewModelScope.launch { preferencesRepository.update(transform) }
    }
}
