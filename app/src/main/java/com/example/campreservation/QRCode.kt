package com.example.campreservation

class QRCode(val campsiteId: String) {

    // Returns the text that goes inside the QR picture
    fun getText(): String {
        return "campreservation://campsite/" + campsiteId
    }
}
