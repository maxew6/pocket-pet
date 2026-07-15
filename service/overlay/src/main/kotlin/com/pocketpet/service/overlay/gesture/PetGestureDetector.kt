package com.pocketpet.service.overlay.gesture

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/** Everything the overlay's gesture area can report — see spec section 9, "Touch and Physics". */
data class PetGestureCallbacks(
    val onTap: () -> Unit = {},
    val onDoubleTap: () -> Unit = {},
    val onLongPress: () -> Unit = {},
    val onDragStart: () -> Unit = {},
    val onDrag: (Offset) -> Unit = {},
    val onDragEnd: () -> Unit = {},
    val onUpwardFling: () -> Unit = {},
    val onHorizontalFling: (velocityPxPerSecond: Float) -> Unit = {},
)

private const val FLING_VELOCITY_THRESHOLD_PX_PER_SEC = 800f

/**
 * Combines Compose's standard tap and drag detectors on the same pointer stream — the drag
 * detector only claims the gesture once movement exceeds touch slop (consuming the position
 * change as it does), which is what lets a plain tap still reach [detectTapGestures] unclaimed.
 *
 * Fling velocity is estimated as total displacement over the whole drag rather than a true
 * instantaneous release velocity — simple, dependency-free, and plenty precise for a toss gesture
 * that only needs to pick a direction and a rough strength.
 */
fun Modifier.petGestures(callbacks: PetGestureCallbacks): Modifier = this
    .pointerInput(callbacks) {
        detectTapGestures(
            onTap = { callbacks.onTap() },
            onDoubleTap = { callbacks.onDoubleTap() },
            onLongPress = { callbacks.onLongPress() },
        )
    }
    .pointerInput(callbacks) {
        var accumulatedDx = 0f
        var accumulatedDy = 0f
        var dragStartTimeMillis = 0L

        detectDragGestures(
            onDragStart = {
                accumulatedDx = 0f
                accumulatedDy = 0f
                dragStartTimeMillis = System.currentTimeMillis()
                callbacks.onDragStart()
            },
            onDrag = { change, dragAmount ->
                change.consume()
                accumulatedDx += dragAmount.x
                accumulatedDy += dragAmount.y
                callbacks.onDrag(dragAmount)
            },
            onDragEnd = {
                val elapsedSeconds =
                    ((System.currentTimeMillis() - dragStartTimeMillis).coerceAtLeast(1)) / 1000f
                val velocityX = accumulatedDx / elapsedSeconds
                val velocityY = accumulatedDy / elapsedSeconds
                callbacks.onDragEnd()
                when {
                    velocityY < -FLING_VELOCITY_THRESHOLD_PX_PER_SEC && abs(velocityY) > abs(velocityX) ->
                        callbacks.onUpwardFling()
                    abs(velocityX) > FLING_VELOCITY_THRESHOLD_PX_PER_SEC ->
                        callbacks.onHorizontalFling(velocityX)
                }
            },
        )
    }
