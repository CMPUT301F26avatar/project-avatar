package com.example.campreservation

class Profile (
    var deviceId: String = "",
    var name: String = "",
    var email: String = "",
    var phoneNumber: String = "",
    var profileImageUrl: String? = null,
    var notificationsEnabled: Boolean = true,
    var role: String = UserRole.ENTRANT.name
){

    fun updateInfo(newName: String, newEmail: String, newPhoneNumber: String = ""){
        require(newName.isNotBlank()) {"Name can not be left empty"}
        name = newName
        email = newEmail
        phoneNumber = newPhoneNumber
    }
    //to push
    fun notificationPreference(enabled: Boolean){
        notificationsEnabled = enabled
    }

    fun disableNotifications(): Boolean = !notificationsEnabled

    fun setImage(image: Image){
        profileImageUrl = image.url
    }

    fun removeImage(){
        profileImageUrl= null
    }

    override fun toString(): String = "Profile(name = $name, email = $email, phone =  $phoneNumber)"
}