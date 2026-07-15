package com.pocketpet.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pocketpet.core.database.entity.PetStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PetStateDao {

    @Upsert
    suspend fun upsert(entity: PetStateEntity)

    @Query("SELECT * FROM pet_state WHERE id = :id LIMIT 1")
    fun observe(id: Int = PetStateEntity.SINGLETON_ID): Flow<PetStateEntity?>

    @Query("SELECT * FROM pet_state WHERE id = :id LIMIT 1")
    suspend fun get(id: Int = PetStateEntity.SINGLETON_ID): PetStateEntity?
}
