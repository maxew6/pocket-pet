package com.pocketpet.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpet.core.domain.repository.PermissionStatusProvider
import com.pocketpet.core.domain.repository.PetPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PermissionsUiState(
    val hasOverlayPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
)

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val permissionStatusProvider: PermissionStatusProvider,
    private val preferencesRepository: PetPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionsUiState())
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** Re-reads every permission from the OS. Call this from `onResume` — a person may have just
     *  come back from the system Settings screen this composable sent them to. */
    fun refresh() {
        _uiState.update {
            it.copy(
                hasOverlayPermission = permissionStatusProvider.hasOverlayPermission(),
                hasNotificationPermission = permissionStatusProvider.hasNotificationPermission(),
            )
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesRepository.update { prefs -> prefs.copy(hasCompletedOnboarding = true) }
        }
    }
}
