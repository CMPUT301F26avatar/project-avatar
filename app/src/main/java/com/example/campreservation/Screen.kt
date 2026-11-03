package com.example.campreservation

import android.app.Fragment

@Suppress("DEPRECATION")
abstract class Screen : Fragment() {

    abstract val role: ENTRANT
}

class ENTRANT {

}
