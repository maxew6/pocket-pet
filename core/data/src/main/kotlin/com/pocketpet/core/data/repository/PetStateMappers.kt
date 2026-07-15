package com.pocketpet.core.data.repository

import com.pocketpet.core.database.entity.PetStateEntity
import com.pocketpet.core.model.BatteryMilestone
import com.pocketpet.core.model.BatteryStatus
import com.pocketpet.core.model.Mood
import com.pocketpet.core.model.PetNeeds
import com.pocketpet.core.model.PetPosition
import com.pocketpet.core.model.PetSnapshot
import com.pocketpet.core.model.PetState
import com.pocketpet.core.model.SpeechBubble
import com.pocketpet.core.model.SpeechTrigger

internal inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
    enumValues<T>().firstOrNull { it.name == this }

internal inline fun <reified T : Enum<T>> String.toEnumOrDefault(default: T): T = toEnumOrNull<T>() ?: default

fun PetStateEntity.toDomain(): PetSnapshot = PetSnapshot(
    state = state.toEnumOrDefault(PetState.Idle),
    mood = mood.toEnumOrDefault(Mood.Content),
    needs = PetNeeds(
        hunger = hunger,
        energy = energy,
        affection = affection,
        curiosity = curiosity,
        boredom = boredom,
        stress = stress,
    ),
    position = PetPosition(xDp = positionXDp, yDp = positionYDp),
    activeSpeech = activeSpeechText?.let { text ->
        SpeechBubble(
            text = text,
            trigger = activeSpeechTrigger?.toEnumOrDefault(SpeechTrigger.Happiness) ?: SpeechTrigger.Happiness,
            shownAtEpochMillis = activeSpeechShownAtEpochMillis ?: 0L,
            autoDismissAfterMillis = activeSpeechAutoDismissAfterMillis ?: 3_500L,
        )
    },
    lastInteractionEpochMillis = lastInteractionEpochMillis,
    lastFeedingEpochMillis = lastFeedingEpochMillis,
    lastPersistedEpochMillis = lastPersistedEpochMillis,
    batteryStatus = BatteryStatus(percent = batteryPercent, isCharging = batteryIsCharging, isFull = batteryIsFull),
    lastStateChangeEpochMillis = lastStateChangeEpochMillis,
    lastSpeechEpochMillis = lastSpeechEpochMillis,
    lastSpeechTrigger = lastSpeechTrigger?.toEnumOrNull<SpeechTrigger>(),
    lastBatteryMilestoneReacted = lastBatteryMilestoneReacted?.toEnumOrNull<BatteryMilestone>(),
)

fun PetSnapshot.toEntity(): PetStateEntity = PetStateEntity(
    state = state.name,
    mood = mood.name,
    hunger = needs.hunger,
    energy = needs.energy,
    affection = needs.affection,
    curiosity = needs.curiosity,
    boredom = needs.boredom,
    stress = needs.stress,
    positionXDp = position.xDp,
    positionYDp = position.yDp,
    activeSpeechText = activeSpeech?.text,
    activeSpeechTrigger = activeSpeech?.trigger?.name,
    activeSpeechShownAtEpochMillis = activeSpeech?.shownAtEpochMillis,
    activeSpeechAutoDismissAfterMillis = activeSpeech?.autoDismissAfterMillis,
    lastInteractionEpochMillis = lastInteractionEpochMillis,
    lastFeedingEpochMillis = lastFeedingEpochMillis,
    lastPersistedEpochMillis = lastPersistedEpochMillis,
    lastStateChangeEpochMillis = lastStateChangeEpochMillis,
    lastSpeechEpochMillis = lastSpeechEpochMillis,
    lastSpeechTrigger = lastSpeechTrigger?.name,
    lastBatteryMilestoneReacted = lastBatteryMilestoneReacted?.name,
    batteryPercent = batteryStatus.percent,
    batteryIsCharging = batteryStatus.isCharging,
    batteryIsFull = batteryStatus.isFull,
)
