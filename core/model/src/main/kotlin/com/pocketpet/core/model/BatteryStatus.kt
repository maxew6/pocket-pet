package com.pocketpet.core.model

/** A point-in-time read of the device's battery, as reported by Android's battery broadcast. */
data class BatteryStatus(
    val percent: Int,
    val isCharging: Boolean,
    val isFull: Boolean,
) {
    init {
        require(percent in 0..100) { "percent must be in [0,100], was $percent" }
    }

    /**
     * The mood-relevant battery band for this reading.
     *
     * The bands are not symmetric around a single comparison direction: "80% or above" is happy
     * (an upper bound), while "30% or below" is concerned (a lower bound). This encodes both
     * directions explicitly instead of forcing one comparison operator on every threshold.
     */
    val currentMilestone: BatteryMilestone
        get() = when {
            isFull || percent >= 100 -> BatteryMilestone.Full
            percent <= 10 -> BatteryMilestone.Emergency
            percent <= 20 -> BatteryMilestone.Critical
            percent <= 30 -> BatteryMilestone.Low
            percent >= 80 -> BatteryMilestone.High
            else -> BatteryMilestone.Normal
        }

    companion object {
        val Unknown = BatteryStatus(percent = 50, isCharging = false, isFull = false)
    }
}
