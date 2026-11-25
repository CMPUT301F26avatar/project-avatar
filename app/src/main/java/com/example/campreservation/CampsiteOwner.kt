/*
 * CampsiteOwner.kt
 *
 * Purpose: the MODEL for the campsite owner side (CRC card: CampsiteOwner). It stores the owner's state
 * (campsites, the selected campsite, form errors), enforces the business rules, saves through the
 * repository, and notifies its registered views after every change.
 *
 * MVC roles
 *  - Model: what data is stored, what rules protect it, which methods change it, who notifies the views.
 *  - It never touches Android UI types, never knows which view is showing it, and never knows the controller.
 *
 * Collaborators: Campsite, CampsiteRepository.
 * Lotteries, reservations and the waiting list come back later (they need their own classes first).
 */
package com.example.campreservation

import com.example.campreservation.model.Owner
import com.example.campreservation.Campsite
import com.example.campreservation.CampsiteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A person or organization that lists campsites.
 *
 * Views register with [addListener] and are told to update after every change. Methods that save data are
 * asynchronous: they return at once, change the state when the save succeeds, notify the views, and then
 * call [onDone] with the outcome.
 *
 * @property ownerId device id of the owner
 * @property displayName name shown to entrants
 */
class CampsiteOwner(
    override val ownerId: String,
    val displayName: String,
    private val campsites: CampsiteRepository,
    private val lotteryRepo: LotteryRepository = LotteryRepository(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
) : Owner {

    // ---- observers --------------------------------------------------------------------------------------

    private val listeners = mutableListOf<(CampsiteOwner) -> Unit>()

    /** Registers a view; it is called after every change to this model. */
    fun addListener(listener: (CampsiteOwner) -> Unit) {
        listeners.add(listener)
    }

    /** Unregisters a view. Pass the same function you passed to [addListener]. */
    fun removeListener(listener: (CampsiteOwner) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyViews(model: CampsiteOwner) {
        listeners.toList().forEach { it(model) }
    }

    // ---- state shown by the views -----------------------------------------------------------------------

    private val _ownedCampsites = mutableListOf<Campsite>()

    /** Read-only snapshot of this owner's campsites, sorted by name (US 02.03.01). */
    val ownedCampsites: List<Campsite> get() = _ownedCampsites.toList()

    /** The campsite opened on the overview screen (US 02.01.04), or null. */
    var selectedCampsite: Campsite? = null
        private set

    /** Problems with the form the owner last submitted: field name to message. */
    var errors: Map<String, String> = emptyMap()
        private set

    /** One-off message (success or unexpected failure) for the views to show. */
    var message: String? = null
        private set

    /** The text inside the selected campsite's QR code (US 02.02.01). */
    val selectedQrPayload: String?
        get() = selectedCampsite?.let { "campreservation://campsite/${it.campsiteId}" }

    // ---- loading ----------------------------------------------------------------------------------------

    /** Loads this owner's campsites from the repository into [ownedCampsites]. */
    override fun loadCampsites() {
        campsites.getByOwner(ownerId) { result ->
            result.onSuccess { list ->
                _ownedCampsites.clear()
                _ownedCampsites.addAll(list.sortedBy { it.campName.lowercase() })
                notifyViews(this)
            }.onFailure { reportFailure(it) }
        }
    }

    // ---- campsites --------------------------------------------------------------------------------------

    /**
     * Registers a new campsite owned by this owner (US 02.01.01, 02.01.02).
     * [draft] carries the form data; its id and owner are replaced here.
     */
    override fun registerCampsite(draft: Campsite, onDone: (Result<Campsite>) -> Unit) {
        val problems = validate(draft)
        if (problems.isNotEmpty()) return reject(problems, onDone)
        val campsite = draft.copy(campsiteId = campsites.newId(), ownerId = ownerId)
        campsites.save(campsite) { saved ->
            saved.onSuccess {
                _ownedCampsites.add(campsite)
                _ownedCampsites.sortBy { it.campName.lowercase() }
                succeed("Campsite saved")
                onDone(Result.success(campsite))
            }.onFailure { fail(it, onDone) }
        }
    }

    /** Replaces the details of one of this owner's campsites (US 02.01.03). */
    override fun updateCampsite(campsiteId: String, draft: Campsite, onDone: (Result<Campsite>) -> Unit) {
        val existing = _ownedCampsites.firstOrNull { it.campsiteId == campsiteId }
            ?: return fail(IllegalArgumentException("Campsite $campsiteId is not one of your campsites"), onDone)
        val problems = validate(draft)
        if (problems.isNotEmpty()) return reject(problems, onDone)
        val updated = draft.copy(campsiteId = existing.campsiteId, ownerId = ownerId)
        campsites.save(updated) { saved ->
            saved.onSuccess {
                _ownedCampsites[_ownedCampsites.indexOf(existing)] = updated
                _ownedCampsites.sortBy { it.campName.lowercase() }
                if (selectedCampsite?.campsiteId == campsiteId) selectedCampsite = updated
                succeed("Campsite saved")
                onDone(Result.success(updated))
            }.onFailure { fail(it, onDone) }
        }
    }

    /** Opens one campsite: sets [selectedCampsite] (US 02.01.04). */
    fun selectCampsite(campsiteId: String, onDone: (Result<Campsite>) -> Unit = {}) {
        val campsite = _ownedCampsites.firstOrNull { it.campsiteId == campsiteId }
            ?: return fail(IllegalArgumentException("Campsite $campsiteId is not one of your campsites"), onDone)
        selectedCampsite = campsite
        notifyViews(this)
        onDone(Result.success(campsite))
    }

    // ---- messages and errors ----------------------------------------------------------------------------

    /** Clears form errors, for example when the owner leaves a form. */
    fun clearErrors() {
        errors = emptyMap()
        notifyViews(this)
    }

    /** Clears the one-off message once a view has shown it. */
    fun clearMessage() {
        message = null
        notifyViews(this)
    }

    // ---- helpers ----------------------------------------------------------------------------------------

    private fun succeed(text: String) {
        errors = emptyMap()
        message = text
        notifyViews(this)
    }

    private fun reportFailure(e: Throwable) {
        message = e.message ?: "Something went wrong"
        notifyViews(this)
    }

    private fun <T> fail(e: Throwable, onDone: (Result<T>) -> Unit) {
        reportFailure(e)
        onDone(Result.failure(e))
    }

    /** Form problems: shown under their fields, and reported to the caller as a failure. */
    private fun <T> reject(problems: Map<String, String>, onDone: (Result<T>) -> Unit) {
        errors = problems
        message = null
        notifyViews(this)
        onDone(Result.failure(IllegalArgumentException("Some fields need fixing")))
    }

    /** The business rules for a campsite. The keys match the field names the form screen shows errors on. */
    private fun validate(c: Campsite): Map<String, String> = buildMap {
        if (c.campName.isBlank()) put("name", "Name is required")
        if (c.descr.isBlank()) put("description", "Describe the amenities")
        if (c.maxOccupancy < 1) put("maxOccupancy", "Must be at least 1")
        if (c.pricePerNight < 0) put("pricePerNight", "Cannot be negative")
        if (c.location.isBlank()) put("location", "Location is required")
    }
    // ---- lotteries --------------------------------------------------------------------------------------

    private var _lotteries: List<Lottery> = emptyList()

    /** Lotteries of the selected campsite, sorted by booking start. */
    val lotteries: List<Lottery> get() = _lotteries

    /** Loads the lotteries of the selected campsite. Call after [selectCampsite]. */
    fun loadLotteries() {
        val campsite = selectedCampsite ?: return
        scope.launch {
            try {
                _lotteries = lotteryRepo.listings(campsite.campsiteId)
                notifyViews(this@CampsiteOwner)
            } catch (e: Exception) { reportFailure(e) }
        }
    }

    /** Publishes a lottery for the selected campsite (US 02.04.01 to 02.04.07). Dates are YYYY-MM-DD. */
    fun createLottery(
        bookingStart: String, bookingEnd: String, winners: Int,
        onDone: (Result<Lottery>) -> Unit = {}
    ) {
        val campsite = selectedCampsite
            ?: return fail(IllegalStateException("Open a campsite first"), onDone)
        if (winners < 1) return reject(mapOf("winnerCount" to "Need at least 1 winner"), onDone)
        val lottery = try {
            Lottery.create(lotteryRepo.newId(campsite.campsiteId), bookingStart, bookingEnd, winners)
        } catch (e: IllegalArgumentException) {
            return reject(mapOf("dates" to (e.message ?: "Check the dates")), onDone)
        }
        scope.launch {
            try {
                lotteryRepo.publish(campsite.campsiteId, ownerId, lottery)
                _lotteries = (_lotteries + lottery).sortedBy { it.bookingStart }
                succeed("Lottery published")
                onDone(Result.success(lottery))
            } catch (e: Exception) { fail(e, onDone) }
        }
    }
}