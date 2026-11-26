package com.example.campreservation

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ReservationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val reservations = db.collection("reservations")

    fun save(reservation: Reservation, onResult: (Result<Unit>) -> Unit) {
        val error = reservation.validationError()
        if (error != null) {
            onResult(Result.failure(IllegalArgumentException(error)))
            return
        }
        reservations.document(reservation.reservationId)
            .set(reservation)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun saveIfAvailable(reservation: Reservation, onResult: (Result<Unit>) -> Unit) {
        val error = reservation.validationError()
        if (error != null) {
            onResult(Result.failure(IllegalArgumentException(error)))
            return
        }
        isAvailable(reservation.campsiteId, reservation.startDate, reservation.endDate){ result ->
            result
                .onSuccess { free ->
                    if (free) save(reservation, onResult)
                    else onResult(Result.failure(IllegalStateException("Those dates are always booked")))
                }
                .onFailure { onResult(Result.failure(it)) }
        }
    }

    fun isAvailable(
        campsiteId: String,
        start: Long,
        end: Long,
        onResult: (Result<Boolean>) -> Unit
    ) {
        getByCampsite(campsiteId) { result ->
        onResult(result.map { list -> list.none { it.isActive() && it.overlaps(start, end) } })
        }
    }

    fun getById(reservationId: String, onResult: (Result<Reservation?>) -> Unit){
        reservations.document(reservationId).get()
            .addOnSuccessListener { doc ->
                onResult(Result.success(doc.toObject(Reservation:: class.java)))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun getAll(onResult: (Result<List<Reservation>>) -> Unit){
        reservations.get()
            .addOnSuccessListener { snap ->
                onResult(Result.success(snap.toObjects(Reservation:: class.java)))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun getByEntrant(deviceId: String, onResult: (Result<List<Reservation>>) -> Unit){
        reservations.whereEqualTo("deviceId", deviceId).get()
            .addOnSuccessListener { snap ->
                val list =  snap.toObjects(Reservation:: class.java).sortedByDescending {it.startDate}
                onResult(Result.success(list))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun getByCampsite(campsiteId: String, onResult: (Result<List<Reservation>>) -> Unit){
        reservations.whereEqualTo("campsiteId", campsiteId).get()
            .addOnSuccessListener { snap ->
                val list =  snap.toObjects(Reservation:: class.java).sortedBy {it.startDate}
                onResult(Result.success(list))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun getByOwner(ownerId: String, onResult: (Result<List<Reservation>>) -> Unit){
        reservations.whereEqualTo("ownerId", ownerId).get()
            .addOnSuccessListener { snap ->
                val list =  snap.toObjects(Reservation:: class.java).sortedByDescending {it.startDate}
                onResult(Result.success(list))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun cancel(reservationId: String, onResult: (Result<Unit>) -> Unit){
        reservations.document(reservationId)
            .update("status", Reservation.STATUS_CANCELLED)
            .addOnSuccessListener { onResult(Result.success(Unit))}
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun delete(reservationId: String, onResult: (Result<Unit>) -> Unit){
        reservations.document(reservationId).delete()
            .addOnSuccessListener { onResult(Result.success(Unit))}
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun listenToEntrantReservations(
        deviceId: String,
        onChange: (List<Reservation>) -> Unit
    ): ListenerRegistration =
        reservations.whereEqualTo("deviceId", deviceId)
            .addSnapshotListener { snap, error ->
                if(error != null || snap == null) return@addSnapshotListener
                onChange(snap.toObjects(Reservation::class.java).sortedByDescending { it.startDate })
            }

    fun listenToOwnerReservations(
        ownerId: String,
        onChange: (List<Reservation>) -> Unit
    ): ListenerRegistration =
        reservations.whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snap, error ->
                if(error != null || snap == null) return@addSnapshotListener
                onChange(snap.toObjects(Reservation::class.java).sortedByDescending { it.startDate })
            }
}