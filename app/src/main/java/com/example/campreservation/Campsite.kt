package com.example.campreservation
import java.util.UUID

data class Campsite ( val campsiteId: String = UUID.randomUUID().toString(), //easy for firebase
    val ownerId: String = "", //campsiteOwner has id, easy for firebase
    val campName: String = "",
    val descr: String = "",
    val location: String = "",          // placeholder until Location class exists
    val maxOccupancy: Int = 1,
    val pricePerNight: Double = 0.0,
    val rules: String = "",
    val cancellationPolicy: String = "",
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null        //url instead of image class/may change
    ) {
        // Used by the registration form before saving (US 02.01.01). Should be written into
        fun validationError(): String? = when {
            campName.isBlank() -> "Name is required"
            location.isBlank() -> "Location is required"
            maxOccupancy <= 0 -> "Max occupancy must be at least 1"
            pricePerNight < 0 -> "Price can't be negative"
            else -> null
        }

        // Used by search/filter (US 01.01.01, 01.01.02)
        fun matches(query: String, guests: Int? = null): Boolean {
            val q = query.trim().lowercase()
            val textMatch = q.isEmpty() ||
                    campName.lowercase().contains(q) ||
                    location.lowercase().contains(q) ||
                    tags.any { it.lowercase().contains(q) }
            val guestMatch = guests == null || guests <= maxOccupancy
            return textMatch && guestMatch
        }
    }