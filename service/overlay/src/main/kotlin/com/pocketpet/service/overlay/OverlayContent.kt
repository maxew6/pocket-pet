package com.pocketpet.service.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.pocketpet.core.designsystem.canvas.PetCanvas
import com.pocketpet.core.designsystem.canvas.rememberPetAnimationController
import com.pocketpet.core.designsystem.component.PetSpeechBubble
import com.pocketpet.core.designsystem.theme.PocketPetTheme
import com.pocketpet.core.model.AppTheme
import com.pocketpet.core.model.PetPreferences
import com.pocketpet.core.model.PetSnapshot
import com.pocketpet.core.model.PetState
import com.pocketpet.service.overlay.gesture.PetGestureCallbacks
import com.pocketpet.service.overlay.gesture.petGestures

/** Every action the overlay's UI can trigger. The Service wires each to a real use-case call. */
data class OverlayActions(
    val onTap: () -> Unit = {},
    val onDoubleTap: () -> Unit = {},
    val onLongPress: () -> Unit = {},
    val onDragStart: () -> Unit = {},
    val onDrag: (Offset) -> Unit = {},
    val onDragEnd: () -> Unit = {},
    val onUpwardFling: () -> Unit = {},
    val onHorizontalFling: (Float) -> Unit = {},
    val onFeed: () -> Unit = {},
    val onPlay: () -> Unit = {},
    val onToggleSleep: () -> Unit = {},
    val onHideTemporarily: () -> Unit = {},
    val onTogglePositionLock: () -> Unit = {},
    val onOpenApp: () -> Unit = {},
)

private const val PET_SIZE_DP = 88

@Composable
fun OverlayRoot(
    snapshot: PetSnapshot,
    preferences: PetPreferences,
    feedingPulse: Boolean,
    chargingCelebration: Boolean,
    isScreenOn: Boolean,
    actions: OverlayActions,
) {
    val darkTheme = when (preferences.theme) {
        AppTheme.Light -> false
        AppTheme.Dark -> true
        AppTheme.System -> isSystemInDarkTheme()
    }
    var menuExpanded by remember { mutableStateOf(false) }

    PocketPetTheme(darkTheme = darkTheme) {
        Box(modifier = Modifier.size(PET_SIZE_DP.dp + 40.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-36).dp),
            ) {
                PetSpeechBubble(
                    text = snapshot.activeSpeech?.text.orEmpty(),
                    visible = snapshot.activeSpeech != null,
                )
            }

            val renderState = rememberPetAnimationController(
                state = snapshot.state,
                mood = snapshot.mood,
                reducedMotion = preferences.appearance.reducedMotion,
                feedingPulse = feedingPulse,
                chargingCelebration = chargingCelebration,
                isAnimationsEnabled = { isScreenOn },
            )

            PetCanvas(
                renderState = renderState,
                appearance = preferences.appearance,
                modifier = Modifier
                    .size(PET_SIZE_DP.dp)
                    .align(Alignment.Center)
                    .petGestures(
                        PetGestureCallbacks(
                            onTap = actions.onTap,
                            onDoubleTap = actions.onDoubleTap,
                            onLongPress = {
                                menuExpanded = true
                                actions.onLongPress()
                            },
                            onDragStart = actions.onDragStart,
                            onDrag = actions.onDrag,
                            onDragEnd = actions.onDragEnd,
                            onUpwardFling = actions.onUpwardFling,
                            onHorizontalFling = actions.onHorizontalFling,
                        ),
                    ),
            )

            AnimatedVisibility(
                visible = menuExpanded,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 44.dp),
            ) {
                PetQuickMenu(
                    isSleeping = snapshot.state == PetState.Sleeping,
                    isPositionLocked = preferences.positionLocked,
                    onFeed = { menuExpanded = false; actions.onFeed() },
                    onPlay = { menuExpanded = false; actions.onPlay() },
                    onToggleSleep = { menuExpanded = false; actions.onToggleSleep() },
                    onHide = { menuExpanded = false; actions.onHideTemporarily() },
                    onToggleLock = { menuExpanded = false; actions.onTogglePositionLock() },
                    onOpenApp = { menuExpanded = false; actions.onOpenApp() },
                    onClose = { menuExpanded = false },
                )
            }
        }
    }
}

@Composable
private fun PetQuickMenu(
    isSleeping: Boolean,
    isPositionLocked: Boolean,
    onFeed: () -> Unit,
    onPlay: () -> Unit,
    onToggleSleep: () -> Unit,
    onHide: () -> Unit,
    onToggleLock: () -> Unit,
    onOpenApp: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
    ) {
        Row(modifier = Modifier.padding(6.dp)) {
            QuickMenuIcon(Icons.Filled.Restaurant, "Feed", onFeed)
            QuickMenuIcon(Icons.Filled.PlayArrow, "Play", onPlay)
            QuickMenuIcon(
                icon = if (isSleeping) Icons.Filled.WbSunny else Icons.Filled.Bedtime,
                contentDescription = if (isSleeping) "Wake up" else "Sleep",
                onClick = onToggleSleep,
            )
            QuickMenuIcon(Icons.Filled.VisibilityOff, "Hide", onHide)
            QuickMenuIcon(
                icon = if (isPositionLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                contentDescription = if (isPositionLocked) "Unlock position" else "Lock position",
                onClick = onToggleLock,
            )
            QuickMenuIcon(Icons.Filled.OpenInNew, "Open app", onOpenApp)
            QuickMenuIcon(Icons.Filled.Close, "Close menu", onClose)
        }
    }
}

@Composable
private fun QuickMenuIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}
