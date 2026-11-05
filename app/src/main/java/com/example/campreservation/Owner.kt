package com.example.campreservation.model

interface Owner {
    val ownerId: String
    fun loadCampsites()
    fun registerCampsite(details: CampsiteDetails, onDone: (Result<Campsite>) -> Unit = {})
    fun updateCampsite(campsiteId: String, details: CampsiteDetails, onDone: (Result<Campsite>) -> Unit = {})
}