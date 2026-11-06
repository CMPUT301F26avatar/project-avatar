package com.example.campreservation

import org.junit.Assert.*
import org.junit.Test

class CampsiteTest {
    private val camp = Campsite(
        campName = "Lakeview", location = "Banff",
        maxOccupancy = 6, tags = listOf("Hiking", "Pets")
    )

    @Test fun validationError_blankName() = assertEquals("Name is required", camp.copy(campName = " ").validationError())
    @Test fun validationError_blankLocation() = assertEquals("Location is required", camp.copy(location = "").validationError())
    @Test fun validationError_zeroOccupancy() = assertEquals("Max occupancy must be at least 1", camp.copy(maxOccupancy = 0).validationError())
    @Test fun validationError_negativePrice() = assertEquals("Price can't be negative", camp.copy(pricePerNight = -1.0).validationError())
    @Test fun validationError_validCampsite() = assertNull(camp.validationError())

    @Test fun matches_emptyQuery() = assertTrue(camp.matches(""))
    @Test fun matches_name_caseInsensitive() = assertTrue(camp.matches("LAKE"))
    @Test fun matches_location() = assertTrue(camp.matches("banff"))
    @Test fun matches_tag() = assertTrue(camp.matches("hik"))
    @Test fun matches_noMatch() = assertFalse(camp.matches("desert"))
    @Test fun matches_tooManyGuests() = assertFalse(camp.matches("", guests = 7))
    @Test fun matches_exactGuests() = assertTrue(camp.matches("", guests = 6))
}