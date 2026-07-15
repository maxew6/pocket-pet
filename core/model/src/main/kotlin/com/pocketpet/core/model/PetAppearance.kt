package com.pocketpet.core.model

/** Everything a person can customize about how the pet looks and moves. */
data class PetAppearance(
    val name: String = "Mochi",
    val colorTone: ColorTone = ColorTone.Milk,
    val accessory: Accessory = Accessory.None,
    /** Uniform render scale; 1.0 is the reference size drawn by `PetRenderer`. */
    val scale: Float = 1f,
    /** Overlay opacity, 0 (invisible) to 1 (fully opaque). */
    val opacity: Float = 1f,
    /** Multiplier applied to every animation's duration; 1.0 is the reference speed. */
    val animationSpeedMultiplier: Float = 1f,
    val reducedMotion: Boolean = false,
) {
    init {
        require(scale in MIN_SCALE..MAX_SCALE) { "scale out of range: $scale" }
        require(opacity in 0f..1f) { "opacity must be in [0,1], was $opacity" }
        require(animationSpeedMultiplier in MIN_SPEED..MAX_SPEED) {
            "animationSpeedMultiplier out of range: $animationSpeedMultiplier"
        }
    }

    companion object {
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 1.75f
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 2f
    }
}
