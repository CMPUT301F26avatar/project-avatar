package com.example.campreservation


import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Test

class EntrantTest {
    @Test fun init_copiesEntrantIdIntoBlankDeviceId() {
        val e = Entrant(entrantId = "dev1")
        assertEquals("dev1", e.profile.deviceId)
    }

    @Test fun init_keepsExistingDeviceId() {
        val e = Entrant(entrantId = "dev1", profile = Profile(deviceId = "other"))
        assertEquals("other", e.profile.deviceId)
    }

    @Test fun equals_isBasedOnEntrantIdOnly() {
        val a = Entrant("1", Profile(deviceId = "x"))
        val b = Entrant("1", Profile(deviceId = "y"))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test fun differentIds_notEqual() = assertNotEquals(Entrant("1"), Entrant("2"))

    @Test fun searchCampsites_delegatesToRepository() {
        val repo = mockk<CampsiteRepository>(relaxed = true)
        val callback: (Result<List<Campsite>>) -> Unit = {}
        Entrant("1").searchCampsites("lake", repo, 4, callback)
        verify(exactly = 1) { repo.search("lake", 4, callback) }
    }
}