package com.example.campreservation

import java.util.UUID

// Defaults on every field so Firestore can build it with toObjects()
data class Notification(
    val notificationId: String = UUID.randomUUID().toString(),
    val recipientId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "GENERAL",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)