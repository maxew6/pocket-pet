package com.pocketpet.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpet.core.domain.repository.PetPreferencesRepository
import com.pocketpet.core.model.Accessory
import com.pocketpet.core.model.ColorTone
import com.pocketpet.core.model.PetPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomizationViewModel @Inject constructor(
    private val preferencesRepository: PetPreferencesRepository,
) : ViewModel() {

    val preferences: StateFlow<PetPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetPreferences())

    fun setName(name: String) = update { it.copy(appearance = it.appearance.copy(name = name.take(24))) }
    fun setColorTone(tone: ColorTone) = update { it.copy(appearance = it.appearance.copy(colorTone = tone)) }
    fun setAccessory(accessory: Accessory) = update { it.copy(appearance = it.appearance.copy(accessory = accessory)) }
    fun setScale(scale: Float) = update { it.copy(appearance = it.appearance.copy(scale = scale)) }
    fun setOpacity(opacity: Float) = update { it.copy(appearance = it.appearance.copy(opacity = opacity)) }
    fun setAnimationSpeed(speed: Float) =
        update { it.copy(appearance = it.appearance.copy(animationSpeedMultiplier = speed)) }
    fun setReducedMotion(enabled: Boolean) = update { it.copy(appearance = it.appearance.copy(reducedMotion = enabled)) }

    private fun update(transform: (PetPreferences) -> PetPreferences) {
        viewModelScope.launch { preferencesRepository.update(transform) }
    }
}
