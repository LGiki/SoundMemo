package net.lgiki.soundmemo.ui.library

import org.junit.Assert.assertEquals
import org.junit.Test

class RecycleBinRetentionTest {
    private val day = 24L * 60L * 60L * 1_000L

    @Test
    fun remainingRecycleBinDays_startsAtRetentionPeriod() {
        assertEquals(
            30,
            remainingRecycleBinDays(
                deletedAt = 1_000L,
                retentionDays = 30,
                nowMillis = 1_000L,
            ),
        )
    }

    @Test
    fun remainingRecycleBinDays_roundsPartialDaysUp() {
        assertEquals(
            2,
            remainingRecycleBinDays(
                deletedAt = 0L,
                retentionDays = 30,
                nowMillis = 28L * day + 1L,
            ),
        )
    }

    @Test
    fun remainingRecycleBinDays_isZeroWhenRetentionPeriodElapsed() {
        assertEquals(
            0,
            remainingRecycleBinDays(
                deletedAt = 0L,
                retentionDays = 30,
                nowMillis = 30L * day,
            ),
        )
    }

    @Test
    fun isNearPermanentDeletion_includesThreeDaysAndScheduledItems() {
        assertEquals(true, isNearPermanentDeletion(3))
        assertEquals(true, isNearPermanentDeletion(0))
        assertEquals(false, isNearPermanentDeletion(4))
    }
}
