package com.example.campreservation

interface EntrantInterface {
    fun searchCampsites(query: String,
                        repository: CampsiteRepository,
                        onResult: (Result<List<Campsite>>) -> Unit)

    fun viewCampsite(campsite: Campsite): CampView
    fun joinWaitlist(waitList: WaitList): Boolean
    fun leaveWaitlist(waitList: WaitList): Boolean
    fun receiveNotification(notification: Notification)
    fun makeReservation(reservation: Reservation)
    fun cancelReservation(reservation: Reservation)
    fun scanQRCode(qrCode: QRCode): String?
    fun rateCampsite(rating: Rating, repository: RatingRepository)
    fun uploadProfileImage(image: Image)
}