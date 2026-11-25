package com.example.campreservation

/**
 * Sends notifications and respects the recipient's profile setting (notificationsEnabled).
 *
 * Usage:  NotificationService(NotificationLog(), profileRepository::findProfile)
 *
 * send() succeeds with true  -> stored in the recipient's inbox
 *                       false -> recipient has notifications turned off, nothing stored
 */
class NotificationService(
    private val log: NotificationStore,
    private val findProfile: (String, (Result<Profile?>) -> Unit) -> Unit
) {

    fun send(
        recipientId: String,
        title: String,
        body: String,
        type: String = "GENERAL",
        onResult: (Result<Boolean>) -> Unit = {}
    ) {
        if (recipientId.isBlank()) {
            onResult(Result.failure(IllegalArgumentException("Recipient is required")))
            return
        }

        findProfile(recipientId) { profileResult ->
            val error = profileResult.exceptionOrNull()
            if (error != null) {
                onResult(Result.failure(error))
            } else {
                val profile = profileResult.getOrNull()
                if (profile != null && profile.disableNotifications()) {
                    // Recipient turned notifications off: skip, store nothing
                    onResult(Result.success(false))
                } else {
                    store(recipientId, title, body, type, onResult)
                }
            }
        }
    }

    private fun store(
        recipientId: String,
        title: String,
        body: String,
        type: String,
        onResult: (Result<Boolean>) -> Unit
    ) {
        val notification = Notification(
            recipientId = recipientId,
            title = title,
            body = body,
            type = type
        )
        val callback: (Result<Unit>) -> Unit = { saved ->
            val saveError = saved.exceptionOrNull()
            if (saveError == null) {
                onResult(Result.success(true))
            } else {
                onResult(Result.failure(saveError))
            }
        }
        log.add(notification, callback)
    }

    fun sendTest(recipientId: String, onResult: (Result<Boolean>) -> Unit = {}) {
        send(recipientId, "Test notification", "This is a test notification.", "TEST", onResult)
    }
}