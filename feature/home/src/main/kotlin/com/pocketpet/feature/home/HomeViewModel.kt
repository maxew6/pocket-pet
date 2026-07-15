package com.pocketpet.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpet.core.domain.action.OverlayController
import com.pocketpet.core.domain.behavior.UserInteractionType
import com.pocketpet.core.domain.repository.PermissionStatusProvider
import com.pocketpet.core.domain.repository.PetPreferencesRepository
import com.pocketpet.core.domain.repository.PetRepository
import com.pocketpet.core.domain.usecase.ProcessBehaviorTickUseCase
import com.pocketpet.core.model.PetPreferences
import com.pocketpet.core.model.PetSnapshot
import com.pocketpet.core.model.ScreenBounds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val petRepository: PetRepository,
    private val preferencesRepository: PetPreferencesRepository,
    private val processBehaviorTick: ProcessBehaviorTickUseCase,
    private val overlayController: OverlayController,
    private val permissionStatusProvider: PermissionStatusProvider,
) : ViewModel() {

    val snapshot: StateFlow<PetSnapshot> = petRepository.snapshot

    val preferences: StateFlow<PetPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetPreferences())

    // The dashboard isn't screen-size-aware the way the overlay is, and dashboard-triggered
    // interactions never target a specific position, so a generous placeholder is sufficient here.
    private val dashboardBounds = ScreenBounds(widthDp = 1000f, heightDp = 2000f)

    fun feed() = tick(UserInteractionType.Feed)
    fun play() = tick(UserInteractionType.Play)
    fun toggleSleep() = tick(UserInteractionType.ToggleSleep)

    private fun tick(interaction: UserInteractionType) {
        viewModelScope.launch { processBehaviorTick(dashboardBounds, pendingInteraction = interaction) }
    }

    fun toggleOverlay() {
        viewModelScope.launch {
            val enabling = !preferencesRepository.current().overlayEnabled
            preferencesRepository.update { it.copy(overlayEnabled = enabling) }
            if (enabling && permissionStatusProvider.hasOverlayPermission()) {
                overlayController.start()
            } else {
                overlayController.stop()
            }
        }
    }

    fun hasOverlayPermission(): Boolean = permissionStatusProvider.hasOverlayPermission()

    /** A simple 0..N "how healthy is the setup" count for the dashboard's permission summary. */
    fun permissionHealth(): PermissionHealth = PermissionHealth(
        overlayGranted = permissionStatusProvider.hasOverlayPermission(),
        notificationsGranted = permissionStatusProvider.hasNotificationPermission(),
        notificationListenerGranted = permissionStatusProvider.isNotificationListenerEnabled(),
        accessibilityGranted = permissionStatusProvider.isAccessibilityServiceEnabled(),
    )
}

data class PermissionHealth(
    val overlayGranted: Boolean,
    val notificationsGranted: Boolean,
    val notificationListenerGranted: Boolean,
    val accessibilityGranted: Boolean,
) {
    val requiredCount = 2
    val requiredGrantedCount = listOf(overlayGranted, notificationsGranted).count { it }
}
