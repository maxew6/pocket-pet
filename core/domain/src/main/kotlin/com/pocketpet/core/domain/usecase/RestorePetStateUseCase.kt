package com.pocketpet.core.domain.usecase

import com.pocketpet.core.domain.behavior.PetBehaviorEngine
import com.pocketpet.core.domain.repository.PetMemoryRepository
import com.pocketpet.core.domain.repository.PetRepository

/**
 * Runs once when the overlay service (re)starts: applies a bounded, elapsed-time need catch-up
 * to whatever was last persisted, instead of resuming as if no time had passed. See
 * [PetBehaviorEngine.restoreAfterGap] for the bounding logic itself.
 */
class RestorePetStateUseCase(
    private val engine: PetBehaviorEngine,
    private val petRepository: PetRepository,
    private val memoryRepository: PetMemoryRepository,
) {
    suspend operator fun invoke() {
        val recentStates = memoryRepository.recentStates()
        val persisted = petRepository.loadPersisted()
        petRepository.updateSnapshot {
            engine.restoreAfterGap(persisted).copy(recentStates = recentStates)
        }
    }
}
