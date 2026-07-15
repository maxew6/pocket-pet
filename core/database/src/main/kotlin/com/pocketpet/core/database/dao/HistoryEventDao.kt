package com.pocketpet.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.pocketpet.core.database.entity.HistoryEventEntity

@Dao
interface HistoryEventDao {

    @Insert
    suspend fun insert(event: HistoryEventEntity)

    @Query(
        """
        SELECT stateName FROM history_event
        WHERE kind = 'STATE_ENTERED' AND stateName IS NOT NULL
        ORDER BY timestampEpochMillis DESC
        LIMIT :limit
        """,
    )
    suspend fun recentStateNames(limit: Int): List<String>

    @Query("DELETE FROM history_event WHERE timestampEpochMillis < :olderThanEpochMillis")
    suspend fun pruneOlderThan(olderThanEpochMillis: Long)

    @Query("SELECT COUNT(*) FROM history_event")
    suspend fun count(): Int
}
