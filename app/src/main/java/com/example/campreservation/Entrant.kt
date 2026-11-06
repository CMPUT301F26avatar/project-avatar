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
    override fun viewCampsite(campsite: Campsite): CampView = CampView(campsite)


    override fun joinWaitlist(waitList: WaitList): Boolean = waitList.addEntrant(entrantId)

    override fun leaveWaitlist(waitList: WaitList): Boolean = waitList.removeEntrant(entrantId)

    override fun receiveNotification(notification: Notification){
        if (profile.disableNotifications()) return
        notifications.add(notification)
    }

    fun receiveMessage(message: Message) {
        messages.add(message)
    }

    override fun makeReservation(reservation: Reservation){
        reservations.add(reservation)
    }

    override fun cancelReservation(reservation: Reservation){
        reservations.remove(reservation)
    }

    override fun scanQRCode(qrCode: QRCode): String? = qrCode.campsiteId

    override fun rateCampsite(rating: Rating, repository: RatingRepository) {
        repository.save(rating.copy(raterId = entrantId)){}
    }
    **/
    override fun uploadProfileImage(image: Image) {
        profile.setImage(image)
    }

    override fun equals(other: Any?): Boolean =
        other is Entrant && other.entrantId == entrantId

    override fun hashCode(): Int = entrantId.hashCode()
}