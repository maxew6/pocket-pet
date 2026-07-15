package com.pocketpet.core.domain.testutil

import com.pocketpet.core.domain.provider.RandomProvider
import java.util.ArrayDeque

/**
 * A [RandomProvider] a test can script exactly. Queue specific values with [queueDouble] /
 * [queueInt] to hit a particular branch; with an empty queue, [nextDouble] returns `0.0` (which
 * makes [weightedPick] deterministically choose the first candidate) and [nextInt] returns
 * [from], so untouched tests still get fully deterministic — just unexciting — behavior.
 */
class FakeRandomProvider : RandomProvider {
    private val doubleQueue = ArrayDeque<Double>()
    private val intQueue = ArrayDeque<Int>()

    fun queueDouble(value: Double) = doubleQueue.addLast(value)
    fun queueInt(value: Int) = intQueue.addLast(value)

    override fun nextDouble(): Double = if (doubleQueue.isEmpty()) 0.0 else doubleQueue.removeFirst()

    override fun nextInt(from: Int, until: Int): Int =
        if (intQueue.isEmpty()) from else intQueue.removeFirst().coerceIn(from, until - 1)
}
