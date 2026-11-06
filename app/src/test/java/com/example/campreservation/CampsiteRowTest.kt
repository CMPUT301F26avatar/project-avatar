package com.example.campreservation

import org.junit.Assert.*
import org.junit.Test

class CampsiteRowTest {
    @Test fun toRow_mapsFields() {
        val row = Campsite(
            campsiteId = "c1", campName = "Lakeview",
            location = "Banff", maxOccupancy = 6
        ).toRow()
        assertEquals("c1", row.id)
        assertEquals("Lakeview", row.name)
        assertEquals("Banff", row.locationSummary)
        assertEquals(6, row.maxOccupancy)
        assertNull(row.imageUrl)
        assertEquals(0.0, row.averageRating, 0.0) // update when real ratings are wired in
    }
}