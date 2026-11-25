package com.example.campreservation

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class Lottery(
    val lotteryId: String = "",
    val bookingStart: String = "",
    val bookingEnd: String = "",
    val winnerCount: Int = 0,
    val registrationClosesAt: Long = 0
) {
    fun acceptsEntries(now: Long) = now < registrationClosesAt

    companion object {
        fun create(
            id: String, start: String, end: String, winners: Int,
            zone: TimeZone = TimeZone.getDefault(), now: Long = System.currentTimeMillis()
        ): Lottery {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                isLenient = false
                timeZone = zone
            }
            fun parseDate(value: String): Long {
                require(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value)) {
                    "Enter dates as YYYY-MM-DD"
                }
                return requireNotNull(format.parse(value, java.text.ParsePosition(0))) {
                    "Enter a valid calendar date"
                }.time
            }
            val startTime = parseDate(start)
            require(parseDate(end) >= startTime) { "Booking end must be on or after booking start" }
            require(startTime > now) { "Choose a future booking start so entrants have time to join" }
            require(winners > 0) { "Enter at least one winner" }
            return Lottery(id, start, end, winners, startTime)
        }
    }
}
