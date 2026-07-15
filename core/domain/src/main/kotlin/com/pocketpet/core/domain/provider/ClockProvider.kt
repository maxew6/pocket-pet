package com.pocketpet.core.domain.provider

/**
 * The only way any domain code reads the current time or wall-clock hour. Production code
 * injects [SystemClockProvider]; tests inject a fake that can be advanced deterministically —
 * see `FakeClockProvider` in the domain test sources.
 */
interface ClockProvider {
    /** Milliseconds since epoch, equivalent to `System.currentTimeMillis()`. */
    fun nowEpochMillis(): Long

    /** The local hour of day, 0-23, used for quiet-hours and time-of-day behavior. */
    fun currentHourOfDay(): Int
}

/** Real-time implementation backed by the JVM clock and the device's default time zone. */
class SystemClockProvider : ClockProvider {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()

    override fun currentHourOfDay(): Int {
        val calendar = java.util.Calendar.getInstance()
        return calendar.get(java.util.Calendar.HOUR_OF_DAY)
    }
}
