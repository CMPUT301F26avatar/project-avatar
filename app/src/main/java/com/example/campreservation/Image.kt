package com.example.campreservation

import java.util.UUID
data class Image(
    val imageId: String = UUID.randomUUID().toString(),
    val url: String = "",
    val storagePath: String = "",
    val uploaderId: String = "",
    val type: String = "CAMPSITE",
    val ownerRefId: String = "",
    val uploadedAt: Long = 0
) {
    fun isOwnedBy(userId: String) = uploaderId == userId
    fun isCampsiteImage() = type == "CAMPSITE"
    fun isProfileImage() = type == "PROFILE"
}