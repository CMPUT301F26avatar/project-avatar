package com.example.campreservation

interface Owner {
    val ownerId: String
    fun loadCampsites()
    fun registerCampsite(details: CampsiteDetails): Result<Campsite>
    fun updateCampsite(campsiteId: String, details: CampsiteDetails): Result<Campsite>
}