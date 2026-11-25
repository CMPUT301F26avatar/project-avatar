package com.example.campreservation

import java.util.UUID
import java.util.concurrent.TimeUnit

data class Reservation(
    val reservationId: String = UUID.randomUUID().toString(),
    val campsiteId: String = "",
    val ownerId: String = "",
    val deviceId: String = "",
    val startDate: Long = 0,
    val endDate: Long = 0,
    val guests: Int = 1,
    val totalPrice: Double = 0.0,
    val status: String = STATUS_CONFIRMED,
    val createdAt: Long = System.currentTimeMillis()
){

    fun validationError(): String? = when{
        campsiteId.isBlank() -> "Campsite is required"
        deviceId.isBlank() -> "Entrant is required"
        startDate <= 0 || endDate <= 0 -> "Dates are required"
        endDate <= startDate -> "Check-out date must be after check-in date"
        guests < 1 -> "At least one guest is required for reservation"
        else -> null
    }

    fun isActive() = status == STATUS_CONFIRMED
    fun isCancelled() = status == STATUS_CANCELLED
    fun isOwnedBy(userId: String) = deviceId == userId

    fun overlaps(start: Long, end: Long): Boolean = startDate< end && start< endDate

    companion object {
        const val STATUS_CONFIRMED = "CONFIRMED"
        const val STATUS_CANCELLED = "CANCELLED"

        fun forCampsite(
            campsite: Campsite,
            deviceId: String,
            startDate: Long,
            endDate: Long,
            guests: Int
        ):  Reservation {
            val nights = maxOf(TimeUnit.MILLISECONDS.toDays(endDate - startDate), 0L)
            return Reservation(
                campsiteId = campsite.campsiteId,
                ownerId = campsite.ownerId,
                deviceId = deviceId,
                startDate = startDate,
                endDate = endDate,
                guests = guests,
                totalPrice = nights * campsite.pricePerNight
            )
        }
    }
}