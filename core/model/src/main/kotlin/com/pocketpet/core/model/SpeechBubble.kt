package com.pocketpet.core.model

/** Why a speech bubble was shown — used to rate-limit and avoid repeating the same category. */
enum class SpeechTrigger {
    Boredom,
    Hunger,
    Sleepiness,
    LowEnergyRequest,
    Discovery,
    Reunion,
    Happiness,
    BatteryReaction,
    NotificationReaction,
}

/**
 * A short, contextual speech line and when it should disappear on its own. Pocket Pet never
 * generates open-ended chatbot text — every line comes from a small, curated pool keyed by
 * [trigger] (see `SpeechLinePicker` in `core:domain`).
 */
data class SpeechBubble(
    val text: String,
    val trigger: SpeechTrigger,
    val shownAtEpochMillis: Long,
    val autoDismissAfterMillis: Long = 3_500L,
) {
    fun isExpiredAt(nowEpochMillis: Long): Boolean =
        nowEpochMillis - shownAtEpochMillis >= autoDismissAfterMillis
}
