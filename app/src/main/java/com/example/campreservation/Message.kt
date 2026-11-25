package com.example.campreservation

import java.util.UUID

data class Message(
    val messageId: String = UUID.randomUUID().toString(),
    val senderId: String = "",
    val recipientId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)