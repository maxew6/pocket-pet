package com.pocketpet.core.domain.repository

import com.pocketpet.core.model.PetPreferences
import kotlinx.coroutines.flow.Flow

/** Owns every persisted, user-controlled setting. See [PetPreferences] for the full shape. */
interface PetPreferencesRepository {
    val preferences: Flow<PetPreferences>

    suspend fun update(transform: (PetPreferences) -> PetPreferences)

    suspend fun current(): PetPreferences
}
