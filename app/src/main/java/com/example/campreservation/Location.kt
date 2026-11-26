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

    // Makes the Location of a campsite: its address text plus its pin, if it has 
    constructor(campsite: Campsite) : this(campsite.location, campsite.latitude, campsite.longitude)

    // Returns true if the owner placed a pin on the map.
    fun hasCoordinates(): Boolean {
        return latitude != null && longitude != null
    }

    /**
     * Returns the distance between this location object and other location in kilometers, or null if either place has no pin.
     * Uses the haversine formula, the standard way to measure distance on a globe.
     */
    fun distanceKmTo(other: Location): Double? {
        // Both places need a pin
        if (latitude == null || longitude == null) {
            return null
        }
        if (other.latitude == null || other.longitude == null) {
            return null
        }

        // The parameters of the functions, value in radians
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)
        val latDifference = Math.toRadians(other.latitude - latitude)
        val lngDifference = Math.toRadians(other.longitude - longitude)

        // Haversine formula
        val sinHalfLat = sin(latDifference / 2)
        val sinHalfLng = sin(lngDifference / 2)
        val a = sinHalfLat * sinHalfLat + cos(lat1) * cos(lat2) * sinHalfLng * sinHalfLng

        val earthRadiusKm = 6371.0
        return 2 * earthRadiusKm * asin(sqrt(a))
    }

    // Location filtering: true if this place is within [km] of [other].
    fun isWithinKm(other: Location, km: Double): Boolean {
        val distance = distanceKmTo(other)
        if (distance == null) {
            return false   // one of the places has no pin
        }
        return distance <= km
    }
}