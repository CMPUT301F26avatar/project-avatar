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
    fun updateProfile(profile: Profile) {
        profilesRef.document(profile.deviceId).set(profile)
            .addOnFailureListener { Log.e(TAG, "updateProfile failed", it) }
    }

    // Delete profile and its image
    fun removeProfile(profile: Profile) {
        removeProfileImage(profile.deviceId)
        profilesRef.document(profile.deviceId).delete()
            .addOnFailureListener { Log.e(TAG, "removeProfile failed", it) }
    }

    // Admin Feature: Remove only the image
    fun removeProfileImage(deviceId: String) {
        // Adjust this path to match wherever ProfileImage uploads to
        storage.reference.child("profile_images/$deviceId").delete()
            .addOnFailureListener { Log.w(TAG, "No profile image to delete", it) }
        profilesRef.document(deviceId).update("profileImageUrl", null)
    }

    // All profiles (admin browse)
    fun getProfiles(): List<Profile> = _profiles

    // Look up one profile by device ID from the list
    fun getProfile(deviceId: String): Profile? {
        return _profiles.find { it.deviceId == deviceId }
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