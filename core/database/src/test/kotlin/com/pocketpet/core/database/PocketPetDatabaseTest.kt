package com.pocketpet.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.pocketpet.core.database.entity.HistoryEventEntity
import com.pocketpet.core.database.entity.PetStateEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PocketPetDatabaseTest {

    private lateinit var database: PocketPetDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PocketPetDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `upserting the singleton pet state row can be read back`() = runTest {
        val dao = database.petStateDao()
        val entity = sampleState(hunger = 0.42f)

        dao.upsert(entity)
        val loaded = dao.get()

        assertThat(loaded).isEqualTo(entity)
    }

    @Test
    fun `upserting twice replaces the single row rather than inserting a second one`() = runTest {
        val dao = database.petStateDao()
        dao.upsert(sampleState(hunger = 0.1f))
        dao.upsert(sampleState(hunger = 0.9f))

        val loaded = dao.get()

        assertThat(loaded?.hunger).isEqualTo(0.9f)
    }

    @Test
    fun `history events return newest state-entered rows first, capped at the limit`() = runTest {
        val dao = database.historyEventDao()
        listOf("Idle", "Walking", "Sitting", "Jumping").forEachIndexed { index, state ->
            dao.insert(
                HistoryEventEntity(
                    kind = HistoryEventEntity.Kind.STATE_ENTERED,
                    stateName = state,
                    timestampEpochMillis = 1_000L * index,
                ),
            )
        }

        val recent = dao.recentStateNames(limit = 2)

        assertThat(recent).containsExactly("Jumping", "Sitting").inOrder()
    }

    @Test
    fun `pruning removes only rows older than the cutoff`() = runTest {
        val dao = database.historyEventDao()
        dao.insert(HistoryEventEntity(kind = HistoryEventEntity.Kind.INTERACTION, stateName = null, timestampEpochMillis = 1_000L))
        dao.insert(HistoryEventEntity(kind = HistoryEventEntity.Kind.INTERACTION, stateName = null, timestampEpochMillis = 5_000L))

        dao.pruneOlderThan(3_000L)

        assertThat(dao.count()).isEqualTo(1)
    }

    private fun sampleState(hunger: Float) = PetStateEntity(
        state = "Idle",
        mood = "Content",
        hunger = hunger,
        energy = 0.8f,
        affection = 0.7f,
        curiosity = 0.5f,
        boredom = 0.2f,
        stress = 0.1f,
        positionXDp = 0f,
        positionYDp = 0f,
        activeSpeechText = null,
        activeSpeechTrigger = null,
        activeSpeechShownAtEpochMillis = null,
        activeSpeechAutoDismissAfterMillis = null,
        lastInteractionEpochMillis = 0L,
        lastFeedingEpochMillis = 0L,
        lastPersistedEpochMillis = 0L,
        lastStateChangeEpochMillis = 0L,
        lastSpeechEpochMillis = 0L,
        lastSpeechTrigger = null,
        lastBatteryMilestoneReacted = null,
        batteryPercent = 50,
        batteryIsCharging = false,
        batteryIsFull = false,
    )
}
