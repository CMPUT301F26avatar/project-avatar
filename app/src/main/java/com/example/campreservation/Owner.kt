package com.example.campreservation.model

import com.example.campreservation.Campsite

interface Owner {
    val ownerId: String
    fun loadCampsites()
    fun registerCampsite(details: CampsiteDetails, onDone: (Result<Campsite>) -> Unit = {})
    fun updateCampsite(campsiteId: String, details: CampsiteDetails, onDone: (Result<Campsite>) -> Unit = {})
}