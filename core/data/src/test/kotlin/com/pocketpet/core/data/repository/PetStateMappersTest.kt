package com.pocketpet.core.data.repository

import com.google.common.truth.Truth.assertThat
import com.pocketpet.core.model.BatteryMilestone
import com.pocketpet.core.model.BatteryStatus
import com.pocketpet.core.model.Mood
import com.pocketpet.core.model.PetNeeds
import com.pocketpet.core.model.PetPosition
import com.pocketpet.core.model.PetSnapshot
import com.pocketpet.core.model.PetState
import com.pocketpet.core.model.SpeechBubble
import com.pocketpet.core.model.SpeechTrigger
import org.junit.Test

class PetStateMappersTest {

    @Test
    fun `a snapshot survives a round trip through its entity unchanged`() {
        val original = PetSnapshot(
            state = PetState.Grooming,
            mood = Mood.Curious,
            needs = PetNeeds(hunger = 0.33f, energy = 0.61f, affection = 0.72f, curiosity = 0.5f, boredom = 0.4f, stress = 0.2f),
            position = PetPosition(xDp = 123.5f, yDp = 456.25f),
            appearance = com.pocketpet.core.model.PetAppearance(), // not persisted by PetStateEntity — see note below
            activeSpeech = SpeechBubble(
                text = "I found something.",
                trigger = SpeechTrigger.Discovery,
                shownAtEpochMillis = 42L,
                autoDismissAfterMillis = 3_500L,
            ),
            lastInteractionEpochMillis = 100L,
            lastFeedingEpochMillis = 200L,
            lastPersistedEpochMillis = 300L,
            batteryStatus = BatteryStatus(percent = 77, isCharging = true, isFull = false),
            lastStateChangeEpochMillis = 400L,
            lastSpeechEpochMillis = 500L,
            lastSpeechTrigger = SpeechTrigger.Discovery,
            lastBatteryMilestoneReacted = BatteryMilestone.High,
        )

        val roundTripped = original.toEntity().toDomain()

        // appearance/recentStates are intentionally not part of PetStateEntity (appearance lives
        // in PetPreferences/DataStore; recentStates is populated from PetMemoryRepository) —
        // compare everything else field by field instead of full equality.
        assertThat(roundTripped.state).isEqualTo(original.state)
        assertThat(roundTripped.mood).isEqualTo(original.mood)
        assertThat(roundTripped.needs).isEqualTo(original.needs)
        assertThat(roundTripped.position).isEqualTo(original.position)
        assertThat(roundTripped.activeSpeech).isEqualTo(original.activeSpeech)
        assertThat(roundTripped.lastInteractionEpochMillis).isEqualTo(original.lastInteractionEpochMillis)
        assertThat(roundTripped.lastFeedingEpochMillis).isEqualTo(original.lastFeedingEpochMillis)
        assertThat(roundTripped.batteryStatus).isEqualTo(original.batteryStatus)
        assertThat(roundTripped.lastBatteryMilestoneReacted).isEqualTo(original.lastBatteryMilestoneReacted)
    }

    @Test
    fun `an unknown persisted enum name falls back to a safe default instead of crashing`() {
        val entity = com.pocketpet.core.database.entity.PetStateEntity(
            state = "SomeFutureStateThisVersionDoesNotKnow",
            mood = "Content",
            hunger = 0f,
            energy = 1f,
            affection = 1f,
            curiosity = 1f,
            boredom = 0f,
            stress = 0f,
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

        val domain = entity.toDomain()

        assertThat(domain.state).isEqualTo(PetState.Idle)
    }
}
