package com.pocketpet.core.domain.behavior

import com.google.common.truth.Truth.assertThat
import com.pocketpet.core.model.PetNeeds
import com.pocketpet.core.model.PetState
import org.junit.Test

class NeedsSimulatorTest {

    @Test
    fun `hunger increases with elapsed time when not fed`() {
        val start = PetNeeds(hunger = 0.1f)
        val threeHoursMillis = 3L * 60 * 60 * 1000

        val result = NeedsSimulator.applyElapsedTime(
            needs = start,
            elapsedMillis = threeHoursMillis,
            isSleepingOrCharging = false,
            batteryStressLevel = 0f,
        )

        assertThat(result.hunger).isGreaterThan(start.hunger)
    }

    @Test
    fun `hunger never exceeds 1_0 even with a very long gap`() {
        val start = PetNeeds(hunger = 0.9f)
        val hundredHoursMillis = 100L * 60 * 60 * 1000

        val result = NeedsSimulator.applyElapsedTime(
            needs = start,
            elapsedMillis = hundredHoursMillis,
            isSleepingOrCharging = false,
            batteryStressLevel = 0f,
        )

        assertThat(result.hunger).isEqualTo(1.0f)
    }

    @Test
    fun `energy drains while awake and recovers while sleeping or charging`() {
        val start = PetNeeds(energy = 0.5f)
        val oneHourMillis = 60L * 60 * 1000

        val drained = NeedsSimulator.applyElapsedTime(start, oneHourMillis, isSleepingOrCharging = false, 0f)
        val recovered = NeedsSimulator.applyElapsedTime(start, oneHourMillis, isSleepingOrCharging = true, 0f)

        assertThat(drained.energy).isLessThan(start.energy)
        assertThat(recovered.energy).isGreaterThan(start.energy)
    }

    @Test
    fun `zero elapsed time changes nothing`() {
        val start = PetNeeds()
        val result = NeedsSimulator.applyElapsedTime(start, 0L, isSleepingOrCharging = false, 0f)
        assertThat(result).isEqualTo(start)
    }

    @Test
    fun `feeding sharply reduces hunger and nudges affection up`() {
        val hungry = PetNeeds(hunger = 0.9f, affection = 0.5f, boredom = 0.6f)

        val fed = NeedsSimulator.applyFeeding(hungry)

        assertThat(fed.hunger).isLessThan(hungry.hunger)
        assertThat(fed.affection).isGreaterThan(hungry.affection)
        assertThat(fed.boredom).isLessThan(hungry.boredom)
    }

    @Test
    fun `clampGap caps an extreme gap at the maximum simulated gap`() {
        val oneWeekMillis = 7L * 24 * 60 * 60 * 1000

        val clamped = NeedsSimulator.clampGap(oneWeekMillis)

        assertThat(clamped).isEqualTo(NeedsSimulator.MAX_SIMULATED_GAP_MILLIS)
    }

    @Test
    fun `clampGap leaves a small gap untouched`() {
        val tenMinutesMillis = 10L * 60 * 1000
        assertThat(NeedsSimulator.clampGap(tenMinutesMillis)).isEqualTo(tenMinutesMillis)
    }

    @Test
    fun `sleeping is the only state considered recovering`() {
        assertThat(NeedsSimulator.isRecoveringState(PetState.Sleeping)).isTrue()
        assertThat(NeedsSimulator.isRecoveringState(PetState.Idle)).isFalse()
        assertThat(NeedsSimulator.isRecoveringState(PetState.Running)).isFalse()
    }
}
