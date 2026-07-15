package com.pocketpet.core.model

/**
 * The pet's internal drives, each normalized to `[0.0, 1.0]`.
 *
 * For [hunger], [boredom], and [stress], 1.0 is the "worst" extreme (starving, very bored, very
 * stressed). For [energy], [affection], and [curiosity], 1.0 is the "best" extreme (fully
 * rested, well loved, highly engaged). Keeping every field on the same 0..1 scale, regardless of
 * which direction is "good", is what lets `PetBehaviorEngine` combine them with simple weighted
 * arithmetic instead of a pile of per-field special cases.
 */
data class PetNeeds(
    val hunger: Float = 0.2f,
    val energy: Float = 0.8f,
    val affection: Float = 0.7f,
    val curiosity: Float = 0.5f,
    val boredom: Float = 0.2f,
    val stress: Float = 0.1f,
) {
    init {
        require(hunger in 0f..1f) { "hunger must be in [0,1], was $hunger" }
        require(energy in 0f..1f) { "energy must be in [0,1], was $energy" }
        require(affection in 0f..1f) { "affection must be in [0,1], was $affection" }
        require(curiosity in 0f..1f) { "curiosity must be in [0,1], was $curiosity" }
        require(boredom in 0f..1f) { "boredom must be in [0,1], was $boredom" }
        require(stress in 0f..1f) { "stress must be in [0,1], was $stress" }
    }

    companion object {
        /** A freshly created pet: fed, rested, and calm. */
        val Fresh = PetNeeds(
            hunger = 0.1f,
            energy = 0.9f,
            affection = 0.6f,
            curiosity = 0.5f,
            boredom = 0.1f,
            stress = 0.05f,
        )
    }
}

/** Clamps a need value into the valid `[0,1]` range, used whenever a need is nudged up/down. */
fun Float.coerceToUnit(): Float = coerceIn(0f, 1f)
