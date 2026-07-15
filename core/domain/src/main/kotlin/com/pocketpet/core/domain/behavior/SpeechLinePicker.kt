package com.pocketpet.core.domain.behavior

import com.pocketpet.core.domain.provider.RandomProvider
import com.pocketpet.core.model.SpeechTrigger

/**
 * Pocket Pet never generates open-ended text. Every line comes from one of these small, curated
 * pools, keyed by why the line is being shown. [pick] avoids repeating the exact line that was
 * shown last time for the same trigger, when an alternative exists.
 */
object SpeechLinePicker {

    private val pools: Map<SpeechTrigger, List<String>> = mapOf(
        SpeechTrigger.Boredom to listOf("I'm bored…", "Let's explore!"),
        SpeechTrigger.Hunger to listOf("Feed me \uD83E\uDD7A", "Please feed me…"),
        SpeechTrigger.Sleepiness to listOf("I'm sleepy…"),
        SpeechTrigger.LowEnergyRequest to listOf("We need energy!", "Please feed me…"),
        SpeechTrigger.Discovery to listOf("I found something.", "Ooh, what's that?"),
        SpeechTrigger.Reunion to listOf("I missed you."),
        SpeechTrigger.Happiness to listOf("I'm happy!"),
        SpeechTrigger.BatteryReaction to listOf("We need energy!", "Please feed me…"),
        SpeechTrigger.NotificationReaction to listOf("I found something."),
    )

    fun pick(trigger: SpeechTrigger, random: RandomProvider, avoiding: String? = null): String {
        val candidates = pools[trigger].orEmpty().ifEmpty { return "" }
        if (candidates.size == 1) return candidates.first()
        val filtered = candidates.filterNot { it == avoiding }.ifEmpty { candidates }
        val index = random.nextInt(0, filtered.size)
        return filtered[index]
    }
}
