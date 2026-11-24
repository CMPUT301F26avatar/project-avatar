package com.example.campreservation

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LotteryTest {
    private val zone = TimeZone.getTimeZone("America/Edmonton")

    @Test fun oneDayBookingPeriodClosesRegistrationAtLocalMidnight() {
        val lottery = Lottery.create("test", "2028-02-29", "2028-02-29", 3, zone, 0)
        val midnight = java.time.Instant.parse("2028-02-29T07:00:00Z").toEpochMilli()
        assertEquals(3, lottery.winnerCount)
        assertEquals(midnight, lottery.registrationClosesAt)
        assertTrue(lottery.acceptsEntries(midnight - 1))
        assertFalse(lottery.acceptsEntries(midnight))
    }

    @Test fun rejectsImpossibleDatesReversedRangesAndNonpositiveCounts() {
        for ((start, end, winners) in listOf(
            Triple("2027-02-29", "2027-03-01", 1),
            Triple("2027-03-01", "2027-02-28", 1),
            Triple("2027-03-01", "2027-03-02", 0)
        )) {
            assertThrows(IllegalArgumentException::class.java) {
                Lottery.create("test", start, end, winners, zone, 0)
            }
        }
    }
}
