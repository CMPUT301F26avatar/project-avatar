package com.example.campreservation

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class Waitlist (
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val collectionName: String = "entrants"
    ){
    private val collection get() = db.collection(collectionName)

    /**
     * Adds an entrant, using [id] as the document id.
     * Adding the same id twice just overwrites the existing entry,
     * so nobody can end up in the pool more than once.
     */
    suspend fun addEntrant(id: String, name: String) {
        collection.document(id).set(mapOf("name" to name)).await()
    }

    /** Removes an entrant. Does nothing if the id doesn't exist. */
    suspend fun removeEntrant(id: String) {
        collection.document(id).delete().await()
    }
}

// Example usage from a coroutine (e.g. viewModelScope.launch { ... })
suspend fun runWaitlist() {
    val waitlist = Waitlist()
    waitlist.addEntrant("user123", "Alex")
    waitlist.removeEntrant("user123")
}