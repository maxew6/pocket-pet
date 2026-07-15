package com.pocketpet.core.designsystem.canvas

import com.pocketpet.core.model.Mood
import com.pocketpet.core.model.PetState

/**
 * The "target" shape of a state's motion: how much it bounces, whether it leans into a squash-
 * and-stretch pose, what the mouth does, and which overlay effects (tears, zzz, food bowl) are
 * active. [PetAnimationController] eases the live [PetRenderState] channels toward these targets
 * rather than snapping — this is the data half of "reusable animation channels", kept as a plain
 * function so it can be unit-tested without touching Compose.
 */
data class MotionProfile(
    val bounceAmplitudeDp: Float,
    val bounceFrequencyHz: Float,
    val baseRotationDegrees: Float,
    val squashStretchAmplitude: Float,
    val tailSwaySpeed: Float,
    val pawActivity: Float,
    val mouth: MouthExpression,
    val showTears: Boolean,
    val showZzz: Boolean,
    val showFoodBowl: Boolean,
)

object PetStateMotionProfiles {

    fun forState(state: PetState, mood: Mood): MotionProfile = when (state) {
        PetState.Idle -> calm(mouth = neutralMouthFor(mood))
        PetState.Walking -> MotionProfile(3f, 1.6f, 0f, 0.06f, 1.4f, 0.8f, neutralMouthFor(mood), false, false, false)
        PetState.Running -> MotionProfile(6f, 3.2f, 0f, 0.14f, 2.4f, 1.6f, MouthExpression.Open, false, false, false)
        PetState.Sleeping -> MotionProfile(0.6f, 0.25f, 0f, 0.02f, 0.1f, 0f, MouthExpression.Neutral, false, true, false)
        PetState.Stretching -> MotionProfile(1f, 0.4f, 0f, 0.35f, 0.3f, 0.2f, MouthExpression.Open, false, false, false)
        PetState.Jumping -> MotionProfile(0f, 0f, 0f, 0.3f, 1.8f, 0.4f, MouthExpression.Happy, false, false, false)
        PetState.LookingAround -> calm(mouth = MouthExpression.Neutral).copy(pawActivity = 0f)
        PetState.FollowingFinger -> MotionProfile(2f, 1.2f, 0f, 0.05f, 1.6f, 1f, MouthExpression.Happy, false, false, false)
        PetState.BeingDragged -> MotionProfile(0f, 0f, 0f, 0.08f, 2.2f, 0f, MouthExpression.Concerned, false, false, false)
        PetState.Sitting -> calm(mouth = neutralMouthFor(mood)).copy(pawActivity = 0.15f)
        PetState.Rolling -> MotionProfile(0f, 0f, 360f, 0.1f, 0.5f, 0.3f, MouthExpression.Happy, false, false, false)
        PetState.Grooming -> MotionProfile(0.5f, 0.5f, 0f, 0.05f, 0.4f, 1.2f, MouthExpression.Neutral, false, false, false)
        PetState.Yawning -> MotionProfile(0.4f, 0.3f, 0f, 0.25f, 0.2f, 0f, MouthExpression.Open, false, false, false)
        PetState.Eating -> MotionProfile(1.5f, 2f, 0f, 0.12f, 0.3f, 0.6f, MouthExpression.Open, false, false, true)
        PetState.Crying -> MotionProfile(0.8f, 0.8f, 0f, 0.08f, 0.1f, 0f, MouthExpression.Sad, true, false, true)
        PetState.Hiding -> MotionProfile(0.3f, 0.4f, 0f, 0.15f, 0f, 0f, MouthExpression.Concerned, false, false, false)
        PetState.HappyDance -> MotionProfile(5f, 4f, 8f, 0.2f, 3f, 1.6f, MouthExpression.Happy, false, false, false)
        PetState.WatchingNotification -> MotionProfile(0.5f, 0.6f, 0f, 0.05f, 0.6f, 0f, MouthExpression.Neutral, false, false, false)
        PetState.WatchingCharging -> MotionProfile(0.6f, 0.5f, 0f, 0.06f, 0.5f, 0.2f, MouthExpression.Happy, false, false, false)
        PetState.Recovering -> MotionProfile(0.4f, 0.4f, 0f, 0.06f, 0.2f, 0f, MouthExpression.Concerned, false, false, false)
    }

    private fun calm(mouth: MouthExpression) = MotionProfile(
        bounceAmplitudeDp = 1f,
        bounceFrequencyHz = 0.5f,
        baseRotationDegrees = 0f,
        squashStretchAmplitude = 0.03f,
        tailSwaySpeed = 0.8f,
        pawActivity = 0f,
        mouth = mouth,
        showTears = false,
        showZzz = false,
        showFoodBowl = false,
    )

    private fun neutralMouthFor(mood: Mood): MouthExpression = when (mood) {
        Mood.Happy, Mood.Excited, Mood.Content -> MouthExpression.Happy
        Mood.Hungry, Mood.Lonely -> MouthExpression.Sad
        Mood.Concerned, Mood.Scared -> MouthExpression.Concerned
        else -> MouthExpression.Neutral
    }
}
