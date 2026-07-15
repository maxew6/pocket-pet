package com.pocketpet.core.domain.behavior

import com.pocketpet.core.model.PetNeeds
import com.pocketpet.core.model.PetState
import com.pocketpet.core.model.coerceToUnit

/**
 * Advances [PetNeeds] by a real elapsed-time delta. Every rate is expressed as "time for this
 * need to travel the full 0..1 range" so the numbers stay readable, and every call is a pure
 * function of `(needs, elapsedMillis, ...)` — nothing here reads a live clock, which is what
 * makes it deterministically testable from `core:domain`'s test sources.
 */
object NeedsSimulator {

    private const val HOUR_MILLIS = 60L * 60 * 1000

    /** Hunger goes from empty to starving over this many hours without feeding. */
    private const val HUNGER_FULL_CYCLE_HOURS = 6.0

    /** Energy drains fully over this many awake, non-charging hours. */
    private const val ENERGY_DRAIN_FULL_CYCLE_HOURS = 18.0

    /** Energy recovers fully over this many hours spent sleeping or charging. */
    private const val ENERGY_RECOVERY_FULL_CYCLE_HOURS = 5.0

    /** Boredom goes from none to maximum over this many hours without interaction. */
    private const val BOREDOM_FULL_CYCLE_HOURS = 3.0

    /** Affection quietly fades over this many days without any interaction at all. */
    private const val AFFECTION_FULL_DECAY_DAYS = 5.0

    /** Stress relaxes back toward baseline over this many hours once its cause clears. */
    private const val STRESS_RELAX_FULL_CYCLE_HOURS = 2.0

    /**
     * The longest gap we'll simulate in one jump. A phone left off for a week should come back
     * to "very hungry, very tired", not a naive extrapolation past that — and definitely not a
     * replay of every missed minute. Restoring from a longer gap clamps the elapsed time to this
     * ceiling before calling [applyElapsedTime].
     */
    val MAX_SIMULATED_GAP_MILLIS = 20L * HOUR_MILLIS

    fun applyElapsedTime(
        needs: PetNeeds,
        elapsedMillis: Long,
        isSleepingOrCharging: Boolean,
        batteryStressLevel: Float,
    ): PetNeeds {
        if (elapsedMillis <= 0L) return needs
        val hours = elapsedMillis.toDouble() / HOUR_MILLIS

        val hungerDelta = (hours / HUNGER_FULL_CYCLE_HOURS).toFloat()
        val energyDelta = if (isSleepingOrCharging) {
            (hours / ENERGY_RECOVERY_FULL_CYCLE_HOURS).toFloat()
        } else {
            -(hours / ENERGY_DRAIN_FULL_CYCLE_HOURS).toFloat()
        }
        val boredomDelta = (hours / BOREDOM_FULL_CYCLE_HOURS).toFloat()
        val affectionDelta = -(hours / (AFFECTION_FULL_DECAY_DAYS * 24)).toFloat()

        // Stress relaxes toward the battery-driven target rather than a flat decay, so a
        // still-low battery keeps the pet stressed instead of calming down on a timer alone.
        val stressRelaxAmount = (hours / STRESS_RELAX_FULL_CYCLE_HOURS).toFloat()
        val stressTarget = batteryStressLevel.coerceToUnit()
        val stressDelta = when {
            needs.stress > stressTarget -> -minOf(stressRelaxAmount, needs.stress - stressTarget)
            needs.stress < stressTarget -> minOf(stressRelaxAmount, stressTarget - needs.stress)
            else -> 0f
        }

        return needs.copy(
            hunger = (needs.hunger + hungerDelta).coerceToUnit(),
            energy = (needs.energy + energyDelta).coerceToUnit(),
            boredom = (needs.boredom + boredomDelta).coerceToUnit(),
            affection = (needs.affection + affectionDelta).coerceToUnit(),
            stress = (needs.stress + stressDelta).coerceToUnit(),
        )
    }

    /** Clamps a raw elapsed-time gap to [MAX_SIMULATED_GAP_MILLIS] before simulating it. */
    fun clampGap(elapsedMillis: Long): Long = elapsedMillis.coerceIn(0L, MAX_SIMULATED_GAP_MILLIS)

    /** Feeding's direct effect: hunger drops sharply, affection ticks up a little. */
    fun applyFeeding(needs: PetNeeds): PetNeeds = needs.copy(
        hunger = (needs.hunger - 0.65f).coerceToUnit(),
        affection = (needs.affection + 0.08f).coerceToUnit(),
        boredom = (needs.boredom - 0.1f).coerceToUnit(),
    )

    /** Whether [state] counts as "recovering energy" for the purposes of [applyElapsedTime]. */
    fun isRecoveringState(state: PetState): Boolean = state == PetState.Sleeping
}
