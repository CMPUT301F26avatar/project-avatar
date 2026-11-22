package com.example.campreservation

class Entrant (
    var entrantId: String = "",
    var profile: Profile = Profile()
): EntrantInterface{

    /**
    val reservations: MutableList<Reservation> = mutableListOf()
    val notifications: MutableList<Notification> = mutableListOf()
    val messages: MutableList<Message> = mutableListOf()
    **/
    init {
        if (profile.deviceId.isBlank()) profile.deviceId = entrantId
    }

    override fun searchCampsites(
                                query: String,
                                repository: CampsiteRepository,
                                guests: Int?,
                                onResult: (Result<List<Campsite>>) -> Unit )
                                = repository.search(query, guests, onResult)

    /**

    override fun joinWaitlist(waitList: WaitList): Boolean = waitList.addEntrant(entrantId)

    override fun leaveWaitlist(waitList: WaitList): Boolean = waitList.removeEntrant(entrantId)

    override fun receiveNotification(notification: Notification){
        if (profile.disableNotifications()) return
        notifications.add(notification)
    }

    fun receiveMessage(message: Message) {
        messages.add(message)
    }
    override fun scanQRCode(qrCode: QRCode): String? = qrCode.campsiteId

    override fun rateCampsite(rating: Rating, repository: RatingRepository) {
        repository.save(rating.copy(raterId = entrantId)){}
    }
    **/

    override fun viewCampsite(
        campsiteId: String,
        repository: CampsiteRepository,
        onResult: (Result<Campsite?>) -> Unit) =
        repository.getById(campsiteId, onResult)

    override fun uploadProfileImage(image: Image) {
        profile.setImage(image)
    }

    override fun makeReservation(
        campsite: Campsite,
        startDate: Long,
        endDate: Long,
        guests: Int,
        repository: ReservationRepository,
        onResult: (Result<Unit>) -> Unit
    ){
        val reservation = Reservation.forCampsite(campsite, entrantId, startDate, endDate, guests)
        repository.saveIfAvailable(reservation, onResult)
    }

    override fun cancelReservation(
        reservation: Reservation,
        repository: ReservationRepository,
        onResult: (Result<Unit>) -> Unit){
        if (!reservation.isOwnedBy(entrantId)) {
            onResult(Result.failure(IllegalArgumentException("Not your reservation")))
            return
        }
        repository.cancel(reservation.reservationId, onResult)
    }

    override fun getMyReservations(
        repository: ReservationRepository,
        onResult: (Result<List<Reservation>>) -> Unit) =
        repository.getByEntrant(entrantId, onResult)

    override fun equals(other: Any?): Boolean =
        other is Entrant && other.entrantId == entrantId

    override fun hashCode(): Int = entrantId.hashCode()
}