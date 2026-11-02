package com.example.campreservation

interface EntrantInterface {
    fun searchCampsites(query: String, repository: CampsiteRepository): List<Campsite>
    fun viewCampsite(campsite: Campsite): CampView
    fun joinWaitlist(waitList: WaitList): Boolean
    fun leaveWaitlist(waitList: WaitList): Boolean
    fun receiveNotification(notification: Notification)
    fun makeReservation(reservation: Reservation)
    fun cancelReservation(reservation: Reservation)
    fun scanQRCode(qrCode: QRCode): Campsite?
    fun rateCampsite(rating: Rating)
    fun uploadProfileImage(image: ProfileImage)
}