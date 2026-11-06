package com.example.campreservation.view

//import com.example.campreservation.model.ValidationError
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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

    // What the screens display. Compose redraws whenever one of these changes.
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

    /** Called by the model after every state change. Copies the model's state; never modifies the model. */
    fun update(model: CampsiteOwner) {
        campsites = model.ownedCampsites
        selectedCampsite = model.selectedCampsite
        //qrPayload = model.selectedQrPayload
        errors = model.errors
        message = model.message
    }

    /** The owner's whole UI: three screens and the navigation between them. */
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
                    MyCampsitesScreen(
                        campsites = campsites,
                        onAdd = { controller.clearErrors(); nav.navigate("campsiteForm") },
                        onManage = { id -> controller.openCampsite(id); nav.navigate("campsite") },
                    )
                }
                composable("campsiteForm") {
                    CampsiteFormScreen(
                        existing = null,
                        errors = errors,
                        onSave = { draft -> controller.saveCampsite(draft, null) { nav.popBackStack() } },
                        onBack = { controller.clearErrors(); nav.popBackStack() },
                    )
                }
                composable("campsiteEdit") {
                    CampsiteFormScreen(
                        existing = selectedCampsite,
                        errors = errors,
                        onSave = { draft ->
                            controller.saveCampsite(draft, selectedCampsite?.campsiteId) { nav.popBackStack() }
                        },
                        onBack = { controller.clearErrors(); nav.popBackStack() },
                    )
                }
                composable("campsite") {
                    CampsiteOverviewScreen(
                        campsite = selectedCampsite,
                        qrPayload = qrPayload,
                        onEdit = { controller.clearErrors(); nav.navigate("campsiteEdit") },
                        onBack = { nav.popBackStack() },
                    )
                }
            }
        }
    }

}