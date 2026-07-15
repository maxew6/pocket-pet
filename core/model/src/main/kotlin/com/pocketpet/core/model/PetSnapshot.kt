package com.pocketpet.core.model

/**
 * The pet's complete, immutable, "right now" state — the single object the overlay `Canvas` and
 * the home dashboard both render. `PetBehaviorEngine` is the only thing that produces new
 * instances of this; everything downstream is a pure function of it.
 */
data class PetSnapshot(
    val state: PetState = PetState.Idle,
    val mood: Mood = Mood.Content,
    val needs: PetNeeds = PetNeeds.Fresh,
    val position: PetPosition = PetPosition(xDp = 0f, yDp = 0f),
    val appearance: PetAppearance = PetAppearance(),
    val activeSpeech: SpeechBubble? = null,
    val lastInteractionEpochMillis: Long = 0L,
    val lastFeedingEpochMillis: Long = 0L,
    val lastPersistedEpochMillis: Long = 0L,
    val batteryStatus: BatteryStatus = BatteryStatus.Unknown,
    val recentStates: List<PetState> = emptyList(),
    val lastStateChangeEpochMillis: Long = 0L,
    val lastSpeechEpochMillis: Long = 0L,
    val lastSpeechTrigger: SpeechTrigger? = null,
    val lastBatteryMilestoneReacted: BatteryMilestone? = null,
)
