package com.pocketpet.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpet.core.domain.repository.PetPreferencesRepository
import com.pocketpet.core.model.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    preferencesRepository: PetPreferencesRepository,
) : ViewModel() {

    /** `null` while still loading — the splash screen stays up until this resolves. */
    private val _hasCompletedOnboarding = MutableStateFlow<Boolean?>(null)
    val hasCompletedOnboarding: StateFlow<Boolean?> = _hasCompletedOnboarding.asStateFlow()

    val theme: StateFlow<AppTheme> = preferencesRepository.preferences
        .map { it.theme }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppTheme.System)

    init {
        viewModelScope.launch {
            _hasCompletedOnboarding.value = preferencesRepository.preferences.first().hasCompletedOnboarding
        }
    }
}
