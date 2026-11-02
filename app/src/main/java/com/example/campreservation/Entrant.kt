package com.example.campreservation

class Entrant (
    var entrantId: String = "",
    var profile: Profile = Profile()
): EntrantInterface{
    val reservations: MutableList<Reservation> = mutableListOf()
    val notifications: MutableList<Notification> = mutableListOf()
    val messages: MutableList<Message> = mutableListOf()

    init {
        if (profile.entrantId.isBlank()) profile.entrantId = entrantId
    }

    override fun searchCampsites(query: String, repository: CampsiteRepository): List<Campsite> =
        repository.search(query)

    override fun viewCampsite(campsite: Campsite): CampView = Campview(campsite)


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

    override fun scanQRCode(qrCode: QRCode): Campsite? = qrCode.campsite

    override fun rateCampsite(rating: Rating){
        rating.campsite.ratingSystem.addRating(rating)
    }

    override fun uploadProfileImage(image: ProfileImage) {
        profile.setImage(image)
    }

    override fun equals(otherEntrant: Any?): Boolean =
        otherEntrant is Entrant && otherEntrant.entrantId == entrantId

    fun hashcode(): Int = entrantId.hashcode
}