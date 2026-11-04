package com.example.campreservation


import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration


/**
 * Handles saving, updating, deleting and retrieving Campsites from Firestore.
 * Matches the CampsiteRepository CRC card. Activities/Fragments call this class
 * and never talk to Firebase directly.
 *
 * Firestore layout:
 *   campsites (collection)
 *     {campsiteId} (document) -> fields of Campsite
 *
 * All calls are asynchronous: Firebase does the work in the background and
 * calls onResult when it finishes.
 */
class CampsiteRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val campsites = db.collection("campsites")

    /** Create OR update. Using campsiteId as the document id means saving
     *  the same campsite twice overwrites it instead of duplicating it. */
    fun save(campsite: Campsite, onResult: (Result<Unit>) -> Unit) {
        val error = campsite.validationError()
        if (error != null) {
            onResult(Result.failure(IllegalArgumentException(error)))
            return
        }
        campsites.document(campsite.campsiteId)
            .set(campsite)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    /** One campsite by id (used by CampView). Returns null inside Result if not found. */
    fun getById(campsiteId: String, onResult: (Result<Campsite?>) -> Unit) {
        campsites.document(campsiteId).get()
            .addOnSuccessListener { doc ->
                onResult(Result.success(doc.toObject(Campsite::class.java)))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    /** Every campsite (admin browse, or the base list for search). */
    fun getAll(onResult: (Result<List<Campsite>>) -> Unit) {
        campsites.get()
            .addOnSuccessListener { snap ->
                onResult(Result.success(snap.toObjects(Campsite::class.java)))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    /** Only this owner's campsites (OwnerView list, US 02.03.01). */
    fun getByOwner(ownerId: String, onResult: (Result<List<Campsite>>) -> Unit) {
        campsites.whereEqualTo("ownerId", ownerId).get()
            .addOnSuccessListener { snap ->
                onResult(Result.success(snap.toObjects(Campsite::class.java)))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    /**
     * Search and filter (US 01.01.01 - 01.01.03).
     * Firestore can't do "contains" text search, so we load campsites and
     * filter on the device with Campsite.matches(). Fine for a class project.
     */
    fun search(
        query: String,
        guests: Int? = null,
        onResult: (Result<List<Campsite>>) -> Unit
    ) {
        getAll { result ->
            onResult(result.map { list -> list.filter { it.matches(query, guests) } })
        }
    }

    /** Delete (owner or admin). */
    fun delete(campsiteId: String, onResult: (Result<Unit>) -> Unit) {
        campsites.document(campsiteId).delete()
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    /**
     * Real-time updates ("real-time status updates" in the project description).
     * onChange fires now and again whenever the data changes.
     * Keep the returned registration and call .remove() in onDestroy/onStop
     * so the listener doesn't leak.
     */
    fun listenToOwnerCampsites(
        ownerId: String,
        onChange: (List<Campsite>) -> Unit
    ): ListenerRegistration =
        campsites.whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                onChange(snap.toObjects(Campsite::class.java))
            }
}