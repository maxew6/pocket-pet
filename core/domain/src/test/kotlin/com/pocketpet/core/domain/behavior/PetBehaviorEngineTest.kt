package com.pocketpet.core.domain.behavior

import com.google.common.truth.Truth.assertThat
import com.pocketpet.core.domain.testutil.FakeClockProvider
import com.pocketpet.core.domain.testutil.FakeRandomProvider
import com.pocketpet.core.model.BatteryMilestone
import com.pocketpet.core.model.BatteryStatus
import com.pocketpet.core.model.Mood
import com.pocketpet.core.model.PetNeeds
import com.pocketpet.core.model.PetPosition
import com.pocketpet.core.model.PetSnapshot
import com.pocketpet.core.model.PetState
import com.pocketpet.core.model.QuietHours
import com.pocketpet.core.model.ScreenBounds
import org.junit.Before
import org.junit.Test

class PetBehaviorEngineTest {

    private lateinit var clock: FakeClockProvider
    private lateinit var random: FakeRandomProvider
    private lateinit var engine: PetBehaviorEngine

    private val bounds = ScreenBounds(widthDp = 400f, heightDp = 800f, topInsetDp = 24f, bottomInsetDp = 16f)
    private val noQuietHours = QuietHours(enabled = false)

    @Before
    fun setUp() {
        clock = FakeClockProvider(epochMillis = 1_700_000_000_000L, hourOfDay = 12)
        random = FakeRandomProvider()
        engine = PetBehaviorEngine(clock, random)
    }

    private fun context(
        snapshot: PetSnapshot,
        battery: BatteryStatus = BatteryStatus(percent = 60, isCharging = false, isFull = false),
        quietHours: QuietHours = noQuietHours,
        hourOfDay: Int = 12,
        interaction: UserInteractionType? = null,
        notification: com.pocketpet.core.model.NotificationEvent? = null,
    ) = BehaviorContext(
        snapshot = snapshot,
        currentBattery = battery,
        screenBounds = bounds,
        quietHours = quietHours,
        reducedMotion = false,
        nowEpochMillis = clock.nowEpochMillis(),
        currentHourOfDay = hourOfDay,
        pendingNotification = notification,
        pendingInteraction = interaction,
    )

    // ------------------------------------------------------------------------------------
    // Mood derivation
    // ------------------------------------------------------------------------------------

    @Test
    fun `very low battery and not charging derives Scared mood`() {
        val mood = engine.deriveMood(
            needs = PetNeeds.Fresh,
            battery = BatteryStatus(percent = 8, isCharging = false, isFull = false),
            isCharging = false,
        )
        assertThat(mood).isEqualTo(Mood.Scared)
    }

    @Test
    fun `high hunger derives Hungry mood even with fine battery`() {
        val mood = engine.deriveMood(
            needs = PetNeeds.Fresh.copy(hunger = 0.9f),
            battery = BatteryStatus(percent = 70, isCharging = false, isFull = false),
            isCharging = false,
        )
        assertThat(mood).isEqualTo(Mood.Hungry)
    }

    @Test
    fun `low energy derives Sleepy mood`() {
        val mood = engine.deriveMood(
            needs = PetNeeds.Fresh.copy(hunger = 0.1f, energy = 0.1f),
            battery = BatteryStatus(percent = 70, isCharging = false, isFull = false),
            isCharging = false,
        )
        assertThat(mood).isEqualTo(Mood.Sleepy)
    }

    @Test
    fun `full battery while charging derives Excited mood`() {
        val mood = engine.deriveMood(
            needs = PetNeeds.Fresh,
            battery = BatteryStatus(percent = 100, isCharging = true, isFull = true),
            isCharging = true,
        )
        assertThat(mood).isEqualTo(Mood.Excited)
    }

    @Test
    fun `balanced needs and mid battery derive a calm mood`() {
        val mood = engine.deriveMood(
            needs = PetNeeds(hunger = 0.2f, energy = 0.7f, affection = 0.6f, boredom = 0.2f, stress = 0.1f),
            battery = BatteryStatus(percent = 60, isCharging = false, isFull = false),
            isCharging = false,
        )
        assertThat(mood).isAnyOf(Mood.Happy, Mood.Content)
    }

    // ------------------------------------------------------------------------------------
    // State cooldown (prevents thrashing)
    // ------------------------------------------------------------------------------------

    @Test
    fun `state does not change before the minimum state duration elapses`() {
        val snapshot = PetSnapshot(
            state = PetState.Sitting,
            lastStateChangeEpochMillis = clock.nowEpochMillis() - 1_000L, // well under MIN_STATE_DURATION_MILLIS
            lastPersistedEpochMillis = clock.nowEpochMillis(),
        )

        val decision = engine.tick(context(snapshot))

        assertThat(decision.nextState).isEqualTo(PetState.Sitting)
        assertThat(decision.stateChanged).isFalse()
    }

    @Test
    fun `state is reconsidered once the maximum idle duration is exceeded`() {
        val snapshot = PetSnapshot(
            state = PetState.Sitting,
            needs = PetNeeds(hunger = 0.2f, energy = 0.7f, affection = 0.6f, boredom = 0.2f, stress = 0.1f),
            lastStateChangeEpochMillis = clock.nowEpochMillis() - 46_000L, // over MAX_IDLE_STATE_DURATION_MILLIS
            lastPersistedEpochMillis = clock.nowEpochMillis(),
            lastBatteryMilestoneReacted = BatteryMilestone.Normal,
        )

        val decision = engine.tick(context(snapshot))

        // FakeRandomProvider's default nextDouble()=0.0 deterministically selects the first
        // weighted candidate, which is always PetState.Idle in this engine's base weight table.
        assertThat(decision.stateChanged).isTrue()
        assertThat(decision.nextState).isEqualTo(PetState.Idle)
    }

    @Test
    fun `a tap always produces a reaction regardless of cooldown`() {
        val snapshot = PetSnapshot(
            state = PetState.Idle,
            lastStateChangeEpochMillis = clock.nowEpochMillis(), // just changed, deep in cooldown
            lastPersistedEpochMillis = clock.nowEpochMillis(),
        )

        val decision = engine.tick(context(snapshot, interaction = UserInteractionType.Tap))

        assertThat(decision.nextState).isEqualTo(PetState.HappyDance)
    }

    @Test
    fun `a double tap makes the pet jump`() {
        val snapshot = PetSnapshot(lastPersistedEpochMillis = clock.nowEpochMillis())
        val decision = engine.tick(context(snapshot, interaction = UserInteractionType.DoubleTap))
        assertThat(decision.nextState).isEqualTo(PetState.Jumping)
    }

    // ------------------------------------------------------------------------------------
    // Feeding cooldown (anti-exploit)
    // ------------------------------------------------------------------------------------

    @Test
    fun `feeding again immediately is rejected by the cooldown`() {
        val snapshot = PetSnapshot(
            needs = PetNeeds(hunger = 0.8f),
            lastFeedingEpochMillis = clock.nowEpochMillis() - 10_000L, // 10s ago, well under 90s cooldown
            lastPersistedEpochMillis = clock.nowEpochMillis(),
        )

        val decision = engine.tick(context(snapshot, interaction = UserInteractionType.Feed))

        assertThat(decision.feedingAccepted).isFalse()
        assertThat(decision.nextNeeds.hunger).isEqualTo(snapshot.needs.hunger)
    }

    @Test
    fun `feeding after the cooldown window succeeds and reduces hunger`() {
        val snapshot = PetSnapshot(
            needs = PetNeeds(hunger = 0.8f),
            lastFeedingEpochMillis = clock.nowEpochMillis() - 100_000L, // past the 90s cooldown
            lastPersistedEpochMillis = clock.nowEpochMillis(),
        )

        val decision = engine.tick(context(snapshot, interaction = UserInteractionType.Feed))

        assertThat(decision.feedingAccepted).isTrue()
        assertThat(decision.nextNeeds.hunger).isLessThan(snapshot.needs.hunger)
        assertThat(decision.nextState).isEqualTo(PetState.Eating)
    }

    @Test
    fun `feeding for the very first time is never blocked by the cooldown`() {
        val snapshot = PetSnapshot(
            needs = PetNeeds(hunger = 0.8f),
            lastFeedingEpochMillis = 0L, // never fed
            lastPersistedEpochMillis = clock.nowEpochMillis(),
        )

        val decision = engine.tick(context(snapshot, interaction = UserInteractionType.Feed))

        assertThat(decision.feedingAccepted).isTrue()
    }

    // ------------------------------------------------------------------------------------
    // Battery milestone reactions fire exactly once per crossing
    // ------------------------------------------------------------------------------------

    @Test
    fun `crossing into critical battery forces a crying reaction with speech`() {
        val snapshot = PetSnapshot(
            lastBatteryMilestoneReacted = BatteryMilestone.Low, // was Low, now dropping to Critical
            lastSpeechEpochMillis = 0L,
            lastPersistedEpochMillis = clock.nowEpochMillis(),
        )
        val critical = BatteryStatus(percent = 15, isCharging = false, isFull = false)

        val decision = engine.tick(context(snapshot, battery = critical))

        assertThat(decision.nextState).isEqualTo(PetState.Crying)
        assertThat(decision.speech?.trigger).isEqualTo(com.pocketpet.core.model.SpeechTrigger.BatteryReaction)
    }

    @Test
    fun `the same still-critical battery does not force crying again next tick`() {
        val snapshot = PetSnapshot(
            state = PetState.Sitting,
            lastStateChangeEpochMillis = clock.nowEpochMillis(), // fresh, cooldown active
            // Already reacted to Critical on a previous tick:
            lastBatteryMilestoneReacted = BatteryMilestone.Critical,
            lastPersistedEpochMillis = clock.nowEpochMillis(),
        )
        val stillCritical = BatteryStatus(percent = 14, isCharging = false, isFull = false)

        val decision = engine.tick(context(snapshot, battery = stillCritical))

        // No forced override this time, and state-change cooldown keeps it from being reconsidered.
        assertThat(decision.nextState).isEqualTo(PetState.Sitting)
        assertThat(decision.stateChanged).isFalse()
    }

    @Test
    fun `reaching full charge while plugged in triggers a celebration exactly once`() {
        val snapshot = PetSnapshot(
            lastBatteryMilestoneReacted = BatteryMilestone.High,
            lastPersistedEpochMillis = clock.nowEpochMillis(),
            batteryStatus = BatteryStatus(percent = 99, isCharging = true, isFull = false),
        )
        val full = BatteryStatus(percent = 100, isCharging = true, isFull = true)

        val decision = engine.tick(context(snapshot, battery = full))

        assertThat(decision.nextState).isEqualTo(PetState.HappyDance)
        assertThat(decision.justReachedFullCharge).isTrue()
    }

    // ------------------------------------------------------------------------------------
    // Quiet hours
    // ------------------------------------------------------------------------------------

    @Test
    fun `quiet hours suppress new speech bubbles even when hunger is critical`() {
        val snapshot = PetSnapshot(
            needs = PetNeeds(hunger = 0.95f),
            lastSpeechEpochMillis = 0L,
            lastPersistedEpochMillis = clock.nowEpochMillis(),
        )
        val nightQuietHours = QuietHours(enabled = true, startHour = 22, endHour = 7)

        val decision = engine.tick(context(snapshot, quietHours = nightQuietHours, hourOfDay = 2))

        assertThat(decision.speech).isNull()
    }

    @Test
    fun `speech respects the minimum interval between bubbles`() {
        val snapshot = PetSnapshot(
            needs = PetNeeds(hunger = 0.95f),
            lastSpeechEpochMillis = clock.nowEpochMillis() - 5_000L, // recently shown
            lastPersistedEpochMillis = clock.nowEpochMillis(),
        )

        val decision = engine.tick(context(snapshot))

        assertThat(decision.speech).isNull()
    }

    // ------------------------------------------------------------------------------------
    // Restore after a process-death gap
    // ------------------------------------------------------------------------------------

    @Test
    fun `restoring after a moderate gap increases hunger and resets to Idle`() {
        val twoHoursAgo = clock.nowEpochMillis() - 2L * 60 * 60 * 1000
        val snapshot = PetSnapshot(
            state = PetState.Crying,
            needs = PetNeeds(hunger = 0.1f),
            lastPersistedEpochMillis = twoHoursAgo,
        )

        val restored = engine.restoreAfterGap(snapshot)

        assertThat(restored.needs.hunger).isGreaterThan(snapshot.needs.hunger)
        assertThat(restored.state).isEqualTo(PetState.Idle)
        assertThat(restored.lastPersistedEpochMillis).isEqualTo(clock.nowEpochMillis())
    }

    @Test
    fun `restoring after an extreme gap saturates needs safely instead of producing invalid values`() {
        val oneWeekAgo = clock.nowEpochMillis() - 7L * 24 * 60 * 60 * 1000
        val snapshot = PetSnapshot(
            needs = PetNeeds(hunger = 0.1f, energy = 0.9f),
            lastPersistedEpochMillis = oneWeekAgo,
        )

        val restored = engine.restoreAfterGap(snapshot)

        assertThat(restored.needs.hunger).isEqualTo(1.0f)
        assertThat(restored.needs.energy).isIn(0.0f..1.0f)
    }

    @Test
    fun `a fresh position offering never moves the pet without a reason`() {
        val snapshot = PetSnapshot(
            position = PetPosition(xDp = 120f, yDp = 300f),
            needs = PetNeeds(hunger = 0.2f, energy = 0.7f, affection = 0.6f, boredom = 0.2f, stress = 0.1f),
            lastStateChangeEpochMillis = clock.nowEpochMillis() - 1_000L,
            lastPersistedEpochMillis = clock.nowEpochMillis(),
        )

        val decision = engine.tick(context(snapshot))

        assertThat(decision.nextPosition).isNull()
    }

    @Test
    fun `a notification nudges the pet toward the safe top edge`() {
        val snapshot = PetSnapshot(
            position = PetPosition(xDp = 120f, yDp = 300f),
            lastPersistedEpochMillis = clock.nowEpochMillis(),
        )
        val notification = com.pocketpet.core.model.NotificationEvent(
            postingPackageName = "com.example.mail",
            postedAtEpochMillis = clock.nowEpochMillis(),
        )

        val decision = engine.tick(context(snapshot, notification = notification))

        assertThat(decision.nextState).isEqualTo(PetState.WatchingNotification)
        assertThat(decision.nextPosition?.yDp).isEqualTo(bounds.safeTop)
    }
}
