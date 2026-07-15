package com.pocketpet.core.domain.repository

import com.pocketpet.core.model.PetState

/**
 * A bounded, rolling record of recent behavior and interactions. [recentStates] is what
 * [com.pocketpet.core.domain.behavior.PetBehaviorEngine] uses to avoid repeating the same state
 * too often. The "current" last-feeding/last-interaction instant is *not* duplicated here — that
 * lives solely on [com.pocketpet.core.model.PetSnapshot] via [PetRepository], so there is exactly
 * one source of truth for "when did that last happen". This repository only ever answers
 * "what happened recently", implemented as a bounded, pruned table (see `core:database`'s DAO) —
 * intentionally not an unbounded log.
 */
interface PetMemoryRepository {
    suspend fun recordStateEntered(state: PetState, atEpochMillis: Long)

    /** The most recent states, newest first, capped at [limit]. */
    suspend fun recentStates(limit: Int = 12): List<PetState>

    suspend fun recordFeeding(atEpochMillis: Long)

    suspend fun recordInteraction(atEpochMillis: Long)

    /** Deletes history rows older than [olderThanEpochMillis] to keep the table bounded. */
    suspend fun pruneOlderThan(olderThanEpochMillis: Long)
}
