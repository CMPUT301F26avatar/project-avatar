package com.example.campreservation

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

interface MessageStore {
    fun add(message: Message, onDone: (Result<Unit>) -> Unit = {})
    fun forRecipient(recipientId: String, callback: (Result<List<Message>>) -> Unit)
    fun markRead(messageId: String, onDone: (Result<Unit>) -> Unit = {})
}

class MessageLog : MessageStore {

    private val ref by lazy { Firebase.firestore.collection("messages") }

    override fun add(message: Message, onDone: (Result<Unit>) -> Unit) {
        ref.document(message.messageId).set(message)
            .addOnSuccessListener { onDone(Result.success(Unit)) }
            .addOnFailureListener {
                Log.e(TAG, "add failed", it)
                onDone(Result.failure(it))
            }
    }

    override fun forRecipient(recipientId: String, callback: (Result<List<Message>>) -> Unit) {
        ref.whereEqualTo("recipientId", recipientId).get()
            .addOnSuccessListener { snap ->
                callback(Result.success(snap.toObjects(Message::class.java).sortedByDescending { it.timestamp }))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    override fun markRead(messageId: String, onDone: (Result<Unit>) -> Unit) {
        ref.document(messageId).update("read", true)
            .addOnSuccessListener { onDone(Result.success(Unit)) }
            .addOnFailureListener { onDone(Result.failure(it)) }
    }

    companion object {
        private const val TAG = "MessageLog"
    }
}