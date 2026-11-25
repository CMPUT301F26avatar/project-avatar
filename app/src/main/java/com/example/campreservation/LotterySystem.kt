package com.example.campreservation

import kotlin.random.Random

enum class LotteryOutcome { WON, LOST }

class LotterySystem(private val random: Random = Random.Default) {
    /** Samples entrant IDs without replacement. Each distinct ID gets one chance. */
    fun draw(lottery: Lottery, entrantIds: Collection<String>, now: Long): Map<String, LotteryOutcome> {
        check(!lottery.drawn) { "This lottery has already been drawn" }
        check(now >= lottery.registrationClosesAt) { "Registration is still open" }
        require(lottery.winnerCount > 0) { "A lottery needs at least one winner slot" }
        val pool = entrantIds.distinct().shuffled(random)
        return pool.mapIndexed { index, id ->
            id to if (index < lottery.winnerCount) LotteryOutcome.WON else LotteryOutcome.LOST
        }.toMap()
    }
}
