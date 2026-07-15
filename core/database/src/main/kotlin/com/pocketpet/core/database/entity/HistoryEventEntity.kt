package com.pocketpet.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per notable event: a state entered, a direct interaction, or a feeding. A single table
 * with a [kind] discriminator keeps the schema simple; [PetMemoryRepository][com.pocketpet.core.domain.repository.PetMemoryRepository]'s
 * implementation filters by kind. Rows are pruned by [PruneOlderThan] queries — this is
 * deliberately not an unbounded log.
 */
@Entity(tableName = "history_event")
data class HistoryEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    /** Only populated for [Kind.STATE_ENTERED] rows. */
    val stateName: String?,
    val timestampEpochMillis: Long,
) {
    object Kind {
        const val STATE_ENTERED = "STATE_ENTERED"
        const val INTERACTION = "INTERACTION"
        const val FEEDING = "FEEDING"
    }
}
