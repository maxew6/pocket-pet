package com.pocketpet.core.domain.behavior

import com.pocketpet.core.model.BatteryStatus
import com.pocketpet.core.model.Mood
import com.pocketpet.core.model.NotificationEvent
import com.pocketpet.core.model.PetNeeds
import com.pocketpet.core.model.PetPosition
import com.pocketpet.core.model.PetSnapshot
import com.pocketpet.core.model.PetState
import com.pocketpet.core.model.QuietHours
import com.pocketpet.core.model.ScreenBounds
import com.pocketpet.core.model.SpeechBubble

/** A direct user gesture the overlay reports to the engine, separate from the ambient tick. */
enum class UserInteractionType {
    Tap,
    DoubleTap,
    LongPress,
    DragStart,
    DragEnd,
    UpwardFling,
    HorizontalFling,
    Feed,
    Play,
    ToggleSleep,
}

/** Everything the engine needs to make one decision. Nothing here is read from a live source. */
data class BehaviorContext(
    val snapshot: PetSnapshot,
    val currentBattery: BatteryStatus,
    val screenBounds: ScreenBounds,
    val quietHours: QuietHours,
    val reducedMotion: Boolean,
    val nowEpochMillis: Long,
    val currentHourOfDay: Int,
    val isForeground: Boolean = true,
    val pendingNotification: NotificationEvent? = null,
    val pendingInteraction: UserInteractionType? = null,
)

/** The engine's output: a new state/mood/needs and optionally a fresh speech bubble or nudge. */
data class BehaviorDecision(
    val nextState: PetState,
    val nextMood: Mood,
    val nextNeeds: PetNeeds,
    val speech: SpeechBubble? = null,
    val nextPosition: PetPosition? = null,
    val stateChanged: Boolean,
    /** True exactly on the tick where a feed request was accepted (not throttled by cooldown). */
    val feedingAccepted: Boolean = false,
    /** True exactly on the tick where the battery just crossed into Full while charging. */
    val justReachedFullCharge: Boolean = false,
)
