package com.pocketpet.core.designsystem.canvas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.pocketpet.core.model.Mood
import com.pocketpet.core.model.PetState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Turns the current [state]/[mood] into a live, continuously-updating [PetRenderState]. Every
 * continuous behavior (breathing, blinking, tail sway, idle pupil wander) keeps a coroutine loop
 * alive for as long as this is composed; state-entry effects (a roll's spin, a jump's ear-perk)
 * key off [state] and run once per entry. [reducedMotion] scales amplitude down rather than
 * freezing everything outright, so the pet still reads as "alive" for people who asked for less
 * motion.
 *
 * [isAnimationsEnabled] is read fresh on every loop iteration (not captured once) so a caller can
 * flip it live — the overlay service passes a screen-on/off flag here so the continuous loops
 * stop doing animation work while the screen is off, per the "pause animation when the screen is
 * off" requirement. It defaults to always-on, so the other call sites (Welcome, Home,
 * Customization previews, which are only ever composed while a screen is already on) don't need
 * to pass anything.
 */
@Composable
fun rememberPetAnimationController(
    state: PetState,
    mood: Mood,
    reducedMotion: Boolean,
    feedingPulse: Boolean,
    chargingCelebration: Boolean,
    isAnimationsEnabled: () -> Boolean = { true },
): PetRenderState {
    val profile = remember(state, mood) { PetStateMotionProfiles.forState(state, mood) }
    val motionScale = if (reducedMotion) 0.15f else 1f

    // The bounce/tail/breathing/blink/paw/pupil loops below are long-lived LaunchedEffects that
    // do NOT restart every recomposition (their keys are state/mood-derived, not
    // isAnimationsEnabled). Reading the `isAnimationsEnabled` parameter directly from inside
    // their bodies would capture a stale, first-composition copy of the lambda — exactly the
    // problem rememberUpdatedState exists to solve: it hands back a State that always reflects
    // the latest value even when read from an effect that hasn't restarted.
    val currentIsAnimationsEnabled by rememberUpdatedState(isAnimationsEnabled)

    val bounce = remember { Animatable(0f) }
    val tail = remember { Animatable(0f) }
    val breathing = remember { Animatable(0f) }
    val paw = remember { Animatable(0f) }
    val eyeOpenness = remember { Animatable(1f) }
    val pupilX = remember { Animatable(0f) }
    val pupilY = remember { Animatable(0f) }
    val bodyRotation = remember { Animatable(0f) }
    val earRotation = remember { Animatable(0f) }
    val tearProgress = remember { Animatable(0f) }
    val heartBurst = remember { Animatable(0f) }
    val confettiBurst = remember { Animatable(0f) }

    LaunchedEffect(profile.bounceFrequencyHz, motionScale) {
        val periodMillis = (1000 / profile.bounceFrequencyHz.coerceAtLeast(0.15f)).roundToInt().coerceIn(200, 6000)
        while (isActive) {
            if (!currentIsAnimationsEnabled()) {
                delay(PAUSED_POLL_INTERVAL_MILLIS)
                continue
            }
            bounce.animateTo(1f, tween(periodMillis / 2, easing = FastOutSlowInEasing))
            bounce.animateTo(-1f, tween(periodMillis / 2, easing = FastOutSlowInEasing))
        }
    }

    LaunchedEffect(profile.tailSwaySpeed) {
        val periodMillis = (1600 / profile.tailSwaySpeed.coerceAtLeast(0.1f)).roundToInt().coerceIn(300, 5000)
        while (isActive) {
            if (!currentIsAnimationsEnabled()) {
                delay(PAUSED_POLL_INTERVAL_MILLIS)
                continue
            }
            tail.animateTo(1f, tween(periodMillis / 2, easing = LinearEasing))
            tail.animateTo(0f, tween(periodMillis / 2, easing = LinearEasing))
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            if (!currentIsAnimationsEnabled()) {
                delay(PAUSED_POLL_INTERVAL_MILLIS)
                continue
            }
            breathing.animateTo(1f, tween(1400, easing = FastOutSlowInEasing))
            breathing.animateTo(0f, tween(1400, easing = FastOutSlowInEasing))
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(Random.nextLong(2_500, 6_000))
            if (!currentIsAnimationsEnabled()) continue
            eyeOpenness.animateTo(0.05f, tween(90))
            eyeOpenness.animateTo(1f, tween(120))
        }
    }

    LaunchedEffect(profile.pawActivity) {
        if (profile.pawActivity <= 0f) {
            paw.snapTo(0f)
        } else {
            while (isActive) {
                if (!currentIsAnimationsEnabled()) {
                    delay(PAUSED_POLL_INTERVAL_MILLIS)
                    continue
                }
                paw.animateTo(1f, tween(260, easing = LinearEasing))
                paw.animateTo(0f, tween(260, easing = LinearEasing))
            }
        }
    }

    LaunchedEffect(state) {
        if (state == PetState.Rolling) {
            bodyRotation.snapTo(0f)
            bodyRotation.animateTo(360f, tween(700, easing = LinearEasing))
            bodyRotation.snapTo(0f)
        } else {
            bodyRotation.animateTo(0f, tween(300))
        }
    }

    LaunchedEffect(state) {
        if (state == PetState.Jumping || state == PetState.HappyDance) {
            earRotation.animateTo(-18f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            earRotation.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
        }
    }

    LaunchedEffect(profile.showTears) {
        tearProgress.animateTo(if (profile.showTears) 1f else 0f, tween(500))
    }

    LaunchedEffect(feedingPulse) {
        if (feedingPulse) {
            heartBurst.snapTo(0f)
            heartBurst.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
            heartBurst.animateTo(0f, tween(400))
        }
    }

    LaunchedEffect(chargingCelebration) {
        if (chargingCelebration) {
            confettiBurst.snapTo(0f)
            confettiBurst.animateTo(1f, tween(1_600, easing = LinearEasing))
            confettiBurst.snapTo(0f)
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            if (!currentIsAnimationsEnabled()) {
                delay(PAUSED_POLL_INTERVAL_MILLIS)
                continue
            }
            val targetX = Random.nextDouble(-0.4, 0.4).toFloat()
            val targetY = Random.nextDouble(-0.25, 0.25).toFloat()
            pupilX.animateTo(targetX, tween(900, easing = FastOutSlowInEasing))
            pupilY.animateTo(targetY, tween(900, easing = FastOutSlowInEasing))
            delay(Random.nextLong(1_800, 3_600))
        }
    }

    return PetRenderState(
        bodyOffsetXDp = 0f,
        bodyOffsetYDp = bounce.value * profile.bounceAmplitudeDp * motionScale,
        bodyRotationDegrees = profile.baseRotationDegrees + bodyRotation.value,
        bodyScale = 1f,
        squash = 1f + (breathing.value - 0.5f) * profile.squashStretchAmplitude * motionScale,
        stretch = 1f - (breathing.value - 0.5f) * profile.squashStretchAmplitude * motionScale,
        eyeOpenness = eyeOpenness.value,
        pupilOffsetX = pupilX.value,
        pupilOffsetY = pupilY.value,
        earRotationDegrees = earRotation.value,
        tailPhase = tail.value,
        pawPhase = (paw.value * profile.pawActivity).coerceIn(0f, 1.2f),
        breathingPhase = breathing.value,
        mouthExpression = profile.mouth,
        tearProgress = tearProgress.value,
        shadowScale = 1f - abs(bounce.value) * 0.15f * motionScale,
        heartBurstProgress = heartBurst.value,
        confettiBurstProgress = confettiBurst.value,
        showZzz = profile.showZzz,
        showFoodBowl = profile.showFoodBowl,
        showEnergyParticles = state == PetState.WatchingCharging,
    )
}

/** How often a paused loop rechecks [rememberPetAnimationController]'s isAnimationsEnabled flag —
 *  frequent enough that animation resumes promptly when the screen turns back on, infrequent
 *  enough that it isn't itself a busy loop. */
private const val PAUSED_POLL_INTERVAL_MILLIS = 400L
