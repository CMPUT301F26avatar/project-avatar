/*
 * OwnerController.kt
 *
 * Purpose: the CONTROLLER for the owner side. The view hands it what the user did (a button click, a
 * submitted form); the controller checks the raw input (is this text a number? a date?), then calls the
 * right CampsiteOwner method to modify the model. It never draws anything and never keeps display state.
 *
 * MVC roles: "What happens when the user clicks or types?" and "Which model method is called for this
 * action?" belong here. Business rules (is this occupancy allowed?) stay in the model.
 *
 * Parameterless actions are exposed as TCommand values (view calls controller.refresh.execute());
 * actions that carry form data are ordinary functions.
 *
 */
package com.example.campreservation.controller

import com.example.campreservation.model.CampsiteDetails
import com.example.campreservation.model.CampsiteOwner
import com.example.campreservation.model.Location
import com.example.campreservation.model.LotteryTerms
import com.example.campreservation.model.ValidationError
import com.example.campreservation.model.ValidationException
import com.example.campreservation.mvc.TCommand
import java.time.LocalDate
import java.time.ZoneId

/** Raw text of the register/edit campsite form, exactly as typed. */
data class CampsiteForm(
    val name: String,
    val address: String,
    val latitude: String,
    val longitude: String,
    val maxOccupancy: String,
    val pricePerNight: String,
    val description: String,
    val tags: String,
    val rules: String,
    val cancellationPolicy: String,
    val promoImageUrl: String? = null,
) {
    /** Turns the typed text into [CampsiteDetails], or reports every field that is not a valid number. */
    fun toDetails(): Result<CampsiteDetails> {
        val problems = mutableListOf<ValidationError>()
        val occupancy = maxOccupancy.trim().toIntOrNull()
            ?: 0.also { problems += ValidationError("maxOccupancy", "Enter a whole number") }
        val price = pricePerNight.trim().toDoubleOrNull()
            ?: 0.0.also { problems += ValidationError("pricePerNight", "Enter a price like 65 or 65.50") }
        val lat = latitude.trim().toDoubleOrNull()
        val lon = longitude.trim().toDoubleOrNull()
        if (lat == null || lon == null) problems += ValidationError("location", "Enter a latitude and a longitude")
        if (problems.isNotEmpty()) return Result.failure(ValidationException(problems))
        return Result.success(
            CampsiteDetails(
                name = name.trim(),
                description = description.trim(),
                maxOccupancy = occupancy,
                pricePerNight = price,
                location = Location(address.trim(), lat!!, lon!!),
                tags = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                rules = rules.trim(),
                cancellationPolicy = cancellationPolicy.trim(),
                promoImageUrl = promoImageUrl,
            ),
        )
    }
}

/** Raw text of the create/edit lottery form. Dates are yyyy-MM-dd. */
data class LotteryForm(
    val campsiteIds: Set<String>,
    val stayStart: String,
    val stayEnd: String,
    val registrationStart: String,
    val registrationEnd: String,
    val bookingEnd: String,
    val winnerCount: String,
    val waitlistCapacity: String,
) {
    /** Turns the typed text into [LotteryTerms], or reports every field that could not be read. */
    fun toTerms(zone: ZoneId): Result<LotteryTerms> {
        val problems = mutableListOf<ValidationError>()
        fun date(text: String, field: String, label: String): LocalDate? =
            runCatching { LocalDate.parse(text.trim()) }.getOrNull()
                .also { if (it == null) problems += ValidationError(field, "$label must look like 2027-05-01") }

        val stayFrom = date(stayStart, "stay", "Stay start")
        val stayTo = date(stayEnd, "stay", "Stay end")
        val regStart = date(registrationStart, "registration", "Registration start")
        val regEnd = date(registrationEnd, "registration", "Registration end")
        val bookEnd = date(bookingEnd, "booking", "Booking end")
        val winners = winnerCount.trim().toIntOrNull()
        if (winners == null) problems += ValidationError("winnerCount", "Enter a whole number")
        val cap = waitlistCapacity.trim().takeIf { it.isNotEmpty() }?.let {
            it.toIntOrNull().also { n -> if (n == null) problems += ValidationError("waitlistCapacity", "Enter a whole number or leave blank") }
        }
        if (problems.isNotEmpty()) return Result.failure(ValidationException(problems))

        val drawsAt = regEnd!!.atTime(OwnerController.DRAW_HOUR, 0).atZone(zone).toInstant()
        return Result.success(
            LotteryTerms(
                campsiteIds = campsiteIds,
                stayStart = stayFrom!!,
                stayEnd = stayTo!!,
                registrationStart = regStart!!.atStartOfDay(zone).toInstant(),
                registrationEnd = drawsAt,
                bookingStart = drawsAt,
                bookingEnd = bookEnd!!.atTime(23, 59).atZone(zone).toInstant(),
                winnerCount = winners!!,
                waitlistCapacity = cap,
            ),
        )
    }
}

class OwnerController(
    private val model: CampsiteOwner,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    /** Reloads the owner's campsites and lotteries (used when the owner screen opens). */
    val refresh = TCommand {
        model.loadCampsites()
        model.loadLotteries()
    }

    /** Clears form errors (used when the owner leaves a form). */
    val clearErrors = TCommand { model.clearErrors() }

    /** Tells the model the current message was shown. */
    val dismissMessage = TCommand { model.clearMessage() }

    /** Owner tapped "Manage" on a campsite. */
    fun openCampsite(campsiteId: String) = model.selectCampsite(campsiteId)

    /** Owner tapped a lottery. */
    fun openLottery(lotteryId: String) = model.selectLottery(lotteryId)

    /**
     * Owner submitted the campsite form. Registers a new campsite when [campsiteId] is null, otherwise updates
     * that campsite. [onSaved] runs only if the model accepted and saved it.
     */
    fun saveCampsite(form: CampsiteForm, campsiteId: String?, onSaved: () -> Unit) {
        form.toDetails()
            .onFailure { model.reportInputProblems((it as ValidationException).errors) }
            .onSuccess { details ->
                val done: (Result<*>) -> Unit = { if (it.isSuccess) onSaved() }
                if (campsiteId == null) model.registerCampsite(details) { done(it) }
                else model.updateCampsite(campsiteId, details) { done(it) }
            }
    }

    /**
     * Owner submitted the lottery form. Creates a lottery when [lotteryId] is null, otherwise updates it.
     * [onSaved] runs only if the model accepted and saved it.
     */
    fun saveLottery(form: LotteryForm, lotteryId: String?, onSaved: () -> Unit) {
        form.toTerms(zone)
            .onFailure { model.reportInputProblems((it as ValidationException).errors) }
            .onSuccess { terms ->
                val done: (Result<*>) -> Unit = { if (it.isSuccess) onSaved() }
                if (lotteryId == null) model.createLottery(terms) { done(it) }
                else model.updateLottery(lotteryId, terms) { done(it) }
            }
    }

    companion object {
        /** Local hour at which registration closes and the draw runs. */
        const val DRAW_HOUR = 9
    }
}