package com.example.campreservation

class CampsiteOwner(
    override val ownerId: String,
    val displayName: String,
    private val campsites: CampsiteRepository,
    private val lotteries: LotteryRepository,
    private val reservations: ReservationRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : Owner {
    private val ownedCampsites = mutableListOf<Campsite>() //The owner's real, editable list of campsites
    val OwnedCampsites: List<Campsite> get() = ownedCampsites.toList() //Read-only snapshot of the campsites this owner has registered

    override fun loadCampsites() { // Loads this owner's campsites from the repository into [ownedCampsites]
        OwnedCampsites.clear()
        OwnedCampsites.addAll(campsites.byOwner(ownerId))
    }

    override fun registerCampsite(details: CampsiteDetails): Result<Campsite> {
        val errors = validate(details)
        if (errors.isNotEmpty()) {
            return Result.failure(IllegalArgumentException(errors.joinToString("; ")))
        }
        val campsite = Campsite(campsites.newId(), ownerId, details)
        campsites.save(campsite)
        OwnedCampsites.add(campsite)
        return Result.success(campsite)
    }

}

