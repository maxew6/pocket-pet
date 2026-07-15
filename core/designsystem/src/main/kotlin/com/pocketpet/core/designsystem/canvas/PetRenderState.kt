package com.pocketpet.core.designsystem.canvas

/** Coarse mouth shape, driven by mood/state rather than drawn as a separate free-form curve. */
enum class MouthExpression {
    Neutral,
    Happy,
    Sad,
    Open,
    Concerned,
}

/**
 * Every value the [PetCanvas][com.pocketpet.core.designsystem.canvas.PetCanvas] draw call reads,
 * all on the same reusable-channel model the product brief asks for. Nothing here is Android- or
 * Compose-specific — it's plain data, produced each frame by [PetAnimationController] and
 * consumed by the drawing code, which keeps the animation *decisions* (what should the tail be
 * doing right now) separate from the animation *plumbing* (Animatable/InfiniteTransition wiring).
 */
data class PetRenderState(
    val bodyOffsetXDp: Float = 0f,
    val bodyOffsetYDp: Float = 0f,
    val bodyRotationDegrees: Float = 0f,
    val bodyScale: Float = 1f,
    val squash: Float = 1f,
    val stretch: Float = 1f,
    val eyeOpenness: Float = 1f,
    val pupilOffsetX: Float = 0f,
    val pupilOffsetY: Float = 0f,
    val earRotationDegrees: Float = 0f,
    val tailPhase: Float = 0f,
    val pawPhase: Float = 0f,
    val breathingPhase: Float = 0f,
    val mouthExpression: MouthExpression = MouthExpression.Neutral,
    val tearProgress: Float = 0f,
    val shadowScale: Float = 1f,
    val heartBurstProgress: Float = 0f,
    val confettiBurstProgress: Float = 0f,
    val showZzz: Boolean = false,
    val showFoodBowl: Boolean = false,
    val showEnergyParticles: Boolean = false,
)
