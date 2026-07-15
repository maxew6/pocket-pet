package com.pocketpet.core.data.repository

import com.pocketpet.core.database.dao.HistoryEventDao
import com.pocketpet.core.database.entity.HistoryEventEntity
import com.pocketpet.core.domain.provider.DispatcherProvider
import com.pocketpet.core.domain.repository.PetMemoryRepository
import com.pocketpet.core.model.PetState
import kotlinx.coroutines.withContext

class PetMemoryRepositoryImpl(
    private val historyEventDao: HistoryEventDao,
    private val dispatchers: DispatcherProvider,
) : PetMemoryRepository {

    override suspend fun recordStateEntered(state: PetState, atEpochMillis: Long) = withContext(dispatchers.io) {
        historyEventDao.insert(
            HistoryEventEntity(
                kind = HistoryEventEntity.Kind.STATE_ENTERED,
                stateName = state.name,
                timestampEpochMillis = atEpochMillis,
            ),
        )
    }

    override suspend fun recentStates(limit: Int): List<PetState> = withContext(dispatchers.io) {
        historyEventDao.recentStateNames(limit).mapNotNull { it.toEnumOrNull<PetState>() }
    }

    override suspend fun recordFeeding(atEpochMillis: Long) = withContext(dispatchers.io) {
        historyEventDao.insert(
            HistoryEventEntity(
                kind = HistoryEventEntity.Kind.FEEDING,
                stateName = null,
                timestampEpochMillis = atEpochMillis,
            ),
        )
    }

    override suspend fun recordInteraction(atEpochMillis: Long) = withContext(dispatchers.io) {
        historyEventDao.insert(
            HistoryEventEntity(
                kind = HistoryEventEntity.Kind.INTERACTION,
                stateName = null,
                timestampEpochMillis = atEpochMillis,
            ),
        )
    }

    override suspend fun pruneOlderThan(olderThanEpochMillis: Long) = withContext(dispatchers.io) {
        historyEventDao.pruneOlderThan(olderThanEpochMillis)
    }
}
