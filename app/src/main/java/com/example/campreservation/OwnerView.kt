package com.example.campreservation.view

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.campreservation.Campsite
import com.example.campreservation.CampsiteOwner
import com.example.campreservation.controller.OwnerController

class OwnerView(private val controller: OwnerController){

    var campsites by mutableStateOf<List<Campsite>>(emptyList())
        private set
    var selectedCampsite by mutableStateOf<Campsite?>(null)
        private set
    var qrPayload by mutableStateOf<String?>(null)
        private set
    var errors by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var message by mutableStateOf<String?>(null)
        private set

    fun update(model: CampsiteOwner) {
        campsites = model.ownedCampsites
        selectedCampsite = model.selectedCampsite
        errors = model.errors
        message = model.message
    }

    @Composable
    fun Content() {
        val nav = rememberNavController()
        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(Unit) { controller.refresh() }
        LaunchedEffect(message) {
            message?.let { snackbar.showSnackbar(it); controller.dismissMessage() }
        }
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
                composable("home") {
                    Text("Owner screens coming soon")
                }
            }
        }
    }
}