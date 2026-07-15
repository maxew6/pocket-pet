package com.pocketpet.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

/**
 * A short, contextual speech bubble. Pocket Pet never shows open-ended text here — [text] always
 * comes from the small curated pool in `core:domain`'s `SpeechLinePicker`.
 */
@Composable
fun PetSpeechBubble(text: String, visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 4.dp,
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
            Canvas(modifier = Modifier.size(width = 14.dp, height = 7.dp)) {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2f, size.height)
                    close()
                }
                drawPath(path, color = Color(0xFFF3E4CE))
            }
        }
    }
}

/** Position hint for [PetSpeechBubble] relative to the pet's bounding box, used by the overlay. */
data class SpeechBubbleAnchor(val xDp: Float, val yDp: Float)

/** Computes where a speech bubble should sit: centered above the pet with a small gap. */
fun speechBubbleAnchorAbove(petTopLeftXDp: Float, petTopLeftYDp: Float, petSizeDp: Float): SpeechBubbleAnchor =
    SpeechBubbleAnchor(
        xDp = petTopLeftXDp + petSizeDp / 2f,
        yDp = petTopLeftYDp - 8f,
    )
