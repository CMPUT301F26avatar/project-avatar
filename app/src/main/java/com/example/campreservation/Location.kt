package com.example.campreservation

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Location(
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
) {

    // Makes the Location of a campsite: its address text plus its pin, if it has one.
    constructor(campsite: Campsite) : this(campsite.location, campsite.latitude, campsite.longitude)

    // Returns true if the owner placed a pin on the map.
    fun hasCoordinates(): Boolean {
        return latitude != null && longitude != null
    }
}