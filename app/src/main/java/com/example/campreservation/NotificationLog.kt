package com.example.campreservation

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

// Interface so the service and screens can be tested with a fake (no Firebase needed)
interface NotificationStore {
    fun add(notification: Notification, onDone: (Result<Unit>) -> Unit = {})
    fun forRecipient(recipientId: String, callback: (Result<List<Notification>>) -> Unit)
    fun markRead(notificationId: String, onDone: (Result<Unit>) -> Unit = {})
}

class NotificationLog : NotificationStore {

    private val ref by lazy { Firebase.firestore.collection("notifications") }

    override fun add(notification: Notification, onDone: (Result<Unit>) -> Unit) {
        ref.document(notification.notificationId).set(notification)
            .addOnSuccessListener { onDone(Result.success(Unit)) }
            .addOnFailureListener {
                Log.e(TAG, "add failed", it)
                onDone(Result.failure(it))
            }
    }

    // Sorted in memory so no composite Firestore index is required
    override fun forRecipient(recipientId: String, callback: (Result<List<Notification>>) -> Unit) {
        ref.whereEqualTo("recipientId", recipientId).get()
            .addOnSuccessListener { snap ->
                callback(Result.success(snap.toObjects(Notification::class.java).sortedByDescending { it.timestamp }))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    override fun markRead(notificationId: String, onDone: (Result<Unit>) -> Unit) {
        ref.document(notificationId).update("read", true)
            .addOnSuccessListener { onDone(Result.success(Unit)) }
            .addOnFailureListener { onDone(Result.failure(it)) }
    }

    companion object {
        private const val TAG = "NotificationLog"
    }
}