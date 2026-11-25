package com.example.campreservation.controller

import com.example.campreservation.Campsite
import com.example.campreservation.CampsiteOwner
import com.example.campreservation.Lottery
//import com.example.campreservation.mvc.TCommand

class OwnerController(
    private val model: CampsiteOwner,
    //private val zone: ZoneId = ZoneId.systemDefault(),
) {
    fun refresh() = model.loadCampsites()
    fun clearErrors() = model.clearErrors()
    fun dismissMessage() = model.clearMessage()

    fun openCampsite(campsiteId: String) {
        model.selectCampsite(campsiteId) { result ->
            if (result.isSuccess) model.loadLotteries()
        }
    }

    /** [onSaved] runs only if the model accepted and saved it. */
    fun saveCampsite(draft: Campsite, campsiteId: String?, onSaved: () -> Unit) {
        val done: (Result<Campsite>) -> Unit = { if (it.isSuccess) onSaved() }
        if (campsiteId == null) model.registerCampsite(draft, done)
        else model.updateCampsite(campsiteId, draft, done)
    }

    fun loadLotteries() = model.loadLotteries()
    /** [onSaved] runs only if the model accepted and published it. */
    fun createLottery(start: String, end: String, winners: Int, onSaved: () -> Unit) {
        model.createLottery(start, end, winners) { if (it.isSuccess) onSaved() }
    }


}
