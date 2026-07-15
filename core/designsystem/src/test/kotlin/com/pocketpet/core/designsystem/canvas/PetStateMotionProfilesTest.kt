package com.pocketpet.core.designsystem.canvas

import com.google.common.truth.Truth.assertThat
import com.pocketpet.core.model.Mood
import com.pocketpet.core.model.PetState
import org.junit.Test

class PetStateMotionProfilesTest {

    @Test
    fun `every pet state has a defined motion profile`() {
        PetState.entries.forEach { state ->
            val profile = PetStateMotionProfiles.forState(state, Mood.Content)
            assertThat(profile.bounceFrequencyHz).isAtLeast(0f)
            assertThat(profile.squashStretchAmplitude).isAtLeast(0f)
        }
    }

    @Test
    fun `crying shows tears and a food bowl`() {
        val profile = PetStateMotionProfiles.forState(PetState.Crying, Mood.Concerned)
        assertThat(profile.showTears).isTrue()
        assertThat(profile.showFoodBowl).isTrue()
        assertThat(profile.mouth).isEqualTo(MouthExpression.Sad)
    }

    @Test
    fun `sleeping shows zzz and stays almost still`() {
        val profile = PetStateMotionProfiles.forState(PetState.Sleeping, Mood.Sleepy)
        assertThat(profile.showZzz).isTrue()
        assertThat(profile.bounceAmplitudeDp).isLessThan(1f)
    }

    @Test
    fun `running is more energetic than walking`() {
        val walking = PetStateMotionProfiles.forState(PetState.Walking, Mood.Content)
        val running = PetStateMotionProfiles.forState(PetState.Running, Mood.Excited)
        assertThat(running.bounceAmplitudeDp).isGreaterThan(walking.bounceAmplitudeDp)
        assertThat(running.bounceFrequencyHz).isGreaterThan(walking.bounceFrequencyHz)
    }

    @Test
    fun `idle mouth expression follows mood`() {
        assertThat(PetStateMotionProfiles.forState(PetState.Idle, Mood.Happy).mouth)
            .isEqualTo(MouthExpression.Happy)
        assertThat(PetStateMotionProfiles.forState(PetState.Idle, Mood.Hungry).mouth)
            .isEqualTo(MouthExpression.Sad)
        assertThat(PetStateMotionProfiles.forState(PetState.Idle, Mood.Scared).mouth)
            .isEqualTo(MouthExpression.Concerned)
    }
}
