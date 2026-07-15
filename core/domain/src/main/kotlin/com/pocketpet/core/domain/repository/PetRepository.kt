package com.pocketpet.core.domain.repository

import com.pocketpet.core.model.PetPosition
import com.pocketpet.core.model.PetSnapshot
import com.pocketpet.core.model.SpeechBubble
import kotlinx.coroutines.flow.StateFlow

/**
 * Owns the pet's live, moment-to-moment [PetSnapshot]. This is runtime state, not settings —
 * settings live in [PetPreferencesRepository]. Implementations persist just enough of this
 * (needs, mood, position, timestamps) to restore a believable state after process death; see
 * `RestorePetStateUseCase` for the bounded catch-up logic that runs on cold start.
 */
interface PetRepository {
    /** The current snapshot, always up to date, safe to collect from Compose. */
    val snapshot: StateFlow<PetSnapshot>

    /**
     * Loads the persisted snapshot from disk into [snapshot] and returns it. Callers that need to
     * act on truly-restored state (see `RestorePetStateUseCase`) must await this *before* reading
     * or transforming [snapshot] — there is no implicit background load racing against it.
     */
    suspend fun loadPersisted(): PetSnapshot

    suspend fun updateSnapshot(transform: (PetSnapshot) -> PetSnapshot)

    suspend fun updatePosition(position: PetPosition)

    suspend fun showSpeech(bubble: SpeechBubble)

    suspend fun clearExpiredSpeech(nowEpochMillis: Long)

    /** Persists the current snapshot immediately (called on interaction and periodically). */
    suspend fun persistNow()
}
