package com.pocketpet.core.data.repository

import com.pocketpet.core.database.dao.PetStateDao
import com.pocketpet.core.domain.provider.DispatcherProvider
import com.pocketpet.core.domain.repository.PetRepository
import com.pocketpet.core.model.PetPosition
import com.pocketpet.core.model.PetSnapshot
import com.pocketpet.core.model.SpeechBubble
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The in-memory [snapshot] `StateFlow` is the fast path every frame reads from; Room is the
 * durable backing store. There is no implicit background load racing anything: [loadPersisted]
 * is the one, explicit, awaitable way the initial disk state reaches [snapshot] — see
 * `RestorePetStateUseCase`, which awaits it before applying the elapsed-time catch-up, so a slow
 * disk read can never be clobbered by a "restore" that ran against a still-default in-memory
 * snapshot.
 *
 * [updateSnapshot] (discrete, low-frequency events — a tick, a feed, a battery change) always
 * writes through immediately. [updatePosition] (up to 60x/second during a drag) only updates
 * memory — [persistNow] is what flushes a fresh position to disk, called by the overlay service
 * on drag-end and periodically, so a live drag never turns into a storm of disk writes.
 */
class PetRepositoryImpl(
    private val petStateDao: PetStateDao,
    private val dispatchers: DispatcherProvider,
) : PetRepository {

    private val _snapshot = MutableStateFlow(PetSnapshot())
    override val snapshot: StateFlow<PetSnapshot> = _snapshot.asStateFlow()

    private val writeMutex = Mutex()

    override suspend fun loadPersisted(): PetSnapshot = writeMutex.withLock {
        val loaded = withContext(dispatchers.io) { petStateDao.get()?.toDomain() } ?: PetSnapshot()
        _snapshot.value = loaded
        loaded
    }

    override suspend fun updateSnapshot(transform: (PetSnapshot) -> PetSnapshot) {
        writeMutex.withLock {
            val updated = transform(_snapshot.value)
            _snapshot.value = updated
            withContext(dispatchers.io) { petStateDao.upsert(updated.toEntity()) }
        }
    }

    override suspend fun updatePosition(position: PetPosition) {
        writeMutex.withLock {
            _snapshot.value = _snapshot.value.copy(position = position)
        }
    }

    override suspend fun showSpeech(bubble: SpeechBubble) {
        writeMutex.withLock {
            _snapshot.value = _snapshot.value.copy(
                activeSpeech = bubble,
                lastSpeechEpochMillis = bubble.shownAtEpochMillis,
                lastSpeechTrigger = bubble.trigger,
            )
        }
    }

    override suspend fun clearExpiredSpeech(nowEpochMillis: Long) {
        writeMutex.withLock {
            val current = _snapshot.value
            if (current.activeSpeech?.isExpiredAt(nowEpochMillis) == true) {
                _snapshot.value = current.copy(activeSpeech = null)
            }
        }
    }

    override suspend fun persistNow() {
        writeMutex.withLock {
            withContext(dispatchers.io) { petStateDao.upsert(_snapshot.value.toEntity()) }
        }
    }
}
