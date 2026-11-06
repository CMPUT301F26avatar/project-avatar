package com.example.campreservation

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage

class ProfileRepository {

    private val db = Firebase.firestore
    private val profilesRef = db.collection("profiles")
    private val storage = Firebase.storage

    private val _profiles = mutableStateListOf<Profile>()

    // Up-to-date list for admin tracking
    val profiles: List<Profile>
        get() = _profiles

    // Save a new profile
    fun addProfile(profile: Profile) {
        profilesRef.document(profile.deviceId).set(profile)
            .addOnFailureListener { Log.e(TAG, "addProfile failed", it) }
    }

    // Update personal info
    fun updateProfile(profile: Profile, onDone: (Result<Unit>) -> Unit = {}) {
        profilesRef.document(profile.deviceId).set(profile)
            .addOnSuccessListener { onDone(Result.success(Unit)) }
            .addOnFailureListener {
                Log.e(TAG, "updateProfile failed", it)
                onDone(Result.failure(it))
            }
    }

    // only write when the user saves a valid profile
    fun findProfile(deviceId: String, callback: (Result<Profile?>) -> Unit) {
        profilesRef.document(deviceId).get()
            .addOnSuccessListener { doc -> callback(Result.success(doc.toObject(Profile::class.java))) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    // Delete profile and its image
    fun removeProfile(profile: Profile, onDone: (Result<Unit>) -> Unit = {}) {
        removeProfileImage(profile.deviceId)
        profilesRef.document(profile.deviceId).delete()
            .addOnSuccessListener { onDone(Result.success(Unit)) }
            .addOnFailureListener {
                Log.e(TAG, "removeProfile failed", it)
                onDone(Result.failure(it))
            }
    }

    // Admin Feature: Remove only the image
    fun removeProfileImage(deviceId: String) {
        // Adjust this path to match wherever ProfileImage uploads to
        storage.reference.child("profile_images/$deviceId").delete()
            .addOnFailureListener { Log.w(TAG, "No profile image to delete", it) }
        profilesRef.document(deviceId).update("profileImageUrl", null)
    }

    // All profiles (admin browse)
    @JvmName("fetchProfiles")
    fun getProfiles(): List<Profile> = _profiles

    // Look up one profile by device ID from the list
    fun getProfile(deviceId: String): Profile? {
        return _profiles.find { it.deviceId == deviceId }
    }

    fun getOrCreateProfile(deviceId: String, callback: (Result<Profile>) -> Unit) {
        val docRef = profilesRef.document(deviceId)
        docRef.get()
            .addOnSuccessListener { doc ->
                val existing = doc.toObject(Profile::class.java)
                if (existing != null) {
                    callback(Result.success(existing))
                } else {
                    val newProfile = Profile(deviceId = deviceId) // adjust to your constructor
                    docRef.set(newProfile)
                        .addOnSuccessListener { callback(Result.success(newProfile)) }
                        .addOnFailureListener { callback(Result.failure(it)) }
                }
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    init {
        profilesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Snapshot listener failed", error)
                return@addSnapshotListener
            }
            _profiles.clear()
            snapshot?.documents?.forEach { doc ->
                doc.toObject(Profile::class.java)?.let { _profiles.add(it) }
            }
        }
    }

    companion object {
        private const val TAG = "ProfileRepository"
    }
}