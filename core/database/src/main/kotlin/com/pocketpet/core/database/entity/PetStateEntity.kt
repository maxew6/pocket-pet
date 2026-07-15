package com.pocketpet.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The pet's live snapshot, persisted as a single row (fixed [id] = 0, always upserted). Room is
 * used here — rather than DataStore — because this row changes at a much higher frequency than
 * settings do, and because `core:data`'s repository needs simple, atomic single-row read/replace
 * semantics that map directly onto a Room `@Upsert`/`@Query` pair.
 */
@Entity(tableName = "pet_state")
data class PetStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val state: String,
    val mood: String,
    val hunger: Float,
    val energy: Float,
    val affection: Float,
    val curiosity: Float,
    val boredom: Float,
    val stress: Float,
    val positionXDp: Float,
    val positionYDp: Float,
    val activeSpeechText: String?,
    val activeSpeechTrigger: String?,
    val activeSpeechShownAtEpochMillis: Long?,
    val activeSpeechAutoDismissAfterMillis: Long?,
    val lastInteractionEpochMillis: Long,
    val lastFeedingEpochMillis: Long,
    val lastPersistedEpochMillis: Long,
    val lastStateChangeEpochMillis: Long,
    val lastSpeechEpochMillis: Long,
    val lastSpeechTrigger: String?,
    val lastBatteryMilestoneReacted: String?,
    val batteryPercent: Int,
    val batteryIsCharging: Boolean,
    val batteryIsFull: Boolean,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
