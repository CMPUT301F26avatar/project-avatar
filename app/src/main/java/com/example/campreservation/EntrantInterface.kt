package com.example.campreservation

interface EntrantInterface {
    fun searchCampsites(query: String,
                        repository: CampsiteRepository,
                        guests: Int? = null,
                        onResult: (Result<List<Campsite>>) -> Unit)

    fun viewCampsite(
        campsiteId: String,
        repository: CampsiteRepository,
        onResult: (Result<Campsite?>) -> Unit)


    fun uploadProfileImage(image: Image)

    fun makeReservation(
        campsite: Campsite,
        startDate: Long,
        endDate: Long,
        guests: Int,
        repository: ReservationRepository,
        onResult: (Result<Unit>) -> Unit
    )

    fun cancelReservation(
        reservation: Reservation,
        repository: ReservationRepository,
        onResult: (Result<Unit>) -> Unit)

    fun getMyReservations(
        repository: ReservationRepository,
        onResult: (Result<List<Reservation>>) -> Unit)
    /**
    fun joinWaitlist(waitList: WaitList): Boolean
    fun leaveWaitlist(waitList: WaitList): Boolean
    fun receiveNotification(notification: Notification)
    fun scanQRCode(qrCode: QRCode): String?
    fun rateCampsite(rating: Rating, repository: RatingRepository)
    **/
}