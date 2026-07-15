package com.pocketpet.core.domain.usecase

import com.pocketpet.core.domain.behavior.BehaviorContext
import com.pocketpet.core.domain.behavior.BehaviorDecision
import com.pocketpet.core.domain.behavior.PetBehaviorEngine
import com.pocketpet.core.domain.behavior.UserInteractionType
import com.pocketpet.core.domain.provider.ClockProvider
import com.pocketpet.core.domain.repository.PetMemoryRepository
import com.pocketpet.core.domain.repository.PetPreferencesRepository
import com.pocketpet.core.domain.repository.PetRepository
import com.pocketpet.core.domain.repository.SystemStatusRepository
import com.pocketpet.core.model.NotificationEvent
import com.pocketpet.core.model.ScreenBounds
import kotlinx.coroutines.flow.first

/**
 * The single entry point every trigger funnels through — the ambient low-frequency ticker, a
 * tap/drag/fling gesture, a feed/play button, a notification event, or a battery broadcast. It
 * gathers the current world state, asks [PetBehaviorEngine] for one decision, and persists the
 * result. Keeping this the *only* place that writes [PetRepository] and [PetMemoryRepository]
 * after startup is what keeps the state machine's invariants (cooldowns, milestone tracking)
 * intact no matter which UI entry point triggered the tick.
 */
class ProcessBehaviorTickUseCase(
    private val engine: PetBehaviorEngine,
    private val petRepository: PetRepository,
    private val preferencesRepository: PetPreferencesRepository,
    private val systemStatusRepository: SystemStatusRepository,
    private val memoryRepository: PetMemoryRepository,
    private val clock: ClockProvider,
) {
    suspend operator fun invoke(
        screenBounds: ScreenBounds,
        pendingInteraction: UserInteractionType? = null,
        pendingNotification: NotificationEvent? = null,
    ): BehaviorDecision {
        val prefs = preferencesRepository.current()
        val battery = systemStatusRepository.batteryStatus.first()
        val baseSnapshot = petRepository.snapshot.value
        val snapshotWithHistory = baseSnapshot.copy(recentStates = memoryRepository.recentStates())

        val context = BehaviorContext(
            snapshot = snapshotWithHistory,
            currentBattery = battery,
            screenBounds = screenBounds,
            quietHours = prefs.quietHours,
            reducedMotion = prefs.appearance.reducedMotion,
            nowEpochMillis = clock.nowEpochMillis(),
            currentHourOfDay = clock.currentHourOfDay(),
            pendingNotification = pendingNotification,
            pendingInteraction = pendingInteraction,
        )

        val decision = engine.tick(context)
        applyDecision(decision, context, pendingInteraction)
        return decision
    }

    private suspend fun applyDecision(
        decision: BehaviorDecision,
        context: BehaviorContext,
        interaction: UserInteractionType?,
    ) {
        val now = context.nowEpochMillis
        petRepository.updateSnapshot { current ->
            current.copy(
                state = decision.nextState,
                mood = decision.nextMood,
                needs = decision.nextNeeds,
                position = decision.nextPosition ?: current.position,
                activeSpeech = decision.speech ?: current.activeSpeech,
                lastStateChangeEpochMillis = if (decision.stateChanged) now else current.lastStateChangeEpochMillis,
                lastSpeechEpochMillis = if (decision.speech != null) now else current.lastSpeechEpochMillis,
                lastSpeechTrigger = decision.speech?.trigger ?: current.lastSpeechTrigger,
                lastInteractionEpochMillis = if (interaction != null) now else current.lastInteractionEpochMillis,
                lastFeedingEpochMillis = if (decision.feedingAccepted) now else current.lastFeedingEpochMillis,
                lastPersistedEpochMillis = now,
                batteryStatus = context.currentBattery,
                lastBatteryMilestoneReacted = context.currentBattery.currentMilestone,
            )
        }
        if (decision.stateChanged) {
            memoryRepository.recordStateEntered(decision.nextState, now)
        }
        if (interaction != null) {
            memoryRepository.recordInteraction(now)
        }
        if (decision.feedingAccepted) {
            memoryRepository.recordFeeding(now)
        }
    }
}
