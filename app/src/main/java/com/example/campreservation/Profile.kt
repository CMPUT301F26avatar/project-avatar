package com.example.campreservation

class Profile (
    var entrantId: String = "",
    var name: String = "",
    var email: String = "",
    var phoneNumber: String = "",
    var profileImage: ProfileImage? = null,
    var notificationsEnabled: Boolean = true
){

    fun updateInfo(newName: String, newEmail: String, newPhoneNumber: String = ""){
        require(newName.isNotBlank()) {"Name can not be left empty"}
        name = newName
        email = newEmail
        phoneNumber = newPhoneNumber
    }

    fun notificationPreference(enabled: Boolean){
        notificationsEnabled = enabled
    }

    fun disableNotifications(): Boolean = !notificationsEnabled

    fun setImage(image: ProfileImage){
        profileImage = image
    }

    fun removeImage(){
        profileImage = null
    }

    override fun toString(): String = "Profile(name = $name, email = $email, phone =  $phoneNumber)"
}
