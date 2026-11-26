package com.example.campreservation

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class Waitlist(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val collectionName: String = "entrants",
    private val lotteryDocument: DocumentReference? = null
) {
    private val collection get() = lotteryDocument?.collection("entrants") ?: db.collection(collectionName)

    suspend fun contains(id: String): Boolean = collection.document(id).get().await().exists()

    /**
     * Adds an entrant, using [id] as the document id.
     * Adding the same id twice just overwrites the existing entry,
     * so nobody can end up in the pool more than once.
     */
    suspend fun addEntrant(id: String, name: String) {
        val entry = collection.document(id)
        val listing = lotteryDocument
        if (listing == null) {
            entry.set(mapOf("name" to name)).await()
            return
        }
        db.runTransaction { transaction ->
            val lottery = transaction.get(listing).toObject(Lottery::class.java)
            check(lottery?.acceptsEntries(System.currentTimeMillis()) == true) { "Registration is closed" }
            transaction.update(listing, "waitlistVersion", FieldValue.increment(1))
            transaction.set(entry, mapOf("name" to name))
        }.await()
    }

    /** Removes an entrant. Does nothing if the id doesn't exist. */
    suspend fun removeEntrant(id: String) {
        val entry = collection.document(id)
        val listing = lotteryDocument
        if (listing == null) {
            entry.delete().await()
            return
        }
        db.runTransaction { transaction ->
            if (transaction.get(entry).exists()) {
                check(transaction.get(listing).getBoolean("drawn") != true) { "Lottery has already been drawn" }
                transaction.update(listing, "waitlistVersion", FieldValue.increment(1))
                transaction.delete(entry)
            }
        }.await()
    }
}

// Example usage from a coroutine (e.g. viewModelScope.launch { ... })
suspend fun runWaitlist() {
    val waitlist = Waitlist()
    waitlist.addEntrant("user123", "Alex")
    waitlist.removeEntrant("user123")
}
