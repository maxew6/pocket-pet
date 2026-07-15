package com.pocketpet.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ScreenBoundsTest {

    private val bounds = ScreenBounds(
        widthDp = 400f,
        heightDp = 800f,
        topInsetDp = 24f,
        bottomInsetDp = 32f,
        leftInsetDp = 0f,
        rightInsetDp = 0f,
    )

    @Test
    fun `a position inside the safe area is left untouched`() {
        val inside = PetPosition(xDp = 100f, yDp = 200f)
        assertThat(bounds.clamp(inside, petSizeDp = 64f)).isEqualTo(inside)
    }

    @Test
    fun `a position above the status bar is pulled down into the safe area`() {
        val aboveStatusBar = PetPosition(xDp = 100f, yDp = 0f)
        val clamped = bounds.clamp(aboveStatusBar, petSizeDp = 64f)
        assertThat(clamped.yDp).isEqualTo(24f)
    }

    @Test
    fun `a position past the right edge is pulled back so the pet stays fully visible`() {
        val pastRightEdge = PetPosition(xDp = 390f, yDp = 200f)
        val clamped = bounds.clamp(pastRightEdge, petSizeDp = 64f)
        assertThat(clamped.xDp).isEqualTo(400f - 64f)
    }

    @Test
    fun `a position below the nav bar is pulled up into the safe area`() {
        val belowNavBar = PetPosition(xDp = 100f, yDp = 790f)
        val clamped = bounds.clamp(belowNavBar, petSizeDp = 64f)
        assertThat(clamped.yDp).isEqualTo(800f - 32f - 64f)
    }
}

class QuietHoursTest {

    @Test
    fun `a same-day window contains hours strictly inside it`() {
        val window = QuietHours(enabled = true, startHour = 9, endHour = 17)
        assertThat(window.contains(12)).isTrue()
        assertThat(window.contains(8)).isFalse()
        assertThat(window.contains(17)).isFalse() // end hour is exclusive
    }

    @Test
    fun `an overnight window wraps past midnight correctly`() {
        val overnight = QuietHours(enabled = true, startHour = 22, endHour = 7)
        assertThat(overnight.contains(23)).isTrue()
        assertThat(overnight.contains(2)).isTrue()
        assertThat(overnight.contains(12)).isFalse()
        assertThat(overnight.contains(7)).isFalse() // end hour is exclusive
    }

    @Test
    fun `a disabled window never contains any hour`() {
        val disabled = QuietHours(enabled = false, startHour = 22, endHour = 7)
        assertThat(disabled.contains(23)).isFalse()
        assertThat(disabled.contains(12)).isFalse()
    }
}

class BatteryStatusTest {

    @Test
    fun `100 percent is Full`() {
        assertThat(BatteryStatus(100, isCharging = false, isFull = true).currentMilestone)
            .isEqualTo(BatteryMilestone.Full)
    }

    @Test
    fun `85 percent is High`() {
        assertThat(BatteryStatus(85, isCharging = false, isFull = false).currentMilestone)
            .isEqualTo(BatteryMilestone.High)
    }

    @Test
    fun `55 percent is Normal`() {
        assertThat(BatteryStatus(55, isCharging = false, isFull = false).currentMilestone)
            .isEqualTo(BatteryMilestone.Normal)
    }

    @Test
    fun `25 percent is Low`() {
        assertThat(BatteryStatus(25, isCharging = false, isFull = false).currentMilestone)
            .isEqualTo(BatteryMilestone.Low)
    }

    @Test
    fun `18 percent is Critical`() {
        assertThat(BatteryStatus(18, isCharging = false, isFull = false).currentMilestone)
            .isEqualTo(BatteryMilestone.Critical)
    }

    @Test
    fun `5 percent is Emergency`() {
        assertThat(BatteryStatus(5, isCharging = false, isFull = false).currentMilestone)
            .isEqualTo(BatteryMilestone.Emergency)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an out-of-range percent is rejected`() {
        BatteryStatus(101, isCharging = false, isFull = false)
    }
}
