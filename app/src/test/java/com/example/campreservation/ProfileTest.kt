package com.example.campreservation

import org.junit.Assert.*
import org.junit.Test

class ProfileTest {
    @Test fun updateInfo_setsFields() {
        val p = Profile()
        p.updateInfo("Sam", "sam@x.com", "780")
        assertEquals("Sam", p.name)
        assertEquals("sam@x.com", p.email)
        assertEquals("780", p.phoneNumber)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateInfo_blankName_throws() = Profile().updateInfo(" ", "a@b.com")

    @Test fun notificationPreference_toggles() {
        val p = Profile()
        p.notificationPreference(false)
        assertTrue(p.disableNotifications())
    }

    @Test fun removeImage_clearsUrl() {
        val p = Profile(profileImageUrl = "http://x/y.png")
        p.removeImage()
        assertNull(p.profileImageUrl)
    }
}