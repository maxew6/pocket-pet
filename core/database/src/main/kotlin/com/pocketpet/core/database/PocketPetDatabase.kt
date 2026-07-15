package com.pocketpet.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pocketpet.core.database.dao.HistoryEventDao
import com.pocketpet.core.database.dao.PetStateDao
import com.pocketpet.core.database.entity.HistoryEventEntity
import com.pocketpet.core.database.entity.PetStateEntity

/**
 * Pocket Pet's local database: the live pet state (one row) and a bounded history log. There are
 * no migrations yet because there is only ever one shipped schema version so far — `version = 1`
 * is the original schema, not a placeholder. When a future release changes either entity, add a
 * real `Migration(1, 2) { ... }` to [DATABASE_MIGRATIONS] rather than falling back to destructive
 * migration, so nobody's pet loses its history across an app update.
 */
@Database(
    entities = [PetStateEntity::class, HistoryEventEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PocketPetDatabase : RoomDatabase() {
    abstract fun petStateDao(): PetStateDao
    abstract fun historyEventDao(): HistoryEventDao

    companion object {
        const val DATABASE_NAME = "pocket_pet.db"

        /** Real migrations get appended here as the schema evolves past version 1. */
        val DATABASE_MIGRATIONS: Array<androidx.room.migration.Migration> = emptyArray()
    }
}
