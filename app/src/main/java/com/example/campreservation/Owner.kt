package com.example.campreservation.model

import com.example.campreservation.Campsite

interface Owner {
    val ownerId: String
    fun loadCampsites()
    fun registerCampsite(draft: Campsite, onDone: (Result<Campsite>) -> Unit = {})
    fun updateCampsite(campsiteId: String, draft: Campsite, onDone: (Result<Campsite>) -> Unit = {})
}