package com.example.campreservation

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LotterySystemTest {
    private val lottery = Lottery(winnerCount = 2, registrationClosesAt = 100)
    private val entrants = listOf("a", "b", "a", "c", "d")

    @Test fun drawsDistinctWinnersAtClosingTimeAndRecordsEveryOutcome() {
        val outcomes = LotterySystem(Random(42)).draw(lottery, entrants, 100)
        assertEquals(entrants.toSet(), outcomes.keys)
        assertEquals(2, outcomes.values.count { it == LotteryOutcome.WON })
        assertEquals(2, outcomes.values.count { it == LotteryOutcome.LOST })
        assertEquals(outcomes, LotterySystem(Random(42)).draw(lottery, entrants, 100))
    }

    @Test fun handlesFewerEntrantsThanSlots() {
        val system = LotterySystem()
        assertEquals(mapOf("a" to LotteryOutcome.WON), system.draw(lottery, listOf("a"), 100))
        assertEquals(emptyMap<String, LotteryOutcome>(), system.draw(lottery, emptyList(), 100))
    }

    @Test fun rejectsEarlyAndRepeatedDraws() {
        val system = LotterySystem()
        assertThrows(IllegalStateException::class.java) { system.draw(lottery, entrants, 99) }
        assertThrows(IllegalStateException::class.java) { system.draw(lottery.copy(drawn = true), entrants, 101) }
    }
}
