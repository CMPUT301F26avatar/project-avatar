package com.example.campreservation

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class LotteryRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    private fun campsite(id: String) = db.collection("campsites").document(id)

    fun newId(campsiteId: String) = campsite(campsiteId).collection("lotteries").document().id

    suspend fun publish(campsiteId: String, ownerId: String, lottery: Lottery) {
        val camp = campsite(campsiteId)
        val listing = camp.collection("lotteries").document(lottery.lotteryId)
        db.runTransaction { transaction ->
            check(transaction.get(camp).getString("ownerId") == ownerId) { "Only the campsite owner can publish a lottery" }
            check(lottery.acceptsEntries(System.currentTimeMillis())) { "Booking has already started" }
            transaction.set(listing, lottery)
        }.await()
    }

    suspend fun listings(campsiteId: String): List<Lottery> =
        campsite(campsiteId).collection("lotteries").get().await()
            .toObjects(Lottery::class.java).sortedBy { it.bookingStart }

    fun waitlist(campsiteId: String, lotteryId: String): Waitlist {
        val listing = campsite(campsiteId).collection("lotteries").document(lotteryId)
        return Waitlist(db, lotteryDocument = listing)
    }
}
