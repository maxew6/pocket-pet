package com.pocketpet.core.domain.testutil

import com.pocketpet.core.domain.provider.ClockProvider

/** A [ClockProvider] fully controlled by the test — no relationship to real wall-clock time. */
class FakeClockProvider(
    private var epochMillis: Long = 1_700_000_000_000L, // arbitrary fixed instant
    private var hourOfDay: Int = 12,
) : ClockProvider {
    override fun nowEpochMillis(): Long = epochMillis
    override fun currentHourOfDay(): Int = hourOfDay

    fun set(epochMillis: Long) {
        this.epochMillis = epochMillis
    }

    fun setHour(hour: Int) {
        require(hour in 0..23)
        hourOfDay = hour
    }

    fun advanceBy(millis: Long) {
        epochMillis += millis
    }
}
