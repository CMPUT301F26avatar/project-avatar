/*
 * CampsiteOwner.kt
 *
 * Purpose: the MODEL for the campsite owner side (CRC card: CampsiteOwner). It stores the owner's state
 * (campsites, lotteries, the selected campsite/lottery, form errors), enforces the business rules, saves
 * through the repositories, and notifies its registered views after every change.
 *
 * MVC roles (see the "MVC Responsibility Checklist" in the course notes)
 *  - Model: what data is stored, what rules protect it, which methods change it, who notifies the views.
 *  - It never touches Android UI types, never knows which view is showing it, and never knows the controller.
 *
 * Collaborators: Campsite, Location, Lottery (drawn by LotterySystem, not here), Reservation, WaitlistEntry,
 * CampsiteRepository, LotteryRepository, ReservationRepository, CampsiteQr.
 */
package com.example.campreservation.model

import com.example.campreservation.Campsite
import com.example.campreservation.CampsiteRepository
import com.example.campreservation.data.CampsiteRepository
import com.example.campreservation.data.LotteryRepository
import com.example.campreservation.data.ReservationRepository
import com.example.campreservation.mvc.TModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlin.collections.isNotEmpty

/**
 * A person or organization that lists campsites and runs lotteries for them.
 *
 * Views register with [addView] and are told to update after every change. Methods that save data are
 * asynchronous: they return at once, change the state when the save succeeds, notify the views, and then
 * call [onDone] (if given) with the outcome.
 *
 * @property ownerId device id of the owner
 * @property displayName name shown to entrants
 */
class CampsiteOwner(
    override val ownerId: String,
    val displayName: String,
    private val campsites: CampsiteRepository,
    //private val lotteries: LotteryRepository,
    //private val reservations: ReservationRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : TModel<CampsiteOwner>(), Owner {

    // ---- state shown by the views -----------------------------------------------------------------------

    private val _ownedCampsites = mutableListOf<Campsite>()

    /** Read-only snapshot of this owner's campsites, sorted by name (US 02.03.01). */
    val ownedCampsites: List<Campsite> get() = _ownedCampsites.toList()

    //private val _ownedLotteries = mutableListOf<Lottery>()

    /** Read-only snapshot of this owner's lotteries, newest registration first. */
    //val ownedLotteries: List<Lottery> get() = _ownedLotteries.toList()

    /** The campsite opened on the overview screen (US 02.01.04), or null. */
    var selectedCampsite: Campsite? = null
        private set

    /** The lottery opened on the manage screen, or null. */
    //var selectedLottery: Lottery? = null
        //private set

    //private var allReservations: List<Reservation> = emptyList()
    private var _waitlist: List<WaitlistEntry> = emptyList()

    /** Entrants on the selected lottery's waiting list, in the order they joined (US 02.09.01). */
    val waitlist: List<WaitlistEntry> get() = _waitlist

    /** Problems with the form the owner last submitted, one per field problem. */
    var errors: List<ValidationError> = emptyList()
        private set

    /** One-off message (success or unexpected failure) for the views to show. */
    var message: String? = null
        private set

    /** The text inside the selected campsite's QR code (US 02.02.01). */
    val selectedQrPayload: String? get() = selectedCampsite?.let { CampsiteQr.payload(it.id) }

    /** Nights already taken at the selected campsite by confirmed reservations (US 02.06.01). */
    //val bookedDates: Set<LocalDate>
        //get() = allReservations.filter { it.status == ReservationStatus.CONFIRMED }
            //.flatMap { nightsOf(it) }.toSet()

    /** Reservations of the selected campsite for one list: active, past or cancelled (US 02.07.01 to .03). */
//    fun reservations(filter: ReservationFilter): List<Reservation> {
//        val today = LocalDate.now(clock)
//        return when (filter) {
//            ReservationFilter.ACTIVE -> allReservations.filter {
//                it.status == ReservationStatus.CONFIRMED && !it.checkOut.isBefore(today)
//            }
//            ReservationFilter.PAST -> allReservations.filter {
//                it.status == ReservationStatus.CONFIRMED && it.checkOut.isBefore(today)
//            }
//            ReservationFilter.CANCELLED -> allReservations.filter { it.status != ReservationStatus.CONFIRMED }
//        }.sortedBy { it.checkIn }
//    }

    /** All three reservation lists at once, so a view can copy them in one go. */
//    fun reservationsByFilter(): Map<ReservationFilter, List<Reservation>> =
//        ReservationFilter.values().associateWith { reservations(it) }

    // ---- loading ----------------------------------------------------------------------------------------

    /** Loads this owner's campsites from the repository into [ownedCampsites]. */
    override fun loadCampsites() {
        campsites.byOwner(ownerId) { result ->
            result.onSuccess { list ->
                _ownedCampsites.clear()
                _ownedCampsites.addAll(list.sortedBy { it.details.name.lowercase() })
                notifyViews(this)
            }.onFailure { reportFailure(it) }
        }
    }

    /** Loads this owner's lotteries from the repository into [ownedLotteries]. */
//    fun loadLotteries() {
//        lotteries.byOwner(ownerId) { result ->
//            result.onSuccess { list ->
//                _ownedLotteries.clear()
//                _ownedLotteries.addAll(list.sortedByDescending { it.terms.registrationStart })
//                notifyViews(this)
//            }.onFailure { reportFailure(it) }
//        }
//    }

    // ---- campsites --------------------------------------------------------------------------------------

    /** Registers a new campsite owned by this owner (US 02.01.01, 02.01.02). */
    override fun registerCampsite(details: CampsiteDetails, onDone: (Result<Campsite>) -> Unit) {
        val problems = validateDetails(details)
        if (problems.isNotEmpty()) return fail(ValidationException(problems), onDone)
        val campsite = Campsite(campsites.newId(), ownerId, details)
        campsites.save(campsite) { saved ->
            saved.onSuccess {
                _ownedCampsites.add(campsite)
                _ownedCampsites.sortBy { it.details.name.lowercase() }
                succeed("Campsite saved")
                onDone(Result.success(campsite))
            }.onFailure { fail(it, onDone) }
        }
    }

    /** Replaces the details of one of this owner's campsites (US 02.01.03). */
    override fun updateCampsite(campsiteId: String, details: CampsiteDetails, onDone: (Result<Campsite>) -> Unit) {
        val existing = _ownedCampsites.firstOrNull { it.id == campsiteId }
            ?: return fail(NotOwnerException("Campsite $campsiteId is not one of your campsites"), onDone)
        val problems = validateDetails(details)
        if (problems.isNotEmpty()) return fail(ValidationException(problems), onDone)
        val updated = existing.copy(details = details)
        campsites.save(updated) { saved ->
            saved.onSuccess {
                _ownedCampsites[_ownedCampsites.indexOf(existing)] = updated
                _ownedCampsites.sortBy { it.details.name.lowercase() }
                if (selectedCampsite?.id == campsiteId) selectedCampsite = updated
                succeed("Campsite saved")
                onDone(Result.success(updated))
            }.onFailure { fail(it, onDone) }
        }
    }

    /** Opens one campsite: sets [selectedCampsite], then loads its reservations (US 02.01.04, 02.06.01, 02.07.x). */
    fun selectCampsite(campsiteId: String, onDone: (Result<Campsite>) -> Unit = {}) {
        val campsite = _ownedCampsites.firstOrNull { it.id == campsiteId }
            ?: return fail(NotOwnerException("Campsite $campsiteId is not one of your campsites"), onDone)
        selectedCampsite = campsite
        allReservations = emptyList()
        notifyViews(this)
        reservations.byCampsite(campsiteId) { result ->
            result.onSuccess { list ->
                allReservations = list
                notifyViews(this)
                onDone(Result.success(campsite))
            }.onFailure { fail(it, onDone) }
        }
    }

    // ---- lotteries --------------------------------------------------------------------------------------

    /** Creates a lottery listing for one or more of this owner's campsites (US 02.04.01 to .05, .07). */
//    fun createLottery(terms: LotteryTerms, onDone: (Result<Lottery>) -> Unit = {}) {
//        val problems = validateTerms(terms, existing = null)
//        if (problems.isNotEmpty()) return fail(ValidationException(problems), onDone)
//        val lottery = Lottery(lotteries.newId(), ownerId, terms, LotteryStatus.OPEN)
//        lotteries.save(lottery) { saved ->
//            saved.onSuccess {
//                _ownedLotteries.add(lottery)
//                _ownedLotteries.sortByDescending { it.terms.registrationStart }
//                succeed("Lottery saved")
//                onDone(Result.success(lottery))
//            }.onFailure { fail(it, onDone) }
//        }
//    }

    /** Changes the terms of a lottery that has not been drawn yet (US 02.04.06). */
//    fun updateLottery(lotteryId: String, terms: LotteryTerms, onDone: (Result<Lottery>) -> Unit = {}) {
//        val existing = _ownedLotteries.firstOrNull { it.id == lotteryId }
//            ?: return fail(NotOwnerException("Lottery $lotteryId is not one of your lotteries"), onDone)
//        if (existing.status != LotteryStatus.OPEN) {
//            return fail(LotteryLockedException("Lottery $lotteryId was already drawn and cannot change"), onDone)
//        }
//        lotteries.waitlist(lotteryId) { waitlistResult ->
//            val onList = waitlistResult.getOrNull()?.size ?: 0
//            val problems = validateTerms(terms, existing.terms).toMutableList()
//            val cap = terms.waitlistCapacity
//            if (cap != null && cap < onList) {
//                problems += ValidationError("waitlistCapacity", "Already $onList entrants on the waiting list")
//            }
//            if (problems.isNotEmpty()) return@waitlist fail(ValidationException(problems), onDone)
//            val updated = existing.copy(terms = terms)
//            lotteries.save(updated) { saved ->
//                saved.onSuccess {
//                    _ownedLotteries[_ownedLotteries.indexOf(existing)] = updated
//                    _ownedLotteries.sortByDescending { it.terms.registrationStart }
//                    if (selectedLottery?.id == lotteryId) selectedLottery = updated
//                    succeed("Lottery saved")
//                    onDone(Result.success(updated))
//                }.onFailure { fail(it, onDone) }
//            }
//        }
//    }

    /** Opens one lottery: sets [selectedLottery], then loads its waiting list (US 02.09.01). */
//    fun selectLottery(lotteryId: String, onDone: (Result<Lottery>) -> Unit = {}) {
//        val lottery = _ownedLotteries.firstOrNull { it.id == lotteryId }
//            ?: return fail(NotOwnerException("Lottery $lotteryId is not one of your lotteries"), onDone)
//        selectedLottery = lottery
//        _waitlist = emptyList()
//        notifyViews(this)
//        lotteries.waitlist(lotteryId) { result ->
//            result.onSuccess { list ->
//                _waitlist = list.sortedBy { it.joinedAt }
//                notifyViews(this)
//                onDone(Result.success(lottery))
//            }.onFailure { fail(it, onDone) }
//        }
//    }

    // ---- messages and errors ----------------------------------------------------------------------------

    /** Records input problems found by the controller (e.g. a field that is not a number) and notifies views. */
    fun reportInputProblems(problems: List<ValidationError>) {
        errors = problems
        message = null
        notifyViews(this)
    }

    /** Clears form errors, for example when the owner leaves a form. */
    fun clearErrors() {
        errors = emptyList()
        notifyViews(this)
    }

    /** Clears the one-off message once a view has shown it. */
    fun clearMessage() {
        message = null
        notifyViews(this)
    }

    // ---- helpers ----------------------------------------------------------------------------------------

    /** Nights a reservation occupies: check-in up to, but not including, check-out. */
    private fun nightsOf(r: Reservation): List<LocalDate> =
        generateSequence(r.checkIn) { it.plusDays(1) }.takeWhile { it.isBefore(r.checkOut) }.toList()

    private fun succeed(text: String) {
        errors = emptyList()
        message = text
        notifyViews(this)
    }

    private fun reportFailure(e: Throwable) {
        if (e is ValidationException) {
            errors = e.errors
            message = null
        } else {
            message = e.message ?: "Something went wrong"
        }
        notifyViews(this)
    }

    private fun <T> fail(e: Throwable, onDone: (Result<T>) -> Unit) {
        reportFailure(e)
        onDone(Result.failure(e))
    }

    private fun validateDetails(d: CampsiteDetails): List<ValidationError> = buildList {
        if (d.name.isBlank()) add(ValidationError("name", "Name is required"))
        if (d.description.isBlank()) add(ValidationError("description", "Describe the amenities"))
        if (d.maxOccupancy < 1) add(ValidationError("maxOccupancy", "Must be at least 1"))
        if (d.pricePerNight < 0) add(ValidationError("pricePerNight", "Cannot be negative"))
        if (d.location.address.isBlank()) add(ValidationError("location", "Address is required"))
        if (d.location.latitude !in -90.0..90.0 || d.location.longitude !in -180.0..180.0) {
            add(ValidationError("location", "Map location is out of range"))
        }
    }

    /**
     * Rules for lottery terms. [existing] is the stored lottery when updating, null when creating.
     * Once registration has opened, its start time cannot move.
     */
    private fun validateTerms(t: LotteryTerms, existing: LotteryTerms?): List<ValidationError> = buildList {
        val now = Instant.now(clock)
        if (t.campsiteIds.isEmpty()) add(ValidationError("campsiteIds", "Pick at least one campsite"))
        t.campsiteIds.filter { id -> _ownedCampsites.none { it.id == id } }.forEach {
            add(ValidationError("campsiteIds", "Campsite $it is not one of your campsites"))
        }
        if (t.stayEnd.isBefore(t.stayStart)) add(ValidationError("stay", "Stay end is before stay start"))
        if (!t.registrationEnd.isAfter(t.registrationStart)) {
            add(ValidationError("registration", "Registration must end after it starts"))
        }
        if (t.bookingStart.isBefore(t.registrationEnd)) {
            add(ValidationError("booking", "Booking cannot start before registration ends"))
        }
        if (!t.bookingEnd.isAfter(t.bookingStart)) add(ValidationError("booking", "Booking must end after it starts"))
        if (t.winnerCount < 1) add(ValidationError("winnerCount", "At least 1 winner"))
        val cap = t.waitlistCapacity
        if (cap != null && cap < t.winnerCount) {
            add(ValidationError("waitlistCapacity", "Capacity cannot be smaller than the number of winners"))
        }
        if (existing == null && !t.registrationEnd.isAfter(now)) {
            add(ValidationError("registration", "Registration must end in the future"))
        }
        if (existing != null && !now.isBefore(existing.registrationStart) && t.registrationStart != existing.registrationStart) {
            add(ValidationError("registration", "Registration already opened; its start cannot change"))
        }
    }
}