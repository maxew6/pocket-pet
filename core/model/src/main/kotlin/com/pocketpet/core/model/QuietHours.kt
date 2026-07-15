package com.pocketpet.core.model

/**
 * A daily quiet window (e.g. 22:00–07:00) during which the pet suppresses speech bubbles and
 * non-essential animation, so it never lights up or "talks" while someone is asleep.
 *
 * [startHour] may be greater than [endHour] to represent a window that crosses midnight, which
 * is the common case for a nighttime quiet period.
 */
data class QuietHours(
    val enabled: Boolean = true,
    val startHour: Int = 22,
    val endHour: Int = 7,
) {
    init {
        require(startHour in 0..23) { "startHour must be in [0,23], was $startHour" }
        require(endHour in 0..23) { "endHour must be in [0,23], was $endHour" }
    }

    /** Whether [hourOfDay] (0-23, in the device's local time) falls inside this quiet window. */
    fun contains(hourOfDay: Int): Boolean {
        if (!enabled) return false
        return if (startHour <= endHour) {
            hourOfDay in startHour until endHour
        } else {
            hourOfDay >= startHour || hourOfDay < endHour
        }
    }
}
