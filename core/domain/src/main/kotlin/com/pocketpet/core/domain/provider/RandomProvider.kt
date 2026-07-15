package com.pocketpet.core.domain.provider

import kotlin.random.Random

/**
 * The only way any domain code draws randomness. Production code injects [DefaultRandomProvider];
 * tests inject a seeded or scripted provider so a "random" behavior pick can be asserted exactly.
 */
interface RandomProvider {
    /** A uniform double in `[0.0, 1.0)`, the building block every weighted pick derives from. */
    fun nextDouble(): Double

    /** A uniform int in `[from, until)`. */
    fun nextInt(from: Int, until: Int): Int

    /**
     * Picks one entry from [weighted] (item to non-negative weight) proportionally to its
     * weight. Returns `null` only if [weighted] is empty or every weight is zero.
     */
    fun <T> weightedPick(weighted: List<Pair<T, Double>>): T? {
        val total = weighted.sumOf { it.second }
        if (total <= 0.0) return null
        var roll = nextDouble() * total
        for ((item, weight) in weighted) {
            roll -= weight
            if (roll <= 0.0) return item
        }
        return weighted.lastOrNull()?.first
    }
}

/** Real implementation backed by [kotlin.random.Random]'s default, non-seeded source. */
class DefaultRandomProvider(private val random: Random = Random.Default) : RandomProvider {
    override fun nextDouble(): Double = random.nextDouble()
    override fun nextInt(from: Int, until: Int): Int = random.nextInt(from, until)
}
