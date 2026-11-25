package com.example.campreservation

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
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
            check(!transaction.get(listing).exists()) { "Lottery already exists" }
            transaction.set(listing, lottery)
        }.await()
    }

    suspend fun listings(campsiteId: String): List<Lottery> =
        campsite(campsiteId).collection("lotteries").get().await()
            .toObjects(Lottery::class.java).sortedBy { it.bookingStart }

    /** Commits all entrant outcomes together and returns the winning entrant IDs. */
    suspend fun draw(campsiteId: String, lotteryId: String, ownerId: String): List<String> {
        val camp = campsite(campsiteId)
        val listing = camp.collection("lotteries").document(lotteryId)
        val entries = listing.collection("entrants")
        // Android transactions read documents individually. Check the membership version
        // to detect joins or withdrawals between the collection query and the commit.
        val version = listing.get(Source.SERVER).await().getLong("waitlistVersion")
        val ids = entries.get(Source.SERVER).await().documents.map { it.id }
        return db.runTransaction { transaction ->
            check(transaction.get(camp).getString("ownerId") == ownerId) { "Only the campsite owner can draw" }
            val snapshot = transaction.get(listing)
            val lottery = checkNotNull(snapshot.toObject(Lottery::class.java)) { "Lottery does not exist" }
            check(snapshot.getLong("waitlistVersion") == version) { "Waitlist changed. Please retry the draw" }
            val outcomes = LotterySystem().draw(lottery, ids, System.currentTimeMillis())
            for ((id, outcome) in outcomes) {
                transaction.update(entries.document(id), "outcome", outcome.name)
            }
            transaction.update(listing, "drawn", true)
            outcomes.filterValues { it == LotteryOutcome.WON }.keys.toList()
        }.await()
    }

    suspend fun winners(campsiteId: String, lotteryId: String): List<String> =
        campsite(campsiteId).collection("lotteries").document(lotteryId).collection("entrants")
            .whereEqualTo("outcome", LotteryOutcome.WON.name).get().await().documents.map { it.id }

    /** Observes the stored result. A missing entry or a pending draw yields null. */
    fun listenToOutcome(
        campsiteId: String, lotteryId: String, entrantId: String,
        onChange: (Result<LotteryOutcome?>) -> Unit
    ): ListenerRegistration =
        campsite(campsiteId).collection("lotteries").document(lotteryId)
            .collection("entrants").document(entrantId).addSnapshotListener { snapshot, error ->
                if (error != null) onChange(Result.failure(error))
                else onChange(runCatching { snapshot?.getString("outcome")?.let(LotteryOutcome::valueOf) })
            }

    fun waitlist(campsiteId: String, lotteryId: String): Waitlist {
        val listing = campsite(campsiteId).collection("lotteries").document(lotteryId)
        return Waitlist(db, lotteryDocument = listing)
    }
}
