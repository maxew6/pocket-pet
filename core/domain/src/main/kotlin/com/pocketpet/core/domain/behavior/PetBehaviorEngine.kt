package com.pocketpet.core.domain.behavior

import com.pocketpet.core.domain.provider.ClockProvider
import com.pocketpet.core.domain.provider.RandomProvider
import com.pocketpet.core.model.BatteryMilestone
import com.pocketpet.core.model.BatteryStatus
import com.pocketpet.core.model.Mood
import com.pocketpet.core.model.PetNeeds
import com.pocketpet.core.model.PetPosition
import com.pocketpet.core.model.PetSnapshot
import com.pocketpet.core.model.PetState
import com.pocketpet.core.model.SpeechBubble
import com.pocketpet.core.model.SpeechTrigger

/**
 * Decides what the pet is doing, how it feels, and whether it says anything — deterministically,
 * given its inputs. Every random choice goes through the injected [random]; the only place the
 * injected [clock] is read directly is [restoreAfterGap], which by nature has no per-call context
 * to source "now" from. Everything else is a pure function of its parameters, which is what makes
 * this class exhaustively unit-testable without Android, a device, or real elapsed time.
 *
 * The engine never busy-loops: [tick] is meant to be invoked by an event-driven caller (a
 * low-frequency coroutine ticker while the overlay is visible, plus immediately on user
 * interaction, notification events, and battery-broadcast changes) — see `service:overlay`'s
 * `OverlayService` for that scheduling.
 */
class PetBehaviorEngine(
    private val clock: ClockProvider,
    private val random: RandomProvider,
) {

    /** Produces the next [BehaviorDecision] for a single point in time, described by [context]. */
    fun tick(context: BehaviorContext): BehaviorDecision {
        val snapshot = context.snapshot
        val isNight = context.quietHours.contains(context.currentHourOfDay)
        val isCharging = context.currentBattery.isCharging

        // 1. Advance needs by real elapsed time since the last time we updated them.
        val rawElapsed = (context.nowEpochMillis - snapshot.lastPersistedEpochMillis).coerceAtLeast(0L)
        val elapsed = rawElapsed.coerceAtMost(MAX_TICK_GAP_MILLIS)
        val batteryStress = batteryStressLevel(context.currentBattery)
        var needs = NeedsSimulator.applyElapsedTime(
            needs = snapshot.needs,
            elapsedMillis = elapsed,
            isSleepingOrCharging = isCharging || NeedsSimulator.isRecoveringState(snapshot.state),
            batteryStressLevel = batteryStress,
        )

        // 2. Handle a feed request, respecting the anti-exploit cooldown.
        val isFeedRequest = context.pendingInteraction == UserInteractionType.Feed
        val sinceLastFeeding = context.nowEpochMillis - snapshot.lastFeedingEpochMillis
        val feedingAccepted = isFeedRequest &&
            (snapshot.lastFeedingEpochMillis <= 0L || sinceLastFeeding >= FEED_COOLDOWN_MILLIS)
        if (feedingAccepted) {
            needs = NeedsSimulator.applyFeeding(needs)
        }

        // 3. Derive mood from the (now up to date) needs and battery.
        val mood = deriveMood(needs, context.currentBattery, isCharging)

        // 4. Detect one-shot events: charging just started, a battery milestone just crossed.
        val batteryJustStartedCharging = isCharging && !snapshot.batteryStatus.isCharging
        val currentMilestone = context.currentBattery.currentMilestone
        val milestoneJustCrossed = currentMilestone.takeIf { it != snapshot.lastBatteryMilestoneReacted }
        val justReachedFullCharge = milestoneJustCrossed == BatteryMilestone.Full && isCharging

        // 5. Pick the next discrete state.
        val forced = forcedStateFor(context, feedingAccepted, batteryJustStartedCharging, milestoneJustCrossed)
        val timeInCurrentState = context.nowEpochMillis - snapshot.lastStateChangeEpochMillis
        val mustReconsider = timeInCurrentState >= MAX_IDLE_STATE_DURATION_MILLIS
        val mayReconsider = timeInCurrentState >= MIN_STATE_DURATION_MILLIS
        val nextState = when {
            forced != null -> forced
            !mayReconsider && !mustReconsider -> snapshot.state
            else -> pickWeightedState(mood, needs, isNight, isCharging, snapshot.recentStates)
        }
        val stateChanged = nextState != snapshot.state

        // 6. Decide whether to show a speech bubble.
        val speech = decideSpeech(context, mood, needs, milestoneJustCrossed, isNight)

        // 7. Decide whether to nudge toward a target position (notification / low battery only).
        val nextPosition = decideTargetPosition(context, milestoneJustCrossed)

        return BehaviorDecision(
            nextState = nextState,
            nextMood = mood,
            nextNeeds = needs,
            speech = speech,
            nextPosition = nextPosition,
            stateChanged = stateChanged,
            feedingAccepted = feedingAccepted,
            justReachedFullCharge = justReachedFullCharge,
        )
    }

    /**
     * Called once on cold start (process restoration) with only the last-persisted snapshot.
     * Applies a real elapsed-time delta, capped at [NeedsSimulator.MAX_SIMULATED_GAP_MILLIS], so
     * a phone left off for days comes back "very hungry" rather than an extrapolated absurdity —
     * and without replaying every tick that would have happened while the process was dead.
     */
    fun restoreAfterGap(snapshot: PetSnapshot): PetSnapshot {
        val now = clock.nowEpochMillis()
        val rawElapsed = (now - snapshot.lastPersistedEpochMillis).coerceAtLeast(0L)
        val clampedElapsed = NeedsSimulator.clampGap(rawElapsed)
        val restoredNeeds = NeedsSimulator.applyElapsedTime(
            needs = snapshot.needs,
            elapsedMillis = clampedElapsed,
            // We don't know whether the device was charging while the process was dead; assume
            // the more conservative "awake, draining" case rather than assuming free recovery.
            isSleepingOrCharging = false,
            batteryStressLevel = snapshot.needs.stress,
        )
        return snapshot.copy(
            needs = restoredNeeds,
            state = PetState.Idle,
            lastStateChangeEpochMillis = now,
            lastPersistedEpochMillis = now,
        )
    }

    // ---------------------------------------------------------------------------------------
    // Mood
    // ---------------------------------------------------------------------------------------

    internal fun deriveMood(needs: PetNeeds, battery: BatteryStatus, isCharging: Boolean): Mood {
        val milestone = battery.currentMilestone
        return when {
            !isCharging && milestone == BatteryMilestone.Emergency -> Mood.Scared
            !isCharging && milestone == BatteryMilestone.Critical -> Mood.Concerned
            needs.hunger > 0.75f -> Mood.Hungry
            !isCharging && milestone == BatteryMilestone.Low -> Mood.Concerned
            needs.energy < 0.2f -> Mood.Sleepy
            needs.stress > 0.6f -> Mood.Concerned
            needs.boredom > 0.75f && needs.affection < 0.35f -> Mood.Lonely
            needs.boredom > 0.7f -> Mood.Curious
            needs.energy < 0.4f && needs.boredom < 0.3f -> Mood.Lazy
            (isCharging && milestone == BatteryMilestone.Full) || needs.affection > 0.85f -> Mood.Excited
            needs.affection > 0.55f && needs.hunger < 0.35f && needs.energy > 0.5f -> Mood.Happy
            else -> Mood.Content
        }
    }

    private fun batteryStressLevel(battery: BatteryStatus): Float {
        if (battery.isCharging) return 0.1f
        return when (battery.currentMilestone) {
            BatteryMilestone.Emergency -> 0.9f
            BatteryMilestone.Critical -> 0.7f
            BatteryMilestone.Low -> 0.45f
            else -> 0.1f
        }
    }

    // ---------------------------------------------------------------------------------------
    // State selection
    // ---------------------------------------------------------------------------------------

    /** Reactions that must win regardless of cooldown — direct interaction always feels heard. */
    private fun forcedStateFor(
        context: BehaviorContext,
        feedingAccepted: Boolean,
        batteryJustStartedCharging: Boolean,
        milestoneJustCrossed: BatteryMilestone?,
    ): PetState? {
        context.pendingInteraction?.let { interaction ->
            return when (interaction) {
                UserInteractionType.Tap -> PetState.HappyDance
                UserInteractionType.DoubleTap -> PetState.Jumping
                UserInteractionType.LongPress -> PetState.Sitting
                UserInteractionType.DragStart -> PetState.BeingDragged
                UserInteractionType.DragEnd -> PetState.Sitting
                UserInteractionType.UpwardFling -> PetState.Jumping
                UserInteractionType.HorizontalFling -> PetState.Rolling
                UserInteractionType.Feed -> if (feedingAccepted) PetState.Eating else context.snapshot.state
                UserInteractionType.Play -> PetState.Running
                UserInteractionType.ToggleSleep ->
                    if (context.snapshot.state == PetState.Sleeping) PetState.Idle else PetState.Sleeping
            }
        }
        if (context.pendingNotification != null) return PetState.WatchingNotification
        if (milestoneJustCrossed == BatteryMilestone.Emergency) return PetState.Hiding
        if (milestoneJustCrossed == BatteryMilestone.Critical) return PetState.Crying
        if (milestoneJustCrossed == BatteryMilestone.Full && context.currentBattery.isCharging) {
            return PetState.HappyDance
        }
        if (batteryJustStartedCharging) return PetState.WatchingCharging
        return null
    }

    private fun pickWeightedState(
        mood: Mood,
        needs: PetNeeds,
        isNight: Boolean,
        isCharging: Boolean,
        recentStates: List<PetState>,
    ): PetState {
        var candidates = baseWeightsFor(mood, needs)
        if (isCharging) {
            candidates = candidates + (PetState.WatchingCharging to 2.5)
        }
        if (isNight) {
            val energetic = setOf(PetState.Running, PetState.Jumping, PetState.HappyDance, PetState.Rolling)
            candidates = candidates.filterNot { it.first in energetic } + (PetState.Sleeping to 4.0)
        }
        val penalized = applyRepetitionPenalty(candidates, recentStates)
        return random.weightedPick(penalized) ?: PetState.Idle
    }

    private fun baseWeightsFor(mood: Mood, needs: PetNeeds): List<Pair<PetState, Double>> {
        val weights = mutableListOf(
            PetState.Idle to 3.0,
            PetState.LookingAround to 2.0,
            PetState.Sitting to 1.5,
        )
        when (mood) {
            Mood.Sleepy -> {
                weights += PetState.Yawning to 4.0
                weights += PetState.Sleeping to 4.5
                weights += PetState.Stretching to 1.0
            }
            Mood.Hungry -> {
                weights += PetState.Crying to (1.5 + needs.hunger * 2.5)
                weights += PetState.Sitting to 2.0
            }
            Mood.Excited -> {
                weights += PetState.HappyDance to 4.0
                weights += PetState.Running to 3.0
                weights += PetState.Jumping to 2.0
            }
            Mood.Happy -> {
                weights += PetState.Walking to 3.0
                weights += PetState.Grooming to 2.0
                weights += PetState.HappyDance to 1.5
            }
            Mood.Lonely -> {
                weights += PetState.Sitting to 3.0
                weights += PetState.LookingAround to 2.5
                weights += PetState.Hiding to 1.0
            }
            Mood.Lazy -> {
                weights += PetState.Sitting to 3.0
                weights += PetState.Grooming to 2.5
            }
            Mood.Curious -> {
                weights += PetState.Walking to 3.0
                weights += PetState.LookingAround to 3.0
                weights += PetState.Jumping to 1.0
            }
            Mood.Concerned -> {
                weights += PetState.Sitting to 2.5
                weights += PetState.LookingAround to 1.0
                weights += PetState.Recovering to 1.5
            }
            Mood.Scared -> {
                weights += PetState.Hiding to 5.0
                weights += PetState.Crying to 1.0
            }
            Mood.Content -> {
                weights += PetState.Walking to 2.0
                weights += PetState.Grooming to 1.5
                weights += PetState.Sitting to 1.5
            }
        }
        return weights
    }

    private fun applyRepetitionPenalty(
        candidates: List<Pair<PetState, Double>>,
        recentStates: List<PetState>,
    ): List<Pair<PetState, Double>> = candidates.map { (state, weight) ->
        val recencyIndex = recentStates.indexOf(state)
        val penalty = when (recencyIndex) {
            -1 -> 1.0
            0 -> 0.15
            1 -> 0.4
            2 -> 0.65
            else -> 0.85
        }
        state to (weight * penalty)
    }

    // ---------------------------------------------------------------------------------------
    // Speech
    // ---------------------------------------------------------------------------------------

    private fun decideSpeech(
        context: BehaviorContext,
        mood: Mood,
        needs: PetNeeds,
        milestoneJustCrossed: BatteryMilestone?,
        isNight: Boolean,
    ): SpeechBubble? {
        if (isNight) return null
        val snapshot = context.snapshot
        val sinceLastSpeech = context.nowEpochMillis - snapshot.lastSpeechEpochMillis
        if (sinceLastSpeech < MIN_SPEECH_INTERVAL_MILLIS) return null

        val sinceLastInteraction = context.nowEpochMillis - snapshot.lastInteractionEpochMillis
        val isReunion = context.pendingInteraction != null && sinceLastInteraction >= REUNION_THRESHOLD_MILLIS

        val trigger: SpeechTrigger = when {
            milestoneJustCrossed == BatteryMilestone.Critical || milestoneJustCrossed == BatteryMilestone.Emergency ->
                SpeechTrigger.BatteryReaction
            milestoneJustCrossed == BatteryMilestone.Low -> SpeechTrigger.LowEnergyRequest
            isReunion -> SpeechTrigger.Reunion
            context.pendingNotification != null -> SpeechTrigger.NotificationReaction
            context.pendingInteraction == UserInteractionType.Feed -> return null
            needs.hunger > 0.8f -> SpeechTrigger.Hunger
            needs.energy < 0.2f -> SpeechTrigger.Sleepiness
            needs.boredom > 0.8f -> SpeechTrigger.Boredom
            mood == Mood.Curious && random.nextDouble() < 0.25 -> SpeechTrigger.Discovery
            (mood == Mood.Happy || mood == Mood.Excited) && random.nextDouble() < 0.3 -> SpeechTrigger.Happiness
            else -> return null
        }

        val text = SpeechLinePicker.pick(trigger, random, avoiding = snapshot.activeSpeech?.text)
        if (text.isBlank()) return null
        return SpeechBubble(text = text, trigger = trigger, shownAtEpochMillis = context.nowEpochMillis)
    }

    // ---------------------------------------------------------------------------------------
    // Positioning
    // ---------------------------------------------------------------------------------------

    /**
     * A target position to animate toward, or `null` for "stay where you are". Only notification
     * reactions and low-battery "seeking energy" reactions request a move — the pet never paces
     * around on its own initiative on every tick. Only the vertical target is set: the engine
     * intentionally does not know the pet's rendered size, so horizontal position and final
     * bounds clamping stay the overlay layer's job (see [com.pocketpet.core.model.ScreenBounds]).
     */
    private fun decideTargetPosition(
        context: BehaviorContext,
        milestoneJustCrossed: BatteryMilestone?,
    ): PetPosition? {
        val seekTopEdge = context.pendingNotification != null ||
            (milestoneJustCrossed != null &&
                milestoneJustCrossed.thresholdPercent <= BatteryMilestone.Low.thresholdPercent &&
                !context.currentBattery.isCharging)
        if (!seekTopEdge) return null
        return context.snapshot.position.copy(yDp = context.screenBounds.safeTop)
    }

    companion object {
        const val MIN_STATE_DURATION_MILLIS = 3_500L
        const val MAX_IDLE_STATE_DURATION_MILLIS = 45_000L
        const val MAX_TICK_GAP_MILLIS = 2 * 60 * 60 * 1000L
        const val MIN_SPEECH_INTERVAL_MILLIS = 40_000L
        const val REUNION_THRESHOLD_MILLIS = 3 * 60 * 60 * 1000L
        const val FEED_COOLDOWN_MILLIS = 90_000L
    }
}
