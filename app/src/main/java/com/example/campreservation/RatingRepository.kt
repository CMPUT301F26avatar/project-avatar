package com.example.campreservation

import com.google.firebase.firestore.FirebaseFirestore

class RatingRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    private fun collection(campsiteId: String) =
        db.collection("campsites").document(campsiteId).collection("ratings")

    fun getForCampsite(campsiteId: String, onResult: (Result<List<Rating>>) -> Unit) {
        collection(campsiteId).get()
            .addOnSuccessListener { onResult(Result.success(it.toObjects(Rating::class.java))) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun save(rating: Rating, onResult: (Result<Unit>) -> Unit) {
        collection(rating.campsiteId).document(rating.ratingId).set(rating)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }
}