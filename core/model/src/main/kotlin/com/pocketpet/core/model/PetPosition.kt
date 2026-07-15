package com.pocketpet.core.model

/** Top-left position of the pet's bounding box, in density-independent pixels from origin. */
data class PetPosition(
    val xDp: Float,
    val yDp: Float,
)

/**
 * The safe area the pet is allowed to occupy: the display size minus system insets (status bar,
 * navigation bar, display cutouts). Recomputed by the overlay service on every configuration
 * change (rotation, inset change) and fed into the behavior engine so it never plans a move that
 * would leave the pet under a cutout or the nav bar.
 */
data class ScreenBounds(
    val widthDp: Float,
    val heightDp: Float,
    val topInsetDp: Float = 0f,
    val bottomInsetDp: Float = 0f,
    val leftInsetDp: Float = 0f,
    val rightInsetDp: Float = 0f,
) {
    val safeLeft: Float get() = leftInsetDp
    val safeTop: Float get() = topInsetDp
    val safeRight: Float get() = widthDp - rightInsetDp
    val safeBottom: Float get() = heightDp - bottomInsetDp

    /** Clamps a candidate top-left [position] (for a pet of [petSizeDp]) inside the safe area. */
    fun clamp(position: PetPosition, petSizeDp: Float): PetPosition = PetPosition(
        xDp = position.xDp.coerceIn(safeLeft, (safeRight - petSizeDp).coerceAtLeast(safeLeft)),
        yDp = position.yDp.coerceIn(safeTop, (safeBottom - petSizeDp).coerceAtLeast(safeTop)),
    )
}
